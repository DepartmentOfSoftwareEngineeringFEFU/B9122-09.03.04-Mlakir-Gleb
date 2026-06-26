# aura-analysis-service

`aura-analysis-service` - stateless FastAPI-микросервис для анализа текстов отзывов об университете. Он сохраняет существующий HTTP-контракт и может работать в режимах `RULE_BASED` и `RUBERT_EMBEDDINGS`.

## API

- `GET /health`
- `GET /info`
- `POST /analyze`
- `POST /analyze/batch`
- `POST /summarize`
- `POST /insights`

Контракт совместим с `aura-core-service`:

```json
{
  "sentiment": "POSITIVE",
  "topic": "EDUCATION",
  "keywords": ["string"],
  "confidence": 0.85,
  "modelVersion": "string"
}
```

## Analysis Modes

`ANALYSIS_MODE` поддерживает:

- `RULE_BASED`
- `RUBERT_EMBEDDINGS`

Поведение:

- `RULE_BASED`: используется встроенный rule-based analyzer.
- `RUBERT_EMBEDDINGS`: используются embeddings из `cointegrated/rubert-tiny2` и два `LogisticRegression` классификатора для `sentiment` и `topic`.
- Если `RUBERT_EMBEDDINGS` запрошен, но RuBERT-модель или артефакты не загрузились, сервис пишет warning и автоматически переключается на `RULE_BASED`.
- `/health` и `/info` всегда показывают фактический активный режим.

По умолчанию проект стартует в `RUBERT_EMBEDDINGS`.

## Структура

```text
app/
  api/
  core/
  domain/
  ml/
    artifacts/
    training/
  services/
    analyzers/
```

## Environment

```env
APP_NAME=aura-analysis-service
APP_PORT=8090
ANALYSIS_MODE=RUBERT_EMBEDDINGS
MODEL_VERSION=rule-based-0.1.0
RUBERT_MODEL_NAME=cointegrated/rubert-tiny2
RUBERT_SENTIMENT_CLASSIFIER_PATH=app/ml/artifacts/rubert_sentiment_classifier.joblib
RUBERT_TOPIC_CLASSIFIER_PATH=app/ml/artifacts/rubert_topic_classifier.joblib
RUBERT_METADATA_PATH=app/ml/artifacts/rubert_metadata.json
LOG_LEVEL=INFO
GOOGLE_AI_API_KEY=
GOOGLE_AI_MODEL=gemini-2.5-flash-lite
GOOGLE_AI_URL=https://generativelanguage.googleapis.com/v1beta/models
SUMMARY_MAX_INPUT_LENGTH=20000
SUMMARY_MAX_OUTPUT_CHARS=700
```

`MODEL_VERSION` используется только в `RULE_BASED`. В `RUBERT_EMBEDDINGS` фактический `modelVersion` читается из `rubert_metadata.json`.

Для `RUBERT_EMBEDDINGS` нужны артефакты:

- `rubert_sentiment_classifier.joblib`
- `rubert_topic_classifier.joblib`
- `rubert_metadata.json`

## Local Run

```bash
python3.12 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8090
```

## Docker

```bash
docker compose up --build
```

Контейнер не скачивает модели при старте. Все артефакты должны уже лежать в проекте или быть примонтированы заранее.

## Dataset Format

CSV:

```csv
text,sentiment,topic
"Преподаватели хорошо объясняют материал",POSITIVE,TEACHERS
"В общежитии грязно и шумно",NEGATIVE,DORMITORY
"Учебная программа устарела",NEGATIVE,EDUCATION
"Много мероприятий и активная студенческая жизнь",POSITIVE,STUDENT_LIFE
```

Правила:

- `text` не пустой
- `sentiment` должен входить в `POSITIVE | NEUTRAL | NEGATIVE`
- `topic` должен входить в поддерживаемые enum
- дубликаты удаляются
- слишком короткие тексты отбрасываются

Пример датасета лежит в [app/ml/training/dataset.example.csv](/Users/glebmlakir/IdeaProjects/aura-analysis/app/ml/training/dataset.example.csv:1).

## RUBERT_EMBEDDINGS Mode

Зависимости:

```bash
pip install -r requirements.txt
```

Обучение:

```bash
python -m app.ml.training.train_rubert \
  --dataset data/dataset.csv \
  --output app/ml/artifacts \
  --model-name cointegrated/rubert-tiny2
```

Скрипт:

- загружает CSV `text,sentiment,topic`
- нормализует текст и валидирует enum values
- строит batched RuBERT embeddings
- обучает два `LogisticRegression` классификатора
- пишет артефакты `rubert_sentiment_classifier.joblib`, `rubert_topic_classifier.joblib`, `rubert_metadata.json`
- сохраняет метрики в `reports/rubert_sentiment_report.json` и `reports/rubert_topic_report.json`

Запуск сервиса:

```bash
export ANALYSIS_MODE=RUBERT_EMBEDDINGS
export RUBERT_MODEL_NAME=cointegrated/rubert-tiny2
uvicorn app.main:app --host 0.0.0.0 --port 8090
```

Замечание:

- Gemini используется только для `POST /summarize`
- `POST /analyze` и `POST /analyze/batch` в режиме `RUBERT_EMBEDDINGS` работают локально

## Evaluation

Метрики для `RUBERT_EMBEDDINGS` сохраняются training-скриптом в `reports/rubert_sentiment_report.json` и `reports/rubert_topic_report.json`.

## Preprocessing And Keywords

- Текст нормализуется через `lower`, `trim`, удаление HTML entities, управляющих символов и лишних пробелов.
- В `RUBERT_EMBEDDINGS` ключевые слова извлекаются локально frequency-based extractor'ом с фильтрацией стоп-слов.
- Если ключевые слова извлечь не удалось, используется fallback `["отзыв"]`.

## Summary

Для краткого конспекта отзыва доступен endpoint `POST /summarize`.

Request:

```json
{
  "text": "string"
}
```

Validation:

- `text` обязателен
- min length: `1`
- max length: `20000`

Response:

```json
{
  "summary": "string",
  "modelVersion": "gemini-2.5-flash-lite"
}
```

Пример:

```bash
curl -X POST http://localhost:8090/summarize \
  -H "Content-Type: application/json" \
  -d '{"text":"Преподаватели хорошо объясняют материал, но расписание часто меняется и это мешает учебе."}'
```

Как включить summary:

- получить API key в Google AI Studio: https://aistudio.google.com/app/apikey
- задать `GOOGLE_AI_API_KEY`
- при необходимости переопределить `GOOGLE_AI_MODEL` и `GOOGLE_AI_URL`
- при необходимости настроить `SUMMARY_MAX_INPUT_LENGTH` и `SUMMARY_MAX_OUTPUT_CHARS`

Fallback:

- если `GOOGLE_AI_API_KEY` отсутствует, Gemini недоступен, возвращает `4xx`, `5xx`, пустой `candidates` или ответ нельзя распарсить, сервис не падает;
- в этих случаях строится локальный extractive summary из первых 2-3 содержательных предложений;
- fallback response возвращает `modelVersion = "fallback-extractive-0.2.0"`;
- причина fallback логируется без полного текста отзыва и без API key.

Gemini request:

```text
POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key=API_KEY
```

## Insights

Для AI-отчёта по пачке отзывов доступен endpoint `POST /insights`.

Request:

```json
{
  "organizationName": "ДВФУ",
  "reviews": [
    {
      "text": "В общежитии грязно и шумно.",
      "sentiment": "NEGATIVE",
      "topic": "DORMITORY"
    },
    {
      "text": "Библиотека удобная и хорошо оборудована.",
      "sentiment": "POSITIVE",
      "topic": "INFRASTRUCTURE"
    }
  ]
}
```

Validation:

- `organizationName` обязателен
- `reviews` от `1` до `100`
- `text` обязателен, max length `1500`
- используются те же enum values для `sentiment` и `topic`, что и в `/analyze`

Response:

```json
{
  "summary": "string",
  "strengths": ["string"],
  "weaknesses": ["string"],
  "recommendations": ["string"],
  "modelVersion": "gemini-2.5-flash-lite"
}
```

Fallback:

- если `GOOGLE_AI_API_KEY` отсутствует, Gemini недоступен или ответ нельзя распарсить как JSON;
- сервис не падает и строит локальный отчёт на основе sentiment/topic статистики;
- fallback response возвращает `modelVersion = "fallback-insights-0.1.0"`.

## Health And Info

`GET /health`:

```json
{
  "status": "ok",
  "mode": "RUBERT_EMBEDDINGS",
  "modelVersion": "rubert-embeddings-0.1.0"
}
```

При fallback:

```json
{
  "status": "ok",
  "mode": "RULE_BASED",
  "modelVersion": "rule-based-0.1.0"
}
```

`GET /info`:

```json
{
  "mode": "RUBERT_EMBEDDINGS",
  "modelVersion": "rubert-embeddings-0.1.0",
  "supportedSentiments": ["POSITIVE", "NEUTRAL", "NEGATIVE"],
  "supportedTopics": [
    "EDUCATION",
    "TEACHERS",
    "INFRASTRUCTURE",
    "DORMITORY",
    "ADMINISTRATION",
    "STUDENT_LIFE",
    "OTHER"
  ]
}
```

## Tests

```bash
pytest
```

Покрыто:

- HTTP-контракт `/analyze` и `/analyze/batch`
- порядок элементов в batch
- активный режим в `/health`
- supported enums в `/info`
- fallback на `RULE_BASED`
- загрузка ML analyzer из артефактов
- диапазон `confidence`
- генерация `keywords`
- создание артефактов training script
- `POST /summarize` через Gemini и fallback-сценарии
- загрузка `RUBERT_EMBEDDINGS`, fallback при отсутствии артефактов и создание RuBERT artifacts
- `POST /insights` через Gemini JSON и локальный fallback
