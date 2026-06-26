# aura-exception

Общий Spring Boot starter дипломного проекта по теме: "РАЗРАБОТКА И РЕАЛИЗАЦИЯ ПРОТОТИПА ИНФОРМАЦИОННОЙ СИСТЕМЫ АВТОМАТИЧЕСКОГО СБОРА И ИНТЕЛЛЕКТУАЛЬНОГО АНАЛИЗА ТЕКСТОВЫХ ОТЗЫВОВ С ИСПОЛЬЗОВАНИЕМ МЕТОДОВ ОБРАБОТКИ ЕСТЕСТВЕННОГО ЯЗЫКА".

## Назначение

`aura-exception` предоставляет общий механизм унифицированной обработки HTTP-ошибок на базе `ProblemDetail` для Spring-сервисов монорепозитория.

## Что входит

- `aura-exception-module` - базовый `AuraException` для бизнес-ошибок
- `aura-exception-starter` - Spring Boot autoconfiguration с глобальным обработчиком исключений

## Технологии

- Java 21
- Spring Boot 3.3+
- Spring MVC

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

## Что покрывает starter

- `AuraException`
- `IllegalArgumentException`
- `MethodArgumentNotValidException`
- `HandlerMethodValidationException`
- `ConstraintViolationException`
- `MethodArgumentTypeMismatchException`
- `HttpMessageNotReadableException`
- `MissingRequestHeaderException`
- fallback `500` для необработанных исключений

## Пример ответа

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

Пример:

```yaml
mlakir:
  aura:
    exception:
      default-title: Internal Server Error
      default-detail: Произошла неожиданная ошибка.
      validation-error-title: Validation error
      malformed-request-title: Malformed request
```

## Сборка

```bash
mvn clean verify
```
