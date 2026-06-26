# aura-analysis

NLP/AI-сервис дипломного проекта по теме: "РАЗРАБОТКА И РЕАЛИЗАЦИЯ ПРОТОТИПА ИНФОРМАЦИОННОЙ СИСТЕМЫ АВТОМАТИЧЕСКОГО СБОРА И ИНТЕЛЛЕКТУАЛЬНОГО АНАЛИЗА ТЕКСТОВЫХ ОТЗЫВОВ С ИСПОЛЬЗОВАНИЕМ МЕТОДОВ ОБРАБОТКИ ЕСТЕСТВЕННОГО ЯЗЫКА".

## Назначение

`aura-analysis` - stateless `FastAPI`-микросервис, который:

- анализирует тональность и тему отзывов
- извлекает ключевые слова
- строит краткие summary отзывов
- генерирует AI insights по отзывам организации

## API

- `GET /health`
- `GET /info`
- `POST /analyze`
- `POST /analyze/batch`
- `POST /summarize`
- `POST /insights`

Контракт совместим с `aura-core-service`.

## Режимы анализа

`ANALYSIS_MODE` поддерживает:

- `RULE_BASED`
- `RUBERT_EMBEDDINGS`

По умолчанию используется `RUBERT_EMBEDDINGS`.

## Технологии

- Python 3.12
- FastAPI
- Pydantic
- scikit-learn
- PyTorch
- transformers

## Запуск в составе монорепозитория

Из корня проекта:

```bash
docker compose up --build -d
```

Сервис будет доступен на `http://localhost:8090`.

## Локальный запуск модуля

```bash
python3.12 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8090
```

## Основные переменные окружения

- `APP_NAME`
- `APP_PORT`
- `ANALYSIS_MODE`
- `MODEL_VERSION`
- `RUBERT_MODEL_NAME`
- `RUBERT_SENTIMENT_CLASSIFIER_PATH`
- `RUBERT_TOPIC_CLASSIFIER_PATH`
- `RUBERT_METADATA_PATH`
- `GOOGLE_AI_API_KEY`
- `GOOGLE_AI_MODEL`
- `GOOGLE_AI_URL`
- `SUMMARY_MAX_INPUT_LENGTH`
- `SUMMARY_MAX_OUTPUT_CHARS`
- `INSIGHTS_MAX_INPUT_CHARS`

## ML-артефакты

Для режима `RUBERT_EMBEDDINGS` требуются:

- `app/ml/artifacts/rubert_sentiment_classifier.joblib`
- `app/ml/artifacts/rubert_topic_classifier.joblib`
- `app/ml/artifacts/rubert_metadata.json`

Контейнер не скачивает модели при старте; артефакты должны уже лежать в проекте.

## Обучение

```bash
python -m app.ml.training.train_rubert \
  --dataset data/dataset.csv \
  --output app/ml/artifacts \
  --model-name cointegrated/rubert-tiny2
```

Пример формата датасета:

```csv
text,sentiment,topic
"Преподаватели хорошо объясняют материал",POSITIVE,TEACHERS
"В общежитии грязно и шумно",NEGATIVE,DORMITORY
```

## Особенности summary и insights

- `summarize` и `insights` могут использовать Gemini
- при недоступности внешней модели сервис не падает и использует fallback, где это предусмотрено
- `aura-core-service` вызывает этот сервис по HTTP и сам хранит результаты в своей БД
