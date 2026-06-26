# aura-auth

Сервис аутентификации и авторизации на `Spring Boot`, `JWT`, `PostgreSQL` и `Liquibase`.

Проект собран как multi-module Maven repository и разделен на API-контракты, общий auth-код, Spring Boot starter и исполняемый сервис.

## Модули

- `aura-auth-api` - REST API интерфейсы и DTO.
- `aura-auth-common` - общая JWT-логика, парсинг токенов, валидаторы, фабрика auth-ошибок.
- `aura-auth-starter` - автоконфигурация Spring Security, JWT filter, CORS, обработчики auth-ошибок.
- `aura-auth-service` - исполняемое приложение, JPA-сущности, репозитории, бизнес-логика, Liquibase-миграции.

## Стек

- Java 21
- Spring Boot 3.3.10
- Spring Security
- Spring Data JPA
- Springdoc OpenAPI
- PostgreSQL
- Liquibase
- JJWT
- Maven

## API

Основные endpoints:

- `POST /login` - аутентификация пользователя.
- `POST /register` - регистрация пользователя.
- `POST /refresh` - обновление пары access/refresh JWT.
- `POST /logout` - завершение текущей сессии по access token.
- `GET /validate` - проверка валидности access token.
- `POST /logout/all` - завершение всех активных сессий пользователя.

Swagger доступен по `swagger-ui` после запуска приложения.

## Конфигурация

Основные переменные окружения:

- `JWT_PRIVATE_KEY` - приватный ключ для подписи JWT.
- `JWT_PUBLIC_KEY` - публичный ключ для верификации JWT.
- `ACCESS_TOKEN_EXPIRATION_SECONDS` - TTL access token, по умолчанию `300`.
- `REFRESH_TOKEN_EXPIRATION_SECONDS` - TTL refresh token, по умолчанию `604800`.
- `JWT_ISSUER` - issuer токенов, по умолчанию `aura-auth`.
- `JWT_DEFAULT_USER_ROLE` - роль по умолчанию при регистрации, по умолчанию `ROLE_USER`.
- `CORS_ALLOWED_ORIGINS` - список разрешенных origins.
- `DB_URL` - JDBC URL базы данных.
- `DB_NAME` - имя базы данных в дефолтном JDBC URL.
- `DB_USER` - пользователь базы данных.
- `DB_PASS` - пароль базы данных.
- `SERVER_PORT` - HTTP-порт сервиса.

Основной runtime-конфиг расположен в [aura-auth-service/src/main/resources/application.properties].

## Подключение стартера в другие микросервисы

`aura-auth-starter` можно использовать как библиотеку для микросервисов, которым нужна JWT-проверка входящих запросов, типовые `ProblemDetail`-ответы на auth-ошибки, CORS-конфигурация и прокидывание `Authorization` заголовка в `OpenFeign`.

Starter регистрируется через Spring Boot auto-configuration, поднимает дефолтный `SecurityFilterChain` раньше стандартной servlet security-конфигурации и не допускает создания дефолтного `inMemoryUserDetailsManager` с generated password в JWT-only сервисах.

1. Добавить зависимость в `pom.xml` микросервиса:

```xml
<dependency>
    <groupId>mlakir</groupId>
    <artifactId>aura-auth-starter</artifactId>
    <version>${aura-auth.version}</version>
</dependency>
```

2. Указать обязательные настройки JWT-валидации:

```properties
aura.security.jwt.verification.public-key=${JWT_PUBLIC_KEY}
aura.security.jwt.verification.jwt-issuer=aura-auth
aura.security.cors.allowed-origins=http://localhost:3000
```

3. При необходимости переопределить список публичных endpoints. Если свойство не задано, starter сам поднимет дефолтный `SecurityFilterChain` и откроет:

- `/login`
- `/register`
- `/refresh`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/actuator/health`

Пример кастомизации:

```properties
aura.security.endpoints.permit-all=/public/**,/actuator/health,/swagger-ui/**,/v3/api-docs/**
```

Если дефолтной политики недостаточно, микросервис может объявить собственный бин `SecurityFilterChain`, и тогда автоконфигурация стартера не будет применена.

Для логина по username/password сервис по-прежнему должен сам определить `AuthenticationManager` и свой `AuthenticationProvider`, если ему нужна локальная форма аутентификации, а не только проверка входящих JWT.

4. Если сервис хранит активные JWT-сессии в своей БД или кэше, нужно определить собственный бин `JwtAccessTokenSessionValidator`. Если бин не объявлен, starter использует permissive-реализацию и считает токен активным.

Пример:

```java
@Component
public class DbJwtAccessTokenSessionValidator implements JwtAccessTokenSessionValidator {

    @Override
    public boolean isActive(UUID accessJti) {
        return true;
    }
}
```

Что дает starter после подключения:

- `JwtFilter` для извлечения и валидации Bearer JWT из входящего запроса
- `JwtClaimProvider` и `JwtParser` для доступа к claims токена
- `ProblemDetail`-ответы для `401` и `403`
- `CorsConfigurationSource`, собираемый из `aura.security.cors.*`
- `OpenAPI` security scheme для Bearer JWT
- `Feign` interceptor, автоматически пробрасывающий `Authorization` header в downstream-вызовы

## Ошибки

Проект использует `aura-exception-starter` для унифицированного ответа в формате `ProblemDetail`.

Пример auth-ошибки:

```json
{
  "type": "about:blank",
  "title": "Invalid credentials",
  "status": 401,
  "detail": "Неверный логин или пароль",
  "instance": "/login",
  "timestamp": "2026-03-25T10:04:40.703537Z"
}
```

Для неожиданных ошибок starter добавляет `errorId`.

## Локальный запуск

Нужно подготовить:

- Java 21
- Maven
- PostgreSQL
- JWT key pair

Пример запуска:

```bash
mvn clean verify
mvn -pl aura-auth-service spring-boot:run
```

Или через Docker Compose, если окружение проекта настроено под локальный контейнерный запуск:

```bash
docker compose up --build
```

## База данных

Liquibase changelog находится в `aura-auth-service/src/main/resources/db/changelog.yaml`.

Миграции создают:

- схему `aura_auth`
- таблицы пользователей
- таблицы ролей
- таблицу credentials
- таблицу token sessions

## Тесты

В проекте есть unit-тесты для auth service и обработки auth-ошибок.

Запуск тестов:

```bash
mvn test
```
