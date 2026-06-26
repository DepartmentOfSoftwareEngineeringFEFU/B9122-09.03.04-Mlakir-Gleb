# aura-core-service

`aura-core-service` хранит организации, источники, отзывы, collection jobs и результаты анализа. Источник всегда принадлежит организации. Для `SourceType.MANUAL_IMPORT` реализован ручной импорт отзывов через CSV upload. Для `SourceType.TABITURIENT`, `SourceType.OTZOVIK` и `SourceType.VUZOPEDIA` реализован сбор публичных отзывов через HTML scraping на `jsoup`.

## Organization API

Организация управляется через:

- `POST /api/organizations`
- `GET /api/organizations`
- `GET /api/organizations/{id}`
- `PATCH /api/organizations/{id}`
- `DELETE /api/organizations/{id}`

`DELETE` выполняет soft delete: `isActive = false`.

Для организации также доступен AI insights report:

- `POST /api/organizations/{organizationId}/insights`

`GET /api/organizations` поддерживает фильтры:

- `name` - ищет по `name` и `shortName`
- `isActive` - фильтр по активным/неактивным организациям

Пример создания организации:

```json
{
  "name": "Дальневосточный федеральный университет",
  "shortName": "ДВФУ",
  "description": "Федеральный университет",
  "website": "https://www.dvfu.ru"
}
```

Пример генерации insights:

```bash
curl -X POST "http://localhost:8081/api/organizations/1/insights" \
  -H "Authorization: Bearer <user-or-admin-token>"
```

Force regeneration:

```bash
curl -X POST "http://localhost:8081/api/organizations/1/insights?force=true&limit=50&from=2026-04-01&to=2026-04-28" \
  -H "Authorization: Bearer <admin-token>"
```

Правила:

- `ROLE_USER` и `ROLE_ADMIN` могут получать cached insight и запускать обычную генерацию
- `force=true` доступен только `ROLE_ADMIN`
- если для организации уже есть insight и `force=false`, возвращается последний сохранённый отчёт
- cache привязан к самой организации; чтобы перестроить отчёт с другими `limit/from/to`, нужно использовать `force=true`
- для построения insights выбираются только `ANALYZED` отзывы этой организации
- `aura-core-service` не вызывает Gemini напрямую, а отправляет подготовленный payload в `aura-analysis-service /insights`
- текст каждого отзыва перед отправкой обрезается до `1500` символов

Пример response:

```json
{
  "organizationId": 1,
  "organizationName": "ДВФУ",
  "summary": "Общий фон отзывов смешанный: сильными сторонами остаются преподаватели и учебная часть, а основные проблемы связаны с общежитием и администрированием.",
  "strengths": ["Сильные преподаватели", "Хороший уровень обучения"],
  "weaknesses": ["Проблемы с общежитием", "Слабая административная коммуникация"],
  "recommendations": ["Улучшить процесс заселения", "Сделать ответы администрации быстрее"],
  "generatedAt": "2026-04-28T12:00:00Z",
  "modelVersion": "gemini-1.5-flash",
  "cached": false,
  "reviewsUsed": 50
}
```

## Source API

Источник создается через `POST /api/sources` и обязательно привязывается к организации.

Каждый источник можно запустить вручную в любой момент через `POST /api/collection/run/{sourceId}`.
Автоматический сбор по расписанию включается отдельно через `scheduleEnabled`.
`collectionMode` оставлен для обратной совместимости, но новая логика не считает ручной и scheduled запуск взаимоисключающими режимами.

`GET /api/sources` поддерживает фильтры:

- `organizationId`
- `name`
- `type`
- `isActive`
- `scheduleEnabled`

Пример:

```json
{
  "organizationId": 1,
  "name": "Manual import source",
  "type": "MANUAL_IMPORT",
  "baseUrl": "manual://csv",
  "scheduleEnabled": false,
  "description": "CSV import отзывов"
}
```

Источник в response содержит краткую организацию:

```json
{
  "id": 1,
  "organization": {
    "id": 1,
    "name": "Дальневосточный федеральный университет",
    "shortName": "ДВФУ"
  },
  "name": "Manual import source",
  "type": "MANUAL_IMPORT",
  "baseUrl": "manual://csv",
  "isActive": true,
  "collectionMode": "MANUAL",
  "scheduleEnabled": false,
  "scheduleIntervalMinutes": null,
  "lastCollectedAt": null,
  "nextCollectionAt": null,
  "description": "CSV import отзывов"
}
```

Пример источника для Tabiturient:

```json
{
  "organizationId": 1,
  "name": "Tabiturient ДВФУ",
  "type": "TABITURIENT",
  "baseUrl": "https://tabiturient.ru/vuzu/dvfu/",
  "scheduleEnabled": true,
  "scheduleIntervalMinutes": 1440,
  "description": "Сбор отзывов со страницы ДВФУ на tabiturient.ru"
}
```

Если `scheduleEnabled=false`, `scheduleIntervalMinutes` и `nextCollectionAt` очищаются.
Если `scheduleEnabled=true`, `scheduleIntervalMinutes` обязателен и должен быть от `15` до `43200` минут.

Для `TABITURIENT` `baseUrl` обязан соответствовать `https://tabiturient.ru/vuzu/{slug}/`. При создании и обновлении URL нормализуется к `https`.
Для `OTZOVIK` `baseUrl` обязан быть URL страницы отзывов `https://otzovik.com/reviews/.../`.
Для `VUZOPEDIA` `baseUrl` обязан соответствовать URL страницы отзывов `https://vuzopedia.ru/vuz/{id}/otziv`.

Пример источника для Otzovik:

```json
{
  "organizationId": 1,
  "name": "Otzovik ДВФУ",
  "type": "OTZOVIK",
  "baseUrl": "https://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu/",
  "scheduleEnabled": false,
  "scheduleIntervalMinutes": null
}
```

Пример источника для Vuzopedia:

```json
{
  "organizationId": 1,
  "name": "Vuzopedia ДВФУ",
  "type": "VUZOPEDIA",
  "baseUrl": "https://vuzopedia.ru/vuz/3281/otziv",
  "scheduleEnabled": false,
  "scheduleIntervalMinutes": null
}
```

## CSV Import

Импорт выполняется через:

```http
POST /api/sources/{sourceId}/import
Content-Type: multipart/form-data
```

Multipart field:

- `file` - CSV-файл

Доступ:

- только `ROLE_ADMIN`

Ожидаемый CSV с header row:

```csv
externalId,text,authorName,publishedAt,originalUrl,rating
1,"Отличные преподаватели и хороший кампус","Иван","2026-04-01T12:00:00Z","",5
2,"В общежитии грязно и неудобно","Мария","2026-04-02T15:30:00Z","",2
```

Обязательные колонки:

- `externalId`
- `text`
- `publishedAt`

Необязательные колонки:

- `authorName`
- `originalUrl`
- `rating`

Пример response:

```json
{
  "sourceId": 1,
  "fileName": "reviews.csv",
  "totalRows": 10,
  "importedCount": 8,
  "duplicateCount": 1,
  "invalidCount": 1
}
```

## Processing Flow

При `POST /api/sources/{sourceId}/import` выполняется сценарий:

1. Проверяется, что source существует и имеет тип `MANUAL_IMPORT`.
2. Backend читает CSV через Apache Commons CSV.
3. Невалидные строки пропускаются и учитываются в `invalidCount`.
4. Дубликаты по `(source_id, external_id)` пропускаются и учитываются в `duplicateCount`.
5. Новые отзывы сохраняются в `reviews`.
6. Тексты новых отзывов отправляются batch-запросом в `aura-analysis-service`.
7. Результаты сохраняются в `review_analysis`, а отзывы получают статус `ANALYZED`.

Если batch analysis не удался, отзывы сохраняются, но получают статус `FAILED_ANALYSIS`.

## Review List API

Список отзывов доступен через:

```http
GET /api/reviews
```

Поддерживаемые фильтры:

- `organizationId`
- `sourceId`
- `sentiment`
- `topic`
- `dateFrom`
- `dateTo`
- `keyword`

`keyword` ищет по ключевым словам анализа (`review_analysis.keywords`): поиск case-insensitive, с `trim` и частичным совпадением.

Пример:

```http
GET /api/reviews?organizationId=1&keyword=общеж&page=0&size=20
```

Популярные ключевые слова доступны через:

```http
GET /api/reviews/keywords/popular?organizationId=1&limit=20
```

Пример response:

```json
[
  {"keyword": "общежитие", "count": 34},
  {"keyword": "преподаватели", "count": 21}
]
```

## Reanalysis Failed Reviews

Если `aura-analysis-service` был недоступен, администратор может повторно отправить отзывы со статусом `FAILED_ANALYSIS` на анализ.
При `force=true` endpoint также повторно отправляет отзывы со статусом `ANALYZED`:

```bash
curl -X POST "http://localhost:8081/api/reviews/reanalyze?organizationId=1&limit=100" \
  -H "Authorization: Bearer <admin-token>"
```

Query params:

- `organizationId` - optional
- `sourceId` - optional
- `limit` - optional, default `100`
- `force` - optional, игнорирует лимит `reviews.analysis.max-retries` для `FAILED_ANALYSIS`
  и дополнительно переанализирует отзывы со статусом `ANALYZED`

Пример response:

```json
{
  "organizationId": 1,
  "sourceId": null,
  "requestedCount": 100,
  "reanalyzedCount": 85,
  "failedCount": 15,
  "skippedCount": 0,
  "errorMessage": "analysis-service unavailable"
}
```

При каждой попытке анализа у отзыва обновляются:

- `analysis_retry_count`
- `last_analysis_attempt_at`
- `analysis_error_message`

Если у отзыва уже есть существующий `review_analysis`, повторный успешный анализ обновляет его, а не создаёт новую запись.

## Review Summary API

Краткий конспект отзыва доступен через:

- `POST /api/reviews/{reviewId}/summary`

Пример:

```bash
curl -X POST "http://localhost:8081/api/reviews/1/summary" \
  -H "Authorization: Bearer <user-or-admin-token>"
```

Принудительная перегенерация доступна только администратору:

```bash
curl -X POST "http://localhost:8081/api/reviews/1/summary?force=true" \
  -H "Authorization: Bearer <admin-token>"
```

Поведение:

- если summary уже есть и `force=false`, сервис возвращает cached version из БД
- если summary отсутствует, `aura-core-service` вызывает `POST /summarize` в `aura-analysis-service`
- `aura-core-service` не вызывает OpenRouter напрямую и не хранит OpenRouter credentials
- при успешном ответе summary сохраняется в `reviews.summary`, вместе с `summary_generated_at` и `summary_model_version`
- при ошибке `analysis-service` существующий cached summary не затирается

Пример response:

```json
{
  "reviewId": 1,
  "summary": "Автор жалуется на нехватку мест в общежитии после первого курса и плохую организацию заселения.",
  "generatedAt": "2026-04-27T12:00:00Z",
  "modelVersion": "deepseek-openrouter-0.1.0",
  "cached": false
}
```

## Tabiturient Collector

Сбор запускается через существующий endpoint:

```http
POST /api/collection/run/{sourceId}
```

Для `SourceType.TABITURIENT` collector:

1. Проверяет и нормализует `baseUrl`.
2. Загружает одну публичную HTML-страницу вуза через `jsoup`.
3. Извлекает карточки отзывов из `#resultsliv`.
4. Нормализует текст, даты и ссылки.
5. Сохраняет только новые отзывы по уникальности `(source_id, external_id)`.
6. Отправляет тексты новых отзывов в `aura-analysis-service`.

Ограничения первой версии:

- scraping работает только для одной страницы вуза `tabiturient.ru/vuzu/{slug}/`
- за один запуск загружается только одна HTML-страница
- число обрабатываемых отзывов ограничено `tabiturient.scraper.max-reviews-per-run`
- collector использует только публичные данные
- при изменении HTML-структуры Tabiturient селекторы могут потребовать обновления

## OTZOVIK Collector

Сбор запускается через тот же endpoint:

```http
POST /api/collection/run/{sourceId}
```

Для `SourceType.OTZOVIK` collector:

1. Принимает URL страницы вида `https://otzovik.com/reviews/.../`.
2. Загружает первую страницу списка отзывов через `jsoup`.
3. Находит карточки `div[itemprop=review]`.
4. Достаёт ссылку на полный отзыв и переходит на страницу отзыва.
5. Парсит полный текст, заголовок, автора, дату, rating, достоинства и недостатки.
6. Если полный отзыв недоступен, использует teaser/pros/cons из карточки и продолжает сбор.
7. Сохраняет только новые отзывы по `(source_id, external_id)`.
8. Отправляет новые отзывы в существующий `aura-analysis-service`.

Collector поддерживает ручной запуск и автосбор через `scheduleEnabled`.
За один запуск количество отзывов ограничено `collectors.otzovik.max-reviews-per-run`.
Между запросами к полным отзывам используется задержка `collectors.otzovik.request-delay-ms`.

## VUZOPEDIA Collector

Сбор запускается через тот же endpoint:

```http
POST /api/collection/run/{sourceId}
```

Для `SourceType.VUZOPEDIA` collector:

1. Принимает URL страницы вида `https://vuzopedia.ru/vuz/{id}/otziv`.
2. Загружает текущую страницу отзывов через `jsoup`.
3. Находит карточки `div.otzivItem`.
4. Достаёт текст отзыва до блока `div.otzivInfo`, удаляет disclaimer и не сохраняет `div.otzPredAns`.
5. Парсит дату и автора из `div.otzivInfo`.
6. Генерирует stable `externalId` из vuz id, даты публикации и hash нормализованного текста.
7. Сохраняет только новые отзывы по `(source_id, external_id)`.
8. Отправляет новые отзывы в существующий `aura-analysis-service`.

Collector поддерживает ручной запуск и автосбор через `scheduleEnabled`.
В первой версии обрабатывается текущая страница без пагинации.
За один запуск количество отзывов ограничено `collectors.vuzopedia.max-reviews-per-run`.

## Configuration

`analysis-service` настраивается через:

```properties
analysis.service.url=${ANALYSIS_SERVICE_URL:http://localhost:8090}
spring.cloud.openfeign.client.config.analysisFeignClient.connect-timeout=${ANALYSIS_SERVICE_CONNECT_TIMEOUT_MS:5000}
spring.cloud.openfeign.client.config.analysisFeignClient.read-timeout=${ANALYSIS_SERVICE_READ_TIMEOUT_MS:15000}
spring.cloud.openfeign.client.config.analysisFeignClient.logger-level=${ANALYSIS_SERVICE_FEIGN_LOGGER_LEVEL:BASIC}
```

Scraper Tabiturient настраивается через:

```properties
tabiturient.scraper.user-agent=${TABITURIENT_USER_AGENT:AuraReviewBot/1.0}
tabiturient.scraper.timeout-ms=${TABITURIENT_TIMEOUT_MS:10000}
tabiturient.scraper.max-reviews-per-run=${TABITURIENT_MAX_REVIEWS_PER_RUN:50}
```

Scraper Otzovik настраивается через:

```properties
collectors.otzovik.max-reviews-per-run=${OTZOVIK_MAX_REVIEWS_PER_RUN:3}
collectors.otzovik.request-delay-ms=${OTZOVIK_REQUEST_DELAY_MS:12000}
collectors.otzovik.user-agent=${OTZOVIK_USER_AGENT:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36}
collectors.otzovik.timeout-ms=${OTZOVIK_TIMEOUT_MS:15000}
```

Scraper Vuzopedia настраивается через:

```properties
collectors.vuzopedia.max-reviews-per-run=${VUZOPEDIA_MAX_REVIEWS_PER_RUN:50}
collectors.vuzopedia.request-delay-ms=${VUZOPEDIA_REQUEST_DELAY_MS:500}
collectors.vuzopedia.user-agent=${VUZOPEDIA_USER_AGENT:Mozilla/5.0}
collectors.vuzopedia.timeout-ms=${VUZOPEDIA_TIMEOUT_MS:10000}
```

Retry / reanalysis настраивается через:

```properties
reviews.analysis.max-retries=${REVIEWS_ANALYSIS_MAX_RETRIES:5}
reviews.reanalysis.enabled=${REVIEWS_REANALYSIS_ENABLED:false}
reviews.reanalysis.fixed-delay-ms=${REVIEWS_REANALYSIS_FIXED_DELAY_MS:300000}
reviews.reanalysis.batch-size=${REVIEWS_REANALYSIS_BATCH_SIZE:100}
reviews.summary.max-input-length=${REVIEWS_SUMMARY_MAX_INPUT_LENGTH:20000}
```

Если `reviews.reanalysis.enabled=true`, scheduler периодически переотправляет отзывы со статусом `FAILED_ANALYSIS` на анализ и не запускает несколько retry batch одновременно.

Scheduler сбора источников настраивается через:

```properties
collection.scheduler.enabled=${COLLECTION_SCHEDULER_ENABLED:true}
collection.scheduler.fixed-delay-ms=${COLLECTION_SCHEDULER_FIXED_DELAY_MS:60000}
```

Если `collection.scheduler.enabled=false`, backend scheduler не запускает источники автоматически.

## Manual Run

1. Создайте организацию через `POST /api/organizations`.
2. Для ручного импорта создайте источник `MANUAL_IMPORT` и загрузите CSV через `POST /api/sources/{sourceId}/import`.
3. Для scraping создайте источник `TABITURIENT` с `baseUrl=https://tabiturient.ru/vuzu/{slug}/` и запустите `POST /api/collection/run/{sourceId}`. Ручной запуск работает независимо от `scheduleEnabled`.
4. Проверьте `GET /api/reviews`.
5. Проверьте `GET /api/dashboard?organizationId={organizationId}`, `GET /api/dashboard/summary?organizationId={organizationId}`, `GET /api/dashboard/sentiment-distribution?organizationId={organizationId}`, `GET /api/dashboard/topic-distribution?organizationId={organizationId}`.
