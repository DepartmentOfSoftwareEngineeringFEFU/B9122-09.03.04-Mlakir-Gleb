# aura-exception

Spring Boot starter для унифицированной обработки HTTP-ошибок на базе `ProblemDetail`.

## Что входит

- `aura-exception-module`: базовый `AuraException` для бизнес-ошибок.
- `aura-exception-starter`: автоконфигурация Spring MVC с глобальным обработчиком исключений.

## Требования

- Java 21
- Spring Boot 3.3+
- Servlet-based Spring MVC application

## Подключение

```xml
<dependency>
    <groupId>mlakir</groupId>
    <artifactId>aura-exception-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Starter автоматически регистрирует:

- `AuraExceptionHandler`
- `MessageSource` для validation messages из `classpath:errors`

Автоконфигурация активируется только в servlet web application.

## Использование

Бизнес-ошибку можно вернуть через `AuraException`:

```java
@GetMapping("/orders/{id}")
public OrderDto getOrder(@PathVariable UUID id) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Order not found");
    detail.setTitle("Order missing");
    throw new AuraException(HttpStatus.NOT_FOUND, detail);
}
```

Для `IllegalArgumentException` starter вернёт `400 Bad Request` с унифицированным `ProblemDetail`.

Также starter покрывает:

- `MethodArgumentNotValidException`
- `HandlerMethodValidationException`
- `ConstraintViolationException`
- `MethodArgumentTypeMismatchException`
- `HttpMessageNotReadableException`
- `MissingRequestHeaderException`
- любой необработанный `Exception` через fallback `500`

## Формат ответа

Пример unexpected error:

```json
{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "Произошла неожиданная ошибка.",
  "instance": "/api/orders",
  "timestamp": "2026-03-24T10:15:30Z",
  "errorId": "8f0a1a8c-8a64-4d0a-b5a4-7f5a9b8b3d2d"
}
```

## Настройки

Все свойства находятся под префиксом `mlakir.aura.exception`.

```yaml
mlakir:
  aura:
    exception:
      default-title: Internal Server Error
      default-type: about:blank
      default-detail: Произошла неожиданная ошибка.
      default-status: 500
      include-exception-message: false
      validation-error-title: Validation error
      validation-error-detail: Ошибка в одном или нескольких полях.
      validation-error-type: about:blank
      validation-error-default-field-message: Введите корректные данные
      constraint-violation-detail: Ошибка в одном или нескольких параметрах.
      malformed-request-title: Malformed request
      malformed-request-detail: Тело запроса имеет неверный формат.
      type-mismatch-title: Invalid parameter
      type-mismatch-detail: Параметр запроса имеет неверный формат или тип.
      illegal-argument-title: Bad Request
      illegal-argument-detail: Запрос некорректен, поскольку выбранные параметры указаны неверно или произошла функциональная ошибка.
      missing-token-title: Missing token
      missing-token-detail: Пользователь не аутентифицирован.
      missing-request-header-title: Missing request header
```

## Расширение

При необходимости можно переопределить стандартный `AuraProblemDetailFactory` своим bean-ом.

## Сборка

```bash
mvn clean verify
```
