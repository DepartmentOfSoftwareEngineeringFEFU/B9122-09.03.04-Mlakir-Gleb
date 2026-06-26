# Aura Monorepo

Монорепозиторий дипломного проекта по теме: "РАЗРАБОТКА И РЕАЛИЗАЦИЯ ПРОТОТИПА ИНФОРМАЦИОННОЙ СИСТЕМЫ АВТОМАТИЧЕСКОГО СБОРА И ИНТЕЛЛЕКТУАЛЬНОГО АНАЛИЗА ТЕКСТОВЫХ ОТЗЫВОВ С ИСПОЛЬЗОВАНИЕМ МЕТОДОВ ОБРАБОТКИ ЕСТЕСТВЕННОГО ЯЗЫКА".

## Состав

- `aura-auth` - сервис аутентификации и авторизации на `Spring Boot` и `JWT`
- `aura-core` - основной backend для организаций, источников, отзывов и orchestration анализа
- `aura-analysis` - `FastAPI`-сервис интеллектуального анализа, summary и insights
- `aura-frontend` - web-интерфейс на `React`, `TypeScript` и `Vite`
- `aura-exception` - общий Spring Boot starter для унифицированной обработки ошибок

## Быстрый старт

1. При необходимости создайте локальный `.env` на основе `.env.example`.
2. Запустите весь стек из корня:

```bash
docker compose up --build -d
```

3. Проверить статус контейнеров:

```bash
docker compose ps
```

## Доступные сервисы

- frontend: `http://localhost:5173`
- auth API: `http://localhost:8080`
- core API: `http://localhost:8081`
- analysis API: `http://localhost:8090`

## Структура запуска

Корневой `docker-compose.yml` поднимает:

- `aura-auth-db` - PostgreSQL для `aura-auth`
- `aura-core-db` - PostgreSQL для `aura-core`
- `aura-auth` - auth backend
- `aura-core` - основной backend
- `aura-analysis` - NLP/AI backend
- `aura-frontend` - SPA через `nginx`

## Корневая Java-сборка

Корневой `pom.xml` поднимает единый Maven reactor для Java-модулей:

```bash
mvn -pl aura-auth/aura-auth-service -am package
mvn -pl aura-core/aura-core-service -am package
```

## Основные переменные окружения

В `.env.example` уже перечислены ключевые override-переменные:

- `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, `JWT_ISSUER`
- `AUTH_DB_*`, `CORE_DB_*`
- `GOOGLE_AI_API_KEY`, `GOOGLE_AI_MODEL`
- `VITE_API_BASE_URL`, `VITE_AUTH_BASE_URL`

Demo RSA keypair в `.env.example` предназначен только для локального запуска и демонстрации дипломного проекта.

## Полезные команды

Остановить стек:

```bash
docker compose down
```

Пересобрать и поднять заново:

```bash
docker compose up --build -d
```

Посмотреть логи:

```bash
docker compose logs -f
```

## Документация по сервисам

- [aura-auth](./aura-auth/README.md)
- [aura-core](./aura-core/README.md)
- [aura-core-service](./aura-core/aura-core-service/README.md)
- [aura-analysis](./aura-analysis/README.md)
- [aura-frontend](./aura-frontend/README.md)
- [aura-exception](./aura-exception/README.md)
