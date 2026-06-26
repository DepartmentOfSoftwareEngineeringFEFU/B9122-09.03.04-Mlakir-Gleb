# aura-auth

Сервис аутентификации и авторизации дипломного проекта по теме: "Разработка и реализация прототипа информационной системы автоматического сбора и интеллектуального анализа текстовых отзывов с использованием методов обработки естественного языка".

## Назначение

`aura-auth` отвечает за:

- регистрацию и аутентификацию пользователей
- выпуск и обновление `JWT`
- завершение текущей или всех активных сессий
- переиспользуемую security-автоконфигурацию для других Spring-сервисов

## Модули

- `aura-auth-api` - REST API интерфейсы и DTO
- `aura-auth-common` - общая JWT-логика, парсинг токенов и валидаторы
- `aura-auth-starter` - Spring Security autoconfiguration, `JWT` filter, `CORS`, auth handlers
- `aura-auth-service` - исполняемое приложение, JPA-сущности, репозитории и Liquibase

## Технологии

- Java 21
- Spring Boot 3.3.10
- Spring Security
- Spring Data JPA
- Springdoc OpenAPI
- PostgreSQL
- Liquibase
- JJWT
- Maven

## Основные endpoints

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/validate`
- `POST /auth/logout/all`

Swagger доступен после запуска сервиса по `swagger-ui`.

## Запуск в составе монорепозитория

Из корня проекта:

```bash
cp .env.example .env
docker compose up --build -d
```

Сервис будет доступен на `http://localhost:8080`.

При запуске через корневой `docker compose` demo-администратор создаётся только если в `.env` включён bootstrap:

- login: `demo-admin`
- password: `demo123`

Повторно пользователь не создаётся, если Docker volume с БД уже существует.

## Локальный запуск модуля

Нужно подготовить:

- Java 21
- Maven
- PostgreSQL
- RSA key pair для `JWT`

Запуск из директории `aura-auth`:

```bash
mvn clean verify
mvn -pl aura-auth-service spring-boot:run
```

## Основные переменные окружения

- `JWT_PRIVATE_KEY` - приватный ключ для подписи JWT
- `JWT_PUBLIC_KEY` - публичный ключ для верификации JWT
- `JWT_ISSUER` - issuer токенов, по умолчанию `aura-auth`
- `JWT_DEFAULT_USER_ROLE` - роль по умолчанию при регистрации
- `BOOTSTRAP_ADMIN_ENABLED` - включает автосоздание demo-администратора
- `BOOTSTRAP_ADMIN_LOGIN` - login bootstrap-администратора
- `BOOTSTRAP_ADMIN_PASSWORD` - пароль bootstrap-администратора
- `ACCESS_TOKEN_EXPIRATION_SECONDS` - TTL access token
- `REFRESH_TOKEN_EXPIRATION_SECONDS` - TTL refresh token
- `CORS_ALLOWED_ORIGINS` - список разрешённых origins
- `DB_URL`, `DB_USER`, `DB_PASS` - параметры подключения к БД
- `SERVER_PORT` - HTTP-порт сервиса

Основной runtime-конфиг расположен в [aura-auth-service/src/main/resources/application.properties](/Users/glebmlakir/IdeaProjects/B9122-09.03.04-Mlakir-Gleb/aura-auth/aura-auth-service/src/main/resources/application.properties:1).

## aura-auth-starter

`aura-auth-starter` используется в других Spring-сервисах монорепозитория для:

- проверки входящих Bearer JWT
- типовых `401` и `403` ответов в формате `ProblemDetail`
- настройки `CORS`
- прокидывания `Authorization` header в `OpenFeign`

Пример подключения:

```xml
<dependency>
    <groupId>mlakir</groupId>
    <artifactId>aura-auth-starter</artifactId>
    <version>${aura-auth.version}</version>
</dependency>
```

Минимальные настройки:

```properties
aura.security.jwt.verification.public-key=${JWT_PUBLIC_KEY}
aura.security.jwt.verification.jwt-issuer=aura-auth
aura.security.cors.allowed-origins=http://localhost:5173
```

## База данных

Liquibase changelog находится в [aura-auth-service/src/main/resources/db/changelog.yaml](/Users/glebmlakir/IdeaProjects/B9122-09.03.04-Mlakir-Gleb/aura-auth/aura-auth-service/src/main/resources/db/changelog.yaml:1).

Миграции создают:

- схему `aura_auth`
- таблицы пользователей
- таблицы ролей
- таблицу credentials
- таблицу token sessions

## Тесты

Запуск тестов:

```bash
mvn test
```
