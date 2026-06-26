# Aura Monorepo

Монорепозиторий дипломного проекта по автоматическому сбору, анализу и визуализации отзывов об университетах.

## Состав

- `aura-auth` - JWT-аутентификация и авторизация
- `aura-core` - организации, источники, отзывы, сбор и orchestration анализа
- `aura-analysis` - FastAPI-сервис анализа, summary и insights
- `aura-frontend` - React/Vite frontend
- `aura-exception` - общий Spring Boot starter для `ProblemDetail`

## Быстрый запуск через Docker Compose

1. При необходимости создайте локальный `.env` на основе `.env.example`.
2. Запустите проект:

```bash
docker compose up --build
```

После старта будут доступны:

- frontend: `http://localhost:5173`
- auth API: `http://localhost:8080`
- core API: `http://localhost:8081`
- analysis API: `http://localhost:8090`

## Что запускает compose

- два отдельных PostgreSQL-контейнера для `aura-auth` и `aura-core`
- `aura-analysis` как Python/FastAPI сервис
- `aura-auth` и `aura-core`, собранные из корня монорепозитория
- `aura-frontend` как статический SPA через `nginx`

## Локальная Java-сборка из корня

Корневой `pom.xml` теперь поднимает единый Maven reactor для Java-модулей:

```bash
mvn -pl aura-core/aura-core-service -am package
mvn -pl aura-auth/aura-auth-service -am package
```

## Переменные окружения

Основные override-переменные уже перечислены в `.env.example`:

- `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, `JWT_ISSUER`
- `AUTH_DB_*`, `CORE_DB_*`
- `GOOGLE_AI_API_KEY`
- `VITE_API_BASE_URL`, `VITE_AUTH_BASE_URL`

В `.env.example` лежит demo RSA keypair только для локального запуска и защиты диплома. Для любого внешнего окружения ключи нужно заменить.

## Замечание по Git

Сейчас сервисные директории всё ещё содержат свои внутренние `.git` каталоги. Для публикации именно как одного чистого монорепозитория их нужно убрать при переносе истории в общий корневой git-репозиторий.
