# aura-core

Основной backend дипломного проекта по теме: "РАЗРАБОТКА И РЕАЛИЗАЦИЯ ПРОТОТИПА ИНФОРМАЦИОННОЙ СИСТЕМЫ АВТОМАТИЧЕСКОГО СБОРА И ИНТЕЛЛЕКТУАЛЬНОГО АНАЛИЗА ТЕКСТОВЫХ ОТЗЫВОВ С ИСПОЛЬЗОВАНИЕМ МЕТОДОВ ОБРАБОТКИ ЕСТЕСТВЕННОГО ЯЗЫКА".

## Назначение

`aura-core` хранит и обслуживает:

- организации
- источники отзывов
- отзывы
- результаты анализа отзывов
- задания на сбор данных
- AI summary и insights по отзывам и организациям

Основной runtime-модуль: `aura-core-service`.

## Доменная модель

Сервис использует модель:

`Organization -> Source -> Review -> ReviewAnalysis`

Поддерживаются источники:

- `MANUAL_IMPORT`
- `TABITURIENT`
- `OTZOVIK`
- `VUZOPEDIA`

## Роли и доступ

В системе используются:

- `ROLE_ADMIN` - управление организациями и источниками, запуск сбора, повторный анализ, доступ к jobs
- `ROLE_USER` - просмотр организаций, источников, отзывов и dashboard

Security реализуется через `aura-auth-starter`.

## Основные возможности

- CRUD для организаций и источников
- импорт CSV для `MANUAL_IMPORT`
- HTML scraping для `TABITURIENT`, `OTZOVIK` и `VUZOPEDIA`
- ручной и scheduled запуск сбора
- batch-анализ новых отзывов через `aura-analysis`
- повторный анализ отзывов со статусом `FAILED_ANALYSIS`
- summary конкретного отзыва
- AI insights по отзывам выбранной организации
- dashboard с агрегированной аналитикой

## Основные API-группы

- `/api/organizations`
- `/api/sources`
- `/api/reviews`
- `/api/dashboard`
- `/api/collection`

Подробности по runtime-модулю описаны в [aura-core-service/README.md](./aura-core-service/README.md).

## Интеграция с aura-analysis

`aura-core-service` не вызывает модели напрямую. Внешний анализ выполняется через HTTP-вызовы в `aura-analysis-service`:

- `POST /analyze/batch`
- `POST /summarize`
- `POST /insights`

## Запуск в составе монорепозитория

Из корня проекта:

```bash
docker compose up --build -d
```

Сервис будет доступен на `http://localhost:8081`.

## Локальный запуск модуля

1. Поднять PostgreSQL:

```bash
docker compose up -d aura-core-db
```

2. Запустить `aura-analysis-service` на `http://localhost:8090`.

3. Запустить сервис из директории `aura-core`:

```bash
ANALYSIS_SERVICE_URL=http://localhost:8090 mvn -pl aura-core-service spring-boot:run
```

## Основные переменные окружения

- `ANALYSIS_SERVICE_URL`
- `JWT_PUBLIC_KEY`
- `JWT_ISSUER`
- `DB_URL`, `DB_USER`, `DB_PASS`
- `REVIEWS_ANALYSIS_MAX_RETRIES`
- `REVIEWS_REANALYSIS_ENABLED`
- `REVIEWS_SUMMARY_MAX_INPUT_LENGTH`
- `COLLECTION_SCHEDULER_ENABLED`
- `COLLECTION_SCHEDULER_FIXED_DELAY_MS`
- `OTZOVIK_*`
- `VUZOPEDIA_*`
- `TABITURIENT_*`

Основной runtime-конфиг расположен в [aura-core-service/src/main/resources/application.properties](/Users/glebmlakir/IdeaProjects/B9122-09.03.04-Mlakir-Gleb/aura-core/aura-core-service/src/main/resources/application.properties:1).

## Пример end-to-end сценария

1. Создать организацию через `/api/organizations`
2. Создать источник
3. Для `MANUAL_IMPORT` загрузить CSV через `/api/sources/{sourceId}/import`
4. Для scraping-источников запустить `/api/collection/run/{sourceId}`
5. Дождаться анализа через `aura-analysis`
6. Просматривать отзывы, dashboard, summary и insights
