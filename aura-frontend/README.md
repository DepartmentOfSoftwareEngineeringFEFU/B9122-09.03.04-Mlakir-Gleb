# aura-frontend

Frontend дипломного проекта по теме: "РАЗРАБОТКА И РЕАЛИЗАЦИЯ ПРОТОТИПА ИНФОРМАЦИОННОЙ СИСТЕМЫ АВТОМАТИЧЕСКОГО СБОРА И ИНТЕЛЛЕКТУАЛЬНОГО АНАЛИЗА ТЕКСТОВЫХ ОТЗЫВОВ С ИСПОЛЬЗОВАНИЕМ МЕТОДОВ ОБРАБОТКИ ЕСТЕСТВЕННОГО ЯЗЫКА".

## Назначение

`aura-frontend` - web-интерфейс для работы с:

- авторизацией пользователей
- организациями
- источниками отзывов
- списком отзывов и фильтрами
- dashboard и агрегированной аналитикой
- AI summary по отзывам
- AI insights по организациям

## Технологии

- React 19
- TypeScript
- Vite
- React Router
- Axios
- Zustand
- TanStack Query
- React Hook Form + Zod
- Tailwind CSS

## Основные возможности

- логин и регистрация через `aura-auth-service`
- управление организациями и источниками
- ручной и scheduled сценарий работы с источниками
- импорт CSV для `MANUAL_IMPORT`
- запуск сбора для `TABITURIENT`, `OTZOVIK`, `VUZOPEDIA`
- список отзывов с фильтрами, сортировкой и пагинацией
- фильтрация по ключевым словам
- повторный анализ `FAILED_ANALYSIS`
- AI summary и AI insights

## Запуск в составе монорепозитория

Из корня проекта:

```bash
docker compose up --build -d
```

Приложение будет доступно на `http://localhost:5173`.

## Локальный запуск модуля

```bash
npm install
npm run dev
```

По умолчанию dev-сервер поднимается на `http://localhost:5173`.

## Mockup-режим

Для локальной демонстрации без backend доступен mockup-режим:

```bash
npm run dev:mockup
```

Демо-учётные данные:

- `demo-admin / demo123`
- `demo-user / demo123`

## Переменные окружения

Создайте `.env` на основе `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8081
VITE_AUTH_BASE_URL=http://localhost:8080
```

## Интеграция с backend

Frontend использует:

- `aura-auth-service`
  - `POST /auth/login`
  - `POST /auth/register`
  - `POST /auth/refresh`
  - `POST /auth/logout`
- `aura-core-service`
  - `GET /api/organizations`
  - `GET /api/sources`
  - `GET /api/reviews`
  - `GET /api/dashboard/*`
  - `POST /api/reviews/reanalyze`
  - `POST /api/reviews/{reviewId}/summary`
  - `POST /api/organizations/{organizationId}/insights`
  - `POST /api/collection/run/{sourceId}`

JWT access token хранится в `localStorage` и автоматически подставляется в `Authorization` header.

## Основные страницы

- `/login`
- `/register`
- `/dashboard`
- `/organizations`
- `/organizations/new`
- `/sources`
- `/sources/new`
- `/reviews`
- `/custom-sources`

## Проверки и сборка

```bash
npm run build
npm run lint
npm test
```
