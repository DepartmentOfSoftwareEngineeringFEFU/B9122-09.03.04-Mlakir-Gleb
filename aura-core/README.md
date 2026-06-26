# aura-core

`aura-core` хранит организации, источники отзывов, сами отзывы и результаты их анализа. Основной runtime-модуль: `aura-core-service`.

## Roles

В системе используются две роли:

- `ROLE_ADMIN` - управление организациями и источниками, запуск сбора, просмотр collection jobs, просмотр отзывов и dashboard
- `ROLE_USER` - просмотр организаций, источников, отзывов, деталей отзывов и dashboard

Матрица доступа:

- `ROLE_ADMIN`: `POST/PATCH/DELETE /api/organizations`, `GET /api/organizations`, `GET /api/organizations/{id}`, `POST /api/organizations/{organizationId}/insights`, `POST/PATCH/DELETE /api/sources`, `GET /api/sources`, `GET /api/sources/{id}`, `POST /api/collection/run/{sourceId}`, `GET /api/collection/jobs`, `GET /api/collection/jobs/{id}`, `GET /api/reviews`, `GET /api/reviews/{id}`, `POST /api/reviews/{reviewId}/summary`, `POST /api/reviews/reanalyze`, `GET /api/dashboard/*`
- `ROLE_USER`: `GET /api/organizations`, `GET /api/organizations/{id}`, `POST /api/organizations/{organizationId}/insights`, `GET /api/sources`, `GET /api/sources/{id}`, `GET /api/reviews`, `GET /api/reviews/{id}`, `POST /api/reviews/{reviewId}/summary`, `GET /api/dashboard/*`

## Domain Model

Сервис поддерживает модель `Organization -> Source -> Review -> ReviewAnalysis`. Источники создаются внутри организаций. Основной сценарий загрузки отзывов реализован через `SourceType.MANUAL_IMPORT` и CSV import endpoint. HTML scraping поддерживается для `SourceType.TABITURIENT`, `SourceType.OTZOVIK` и `SourceType.VUZOPEDIA`.
Каждый источник можно запустить вручную в любой момент. Дополнительно для источника можно включить автоматический сбор через `scheduleEnabled` и `scheduleIntervalMinutes`; backend scheduler сам запускает due sources.

`GET /api/organizations` поддерживает фильтры `name` и `isActive`.
`GET /api/sources` поддерживает фильтры `organizationId`, `name`, `type`, `isActive`, `scheduleEnabled`.
`GET /api/reviews` поддерживает фильтр `keyword` по `review_analysis.keywords` вместе с остальными фильтрами списка.
`GET /api/reviews/keywords/popular` возвращает популярные ключевые слова.

## Analysis Integration

`aura-core-service` отправляет новые отзывы во внешний `aura-analysis-service` по HTTP через Spring Cloud OpenFeign и сохраняет результат в `review_analysis`.

Основной сценарий:

1. Запускается collection job или CSV import.
2. `aura-core-service` собирает и сохраняет новые отзывы.
3. Тексты новых отзывов отправляются в `POST /analyze/batch` внешнего `aura-analysis-service`.
4. Ответ маппится в `ReviewAnalysisEntity`.
5. Отзывы получают статус `ANALYZED`.
6. Job или import завершается успешно.

Если batch-анализ не удался:

1. Collection job завершается со статусом `FAILED`, либо import завершает сохранение без потери данных.
2. Новые отзывы, не дошедшие до успешного анализа, получают статус `FAILED_ANALYSIS`.
3. Текст ошибки сохраняется в `collection_jobs.error_message`, если сценарий шел через collection job.

## Reanalysis / Retry

Если `aura-analysis-service` был недоступен или временно отдавал ошибку, отзывы не теряются и остаются в `FAILED_ANALYSIS`. Администратор может повторно отправить их на анализ вручную:

```bash
curl -X POST "http://localhost:8081/api/reviews/reanalyze?organizationId=1&limit=100" \
  -H "Authorization: Bearer <admin-token>"
```

Поддерживаемые query params:

- `organizationId` - optional
- `sourceId` - optional
- `limit` - optional, default `100`
- `force` - optional, игнорирует лимит `REVIEWS_ANALYSIS_MAX_RETRIES` для `FAILED_ANALYSIS`
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

Для автоматического retry flow доступны настройки:

- `REVIEWS_ANALYSIS_MAX_RETRIES` - максимальное число автоматических retry, default `5`
- `REVIEWS_REANALYSIS_ENABLED` - включает scheduler, default `false`
- `REVIEWS_REANALYSIS_FIXED_DELAY_MS` - период scheduler, default `300000`
- `REVIEWS_REANALYSIS_BATCH_SIZE` - размер scheduled batch, default `100`

## Review Summary

Frontend может запросить краткий конспект конкретного отзыва через `aura-core-service`, но сам `aura-core-service` не вызывает OpenRouter напрямую. Summary генерируется только через HTTP-вызов в `aura-analysis-service`.

Endpoint:

```bash
curl -X POST "http://localhost:8081/api/reviews/1/summary" \
  -H "Authorization: Bearer <user-or-admin-token>"
```

Для принудительной перегенерации:

```bash
curl -X POST "http://localhost:8081/api/reviews/1/summary?force=true" \
  -H "Authorization: Bearer <admin-token>"
```

Правила:

- если `summary` уже сохранён, сервис возвращает его из БД с `cached=true`
- если `summary` отсутствует, `aura-core-service` вызывает `POST /summarize` в `aura-analysis-service`, сохраняет результат и возвращает `cached=false`
- `force=true` игнорирует cache, но доступен только `ROLE_ADMIN`
- `ROLE_USER` и `ROLE_ADMIN` могут вызывать endpoint только с JWT
- текст отзыва при необходимости обрезается до `REVIEWS_SUMMARY_MAX_INPUT_LENGTH` перед отправкой в `aura-analysis-service`

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

## Configuration

`aura-core-service` использует переменные для интеграции с `aura-analysis-service`:

- `ANALYSIS_SERVICE_URL` - базовый URL analysis-service, по умолчанию `http://localhost:8090`
- `ANALYSIS_SERVICE_CONNECT_TIMEOUT_MS` - connect timeout Feign-клиента, по умолчанию `5000`
- `ANALYSIS_SERVICE_READ_TIMEOUT_MS` - read timeout Feign-клиента, по умолчанию `15000`
- `ANALYSIS_SERVICE_FEIGN_LOGGER_LEVEL` - уровень логирования Feign, по умолчанию `BASIC`
- `REVIEWS_ANALYSIS_MAX_RETRIES` - максимальное число автоматических retry, по умолчанию `5`
- `REVIEWS_REANALYSIS_ENABLED` - включает scheduled retry, по умолчанию `false`
- `REVIEWS_REANALYSIS_FIXED_DELAY_MS` - fixed delay scheduler, по умолчанию `300000`
- `REVIEWS_REANALYSIS_BATCH_SIZE` - размер scheduled batch, по умолчанию `100`
- `REVIEWS_SUMMARY_MAX_INPUT_LENGTH` - лимит текста для summary endpoint, по умолчанию `20000`
- `COLLECTION_SCHEDULER_ENABLED` - включает scheduled collection источников, по умолчанию `true`
- `COLLECTION_SCHEDULER_FIXED_DELAY_MS` - период проверки due sources, по умолчанию `60000`
- `OTZOVIK_MAX_REVIEWS_PER_RUN` - максимум Otzovik отзывов за один запуск, по умолчанию `100`
- `OTZOVIK_REQUEST_DELAY_MS` - задержка между запросами к полным Otzovik отзывам, по умолчанию `12000`
- `OTZOVIK_USER_AGENT` - User-Agent для Otzovik scraping, по умолчанию browser-like `Mozilla/5.0`
- `VUZOPEDIA_MAX_REVIEWS_PER_RUN` - максимум Vuzopedia отзывов за один запуск, по умолчанию `100`
- `VUZOPEDIA_REQUEST_DELAY_MS` - задержка перед запросом к Vuzopedia, по умолчанию `500`
- `VUZOPEDIA_USER_AGENT` - User-Agent для Vuzopedia scraping, по умолчанию `Mozilla/5.0`
- `VUZOPEDIA_TIMEOUT_MS` - timeout Vuzopedia scraping, по умолчанию `10000`

## Organization Insights

`aura-core-service` может построить AI-отчёт по отзывам конкретной организации, но сам не вызывает Gemini напрямую. Генерация выполняется только через `aura-analysis-service` и его endpoint `POST /insights`.

Endpoint:

```bash
curl -X POST "http://localhost:8081/api/organizations/1/insights" \
  -H "Authorization: Bearer <user-or-admin-token>"
```

Пример c `force` и фильтрами:

```bash
curl -X POST "http://localhost:8081/api/organizations/1/insights?force=true&limit=50&from=2026-04-01&to=2026-04-28" \
  -H "Authorization: Bearer <admin-token>"
```

Поведение:

- если для организации уже есть insight и `force=false`, возвращается последний сохранённый отчёт с `cached=true`
- cache привязан к организации, а не к комбинации `limit/from/to`
- если нужен новый отчёт по другим фильтрам, нужно вызывать `force=true`
- обычная генерация доступна авторизованным `ROLE_USER` и `ROLE_ADMIN`
- `force=true` доступен только `ROLE_ADMIN`
- для построения отчёта выбираются только отзывы организации со статусом `ANALYZED`
- используется `limit`, по умолчанию `50`, максимум `100`
- каждый текст отзыва перед отправкой в `aura-analysis-service` обрезается до `1500` символов

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

## Local Run

1. Поднимите PostgreSQL:

```bash
docker compose up -d aura-core-database
```

2. Запустите `aura-analysis-service` на `http://localhost:8090`.

3. Запустите `aura-core-service`:

```bash
ANALYSIS_SERVICE_URL=http://localhost:8090 mvn -pl aura-core-service spring-boot:run
```

## End-to-End Example

1. Создайте организацию через `POST /api/organizations`.
2. Создайте источник типа `MANUAL_IMPORT`, указав `organizationId`.
3. Загрузите CSV через `POST /api/sources/{sourceId}/import`.
4. `aura-core-service` сохранит новые отзывы.
5. `aura-core-service` вызовет `aura-analysis-service /analyze/batch`.
6. Результаты анализа сохранятся в БД и станут доступны через API/frontend-потоки.
