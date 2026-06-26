# Aura Frontend

Frontend-приложение для дипломного проекта по автоматическому сбору и анализу отзывов об университете. Интерфейс ориентирован на аналитика или администратора и работает поверх `aura-auth-service` и `aura-core-service`.

## Стек

- React 19
- TypeScript
- Vite
- React Router
- Axios
- Zustand
- TanStack Query
- React Hook Form + Zod
- Tailwind CSS
- ESLint
- Prettier

## Возможности

- Авторизация и регистрация через `aura-auth-service`
- Управление организациями и привязка источников к организации
- Dashboard с ключевой аналитикой по отзывам
- Просмотр списка источников и запуск сбора
- Создание новых источников
- Поддержка источников `MANUAL_IMPORT`, `TABITURIENT`, `OTZOVIK` и `VUZOPEDIA`
- Ручной импорт отзывов из CSV для источников `MANUAL_IMPORT`
- Ручной запуск сбора доступен всегда
- Дополнительная настройка автоматического сбора по расписанию для источников
- Просмотр отзывов с фильтрами, сортировкой и пагинацией
- Поиск и фильтрация отзывов по ключевым словам
- Блок популярных ключевых слов для быстрого применения фильтра
- Детальная карточка отзыва с результатом анализа
- Повторный запуск анализа отзывов со статусом `FAILED_ANALYSIS`
- Генерация краткого конспекта отзыва через AI по запросу пользователя
- Генерация ИИ-отчёта по организации на dashboard

## Запуск локально

```bash
npm install
npm run dev
```

По умолчанию приложение будет доступно на `http://localhost:5173`.

## Макет-версия без backend

Для дипломной демонстрации доступен отдельный макетный режим с локальными данными и упрощённым визуальным стилем.

```bash
npm run dev:mockup
```

В этом режиме:

- backend не требуется
- данные берутся из локального mock-store
- используется упрощённый `mockup`-дизайн
- для входа можно использовать `demo-admin / demo123` или `demo-user / demo123`

## Сборка

```bash
npm run build
```

## Линтер и форматирование

```bash
npm run lint
npm run format
```

## Переменные окружения

Создайте `.env` на основе `.env.example`.

```env
VITE_API_BASE_URL=http://localhost:8081
VITE_AUTH_BASE_URL=http://localhost:8080
```

### Назначение переменных

- `VITE_API_BASE_URL`: base URL для `aura-core-service`
- `VITE_AUTH_BASE_URL`: base URL для `aura-auth-service`

## Интеграция с backend

Frontend использует два backend-сервиса:

- `aura-auth-service`
  - `POST /auth/login`
  - `POST /auth/register`
  - `POST /auth/logout`
- `aura-core-service`
  - `GET /api/organizations`
  - `POST /api/organizations`
  - `GET /api/organizations/{id}`
  - `PATCH /api/organizations/{id}`
  - `DELETE /api/organizations/{id}`
  - `GET /api/dashboard/summary`
  - `GET /api/dashboard/sentiment-distribution`
  - `GET /api/dashboard/topic-distribution`
- `GET /api/sources`
  - `POST /api/sources`
  - `PATCH /api/sources/{id}`
  - `POST /api/sources/{sourceId}/import`
  - `GET /api/reviews`
  - `GET /api/reviews/keywords/popular`
  - `GET /api/reviews/{id}`
  - `POST /api/reviews/reanalyze`
  - `POST /api/reviews/{reviewId}/summary`
  - `POST /api/organizations/{organizationId}/insights`
  - `POST /api/collection/run/{sourceId}`
  - `GET /api/collection/jobs`

JWT access token сохраняется в `localStorage`, автоматически подставляется в `Authorization: Bearer <token>`, а при `401` пользователь перенаправляется на `/login`.

### Работа с источниками

В системе источники принадлежат организации. Базовый сценарий работы:

1. создать организацию
2. создать для нее источник `MANUAL_IMPORT`, `TABITURIENT`, `OTZOVIK` или `VUZOPEDIA`
3. при необходимости включить для источника автоматический сбор по расписанию
4. для `MANUAL_IMPORT` импортировать отзывы из CSV
5. в любой момент запускать сбор вручную кнопкой `Запустить сбор`
6. просматривать отзывы и аналитику

Если организаций еще нет, форма создания источника предлагает сначала перейти к созданию организации.

### Платформы

- `MANUAL_IMPORT` — ручной импорт отзывов через CSV после создания источника
- `TABITURIENT` — автоматический сбор отзывов со страницы вуза на `tabiturient.ru`
- `OTZOVIK` — автоматический сбор отзывов со страницы организации на `otzovik.com`
- `VUZOPEDIA` — автоматический сбор отзывов со страницы вуза на `vuzopedia.ru`

Для `TABITURIENT` укажите ссылку вида:

```text
https://tabiturient.ru/vuzu/dvfu/
```

Для `OTZOVIK` укажите ссылку вида:

```text
https://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu/
```

Для `VUZOPEDIA` укажите ссылку вида:

```text
https://vuzopedia.ru/vuz/3281/otziv
```

Поддерживаемые источники:

- Tabiturient
- Otzovik
- Vuzopedia

Пример Otzovik source:

```json
{
  "organizationId": 1,
  "name": "Otzovik ДВФУ",
  "type": "OTZOVIK",
  "baseUrl": "https://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu/",
  "scheduleEnabled": false
}
```

Пример Vuzopedia source:

```json
{
  "organizationId": 1,
  "name": "Vuzopedia ДВФУ",
  "type": "VUZOPEDIA",
  "baseUrl": "https://vuzopedia.ru/vuz/3281/otziv",
  "scheduleEnabled": false
}
```

После создания источника ручной запуск сбора остаётся доступен всегда. Дополнительно для источника можно включить автоматический сбор по расписанию. Собранные отзывы появятся в списке отзывов, а dashboard обновится после завершения фоновой обработки.

### Импорт CSV

Для источников типа `MANUAL_IMPORT` на странице `/sources` доступна кнопка `Импортировать отзывы`. Она открывает диалог загрузки CSV-файла и отправляет его в backend через `multipart/form-data`.

Ожидаемые колонки CSV:

- `externalId`
- `text`
- `authorName`
- `publishedAt`
- `originalUrl`
- `rating`

Обязательные колонки:

- `externalId`
- `text`
- `publishedAt`

Пример:

```csv
externalId,text,authorName,publishedAt,originalUrl,rating
1,"Отличные преподаватели и хороший кампус","Иван","2026-04-01T12:00:00Z","",5
2,"В общежитии грязно и неудобно","Мария","2026-04-02T15:30:00Z","",2
```

### Повторный анализ отзывов

Если анализ временно не сработал из-за недоступности `aura-analysis-service`, администратор может повторно отправить отзывы на анализ из интерфейса.

- Кнопка `Повторить анализ` доступна на страницах `/reviews` и `/dashboard` только для `ROLE_ADMIN`
- В диалоге можно выбрать организацию, источник, лимит batch (`1..1000`) и флаг `force`
- После запуска frontend обновляет список отзывов, dashboard, источники и связанные фоновые задания

### Конспект от ИИ

### Поиск по ключевым словам

Ключевые слова используются для быстрого поиска и фильтрации отзывов по часто встречающимся проблемам или преимуществам.

На странице `/reviews` также отображается блок популярных ключевых слов. Frontend получает его через `GET /api/reviews/keywords/popular` и позволяет одним нажатием применить выбранное слово как фильтр. Если выбрана организация, backend возвращает популярные keywords в рамках этой организации.

На странице конкретного отзыва доступна кнопка `Конспект от ИИ`. Frontend не вызывает LLM напрямую: он отправляет запрос только в `aura-core-service`, а backend сам запрашивает и кэширует summary.

- Конспект создаётся только по нажатию пользователя
- Повторное раскрытие блока использует уже полученный результат без нового запроса
- Backend может вернуть уже готовый результат из кэша
- Для `ROLE_ADMIN` доступна кнопка `Сгенерировать заново`, которая принудительно запрашивает новый summary через `force=true`

### ИИ-отчёт по организации

На dashboard выбранной организации доступна карточка `ИИ-отчёт по отзывам`. Frontend не вызывает Gemini напрямую и не содержит API key: он отправляет запрос только в `aura-core-service`.

- Отчёт генерируется только по нажатию пользователя
- В отчёте показываются summary, сильные стороны, проблемы и рекомендации
- Backend может вернуть уже кэшированный результат
- Для `ROLE_ADMIN` доступна кнопка `Обновить отчёт` с принудительной перегенерацией

## Основные страницы

- `/login` — вход в систему
- `/register` — регистрация
- `/dashboard` — обзор по отзывам и аналитике
- `/organizations` — список организаций
- `/organizations/new` — создание организации
- `/sources` — список источников и управление сбором
- `/sources/new` — создание источника
- `/custom-sources` — UI-заготовка для будущих пользовательских источников
- `/reviews` — список отзывов с фильтрами
- `/reviews/:id` — детальная карточка отзыва

## Платформы отзывов

Раздел пользовательских платформ находится в разработке. Интерфейс демонстрирует два планируемых способа подключения новых источников отзывов: HTML-скрапинг и API-интеграция. Backend-реализация универсальных collectors запланирована как направление дальнейшего развития.

## Структура проекта

```text
src/
  api/           # axios-клиенты и API-модули
  app/           # router и query client
  components/    # layout, ui и общие компоненты
  features/      # query hooks и auth store
  lib/           # форматирование, константы, class helpers
  pages/         # страницы приложения
  types/         # DTO и доменные типы
```
