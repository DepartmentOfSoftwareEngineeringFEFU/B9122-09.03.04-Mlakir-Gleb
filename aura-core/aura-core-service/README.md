# aura-core-service

Исполняемый Spring Boot модуль сервиса `aura-core` для дипломного проекта по теме: "РАЗРАБОТКА И РЕАЛИЗАЦИЯ ПРОТОТИПА ИНФОРМАЦИОННОЙ СИСТЕМЫ АВТОМАТИЧЕСКОГО СБОРА И ИНТЕЛЛЕКТУАЛЬНОГО АНАЛИЗА ТЕКСТОВЫХ ОТЗЫВОВ С ИСПОЛЬЗОВАНИЕМ МЕТОДОВ ОБРАБОТКИ ЕСТЕСТВЕННОГО ЯЗЫКА".

## Что делает модуль

`aura-core-service` отвечает за:

- API для организаций, источников, отзывов и dashboard
- импорт CSV
- scraping внешних источников
- запуск и учёт collection jobs
- orchestration batch-анализа через `aura-analysis-service`
- генерацию summary и organization insights

## Основные API

- `POST /api/organizations`
- `GET /api/organizations`
- `POST /api/sources`
- `GET /api/sources`
- `POST /api/sources/{sourceId}/import`
- `POST /api/collection/run/{sourceId}`
- `GET /api/reviews`
- `POST /api/reviews/reanalyze`
- `POST /api/reviews/{reviewId}/summary`
- `POST /api/organizations/{organizationId}/insights`
- `GET /api/dashboard/*`

## Источники данных

Поддерживаются:

- `MANUAL_IMPORT`
- `TABITURIENT`
- `OTZOVIK`
- `VUZOPEDIA`

## Запуск

В составе монорепозитория:

```bash
docker compose up --build -d
```

Локально из директории `aura-core`:

```bash
ANALYSIS_SERVICE_URL=http://localhost:8090 mvn -pl aura-core-service spring-boot:run
```

## Основные настройки

- `ANALYSIS_SERVICE_URL`
- `JWT_PUBLIC_KEY`
- `JWT_ISSUER`
- `REVIEWS_REANALYSIS_*`
- `REVIEWS_SUMMARY_MAX_INPUT_LENGTH`
- `COLLECTION_SCHEDULER_*`
- `TABITURIENT_*`
- `OTZOVIK_*`
- `VUZOPEDIA_*`

## Дополнительная документация

Более широкий контекст по модулю находится в [../README.md](../README.md).
