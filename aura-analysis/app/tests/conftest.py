import csv
import json
from pathlib import Path

import joblib
import numpy as np
import pytest
from fastapi.testclient import TestClient
from sklearn.linear_model import LogisticRegression

from app.main import create_app
from app.domain.enums import Topic


@pytest.fixture(autouse=True)
def configure_env(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("ANALYSIS_MODE", "RULE_BASED")
    monkeypatch.setenv("MODEL_VERSION", "rule-based-0.1.0")
    monkeypatch.delenv("RUBERT_MODEL_NAME", raising=False)
    monkeypatch.delenv("RUBERT_SENTIMENT_CLASSIFIER_PATH", raising=False)
    monkeypatch.delenv("RUBERT_TOPIC_CLASSIFIER_PATH", raising=False)
    monkeypatch.delenv("RUBERT_METADATA_PATH", raising=False)
    monkeypatch.delenv("GOOGLE_AI_API_KEY", raising=False)
    monkeypatch.delenv("GOOGLE_AI_URL", raising=False)
    monkeypatch.delenv("GOOGLE_AI_MODEL", raising=False)


@pytest.fixture()
def client() -> TestClient:
    app = create_app()
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture()
def sample_dataset_path(tmp_path: Path) -> Path:
    dataset_path = tmp_path / "dataset.csv"
    rows = [
        ["Преподаватели отлично объясняют материал", "POSITIVE", "TEACHERS"],
        ["Преподаватели отвечают спокойно и понятно", "POSITIVE", "TEACHERS"],
        ["Учебная программа устарела и скучная", "NEGATIVE", "EDUCATION"],
        ["Лекции обычные и без ярких особенностей", "NEUTRAL", "EDUCATION"],
        ["В аудиториях холодно и тесно", "NEGATIVE", "INFRASTRUCTURE"],
        ["В библиотеке тихо и удобно заниматься", "POSITIVE", "INFRASTRUCTURE"],
        ["В общежитии грязно и шумно", "NEGATIVE", "DORMITORY"],
        ["В общежитии сделали нормальный ремонт", "POSITIVE", "DORMITORY"],
        ["Деканат долго оформляет документы", "NEGATIVE", "ADMINISTRATION"],
        ["Администрация ответила без эмоций", "NEUTRAL", "ADMINISTRATION"],
        ["Студенческие клубы активные и интересные", "POSITIVE", "STUDENT_LIFE"],
        ["Мероприятий мало, но они бывают", "NEUTRAL", "STUDENT_LIFE"],
        ["Обычный день в университете без особенностей", "NEUTRAL", "OTHER"],
        ["Ничего особенно хорошего или плохого не произошло", "NEUTRAL", "OTHER"],
    ]
    with dataset_path.open("w", encoding="utf-8", newline="") as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(["text", "sentiment", "topic"])
        writer.writerows(rows)
    return dataset_path


class FakeRubertEmbedder:
    topic_index = {
        Topic.EDUCATION.value: 0,
        Topic.TEACHERS.value: 1,
        Topic.INFRASTRUCTURE.value: 2,
        Topic.DORMITORY.value: 3,
        Topic.ADMINISTRATION.value: 4,
        Topic.STUDENT_LIFE.value: 5,
        Topic.OTHER.value: 6,
    }

    def __init__(self, model_name: str, tokenizer=None, model=None, device=None) -> None:
        self.model_name = model_name

    def embed_texts(self, texts: list[str], batch_size: int = 16) -> np.ndarray:
        embeddings = [self._embed_text(text) for text in texts]
        return np.asarray(embeddings, dtype=np.float32)

    def _embed_text(self, text: str) -> np.ndarray:
        text = text.lower()
        vector = np.zeros(10, dtype=np.float32)

        if any(marker in text for marker in ("хорош", "отлич", "интерес", "прекрас")):
            vector[0] = 5.0
        elif any(marker in text for marker in ("гряз", "шум", "плох", "долго", "слож")):
            vector[2] = 5.0
        else:
            vector[1] = 5.0

        if "преподав" in text:
            topic = Topic.TEACHERS.value
        elif any(marker in text for marker in ("лекц", "програм", "курс", "учеб")):
            topic = Topic.EDUCATION.value
        elif any(marker in text for marker in ("кампус", "аудитор", "библиотек", "корпус")):
            topic = Topic.INFRASTRUCTURE.value
        elif "общежит" in text:
            topic = Topic.DORMITORY.value
        elif any(marker in text for marker in ("деканат", "администрац", "документ", "расписан")):
            topic = Topic.ADMINISTRATION.value
        elif any(marker in text for marker in ("клуб", "мероприят", "студенческ", "актив")):
            topic = Topic.STUDENT_LIFE.value
        else:
            topic = Topic.OTHER.value

        vector[3 + self.topic_index[topic]] = 5.0
        return vector


@pytest.fixture()
def rubert_artifacts_dir(tmp_path: Path) -> Path:
    artifacts_dir = tmp_path / "rubert_artifacts"
    artifacts_dir.mkdir(parents=True, exist_ok=True)

    topic_labels = [
        Topic.EDUCATION.value,
        Topic.TEACHERS.value,
        Topic.INFRASTRUCTURE.value,
        Topic.DORMITORY.value,
        Topic.ADMINISTRATION.value,
        Topic.STUDENT_LIFE.value,
        Topic.OTHER.value,
    ]
    sentiment_labels = ["POSITIVE", "NEUTRAL", "NEGATIVE"]

    embeddings: list[np.ndarray] = []
    sentiment_targets: list[str] = []
    topic_targets: list[str] = []

    for sentiment_index, sentiment in enumerate(sentiment_labels):
        for topic_index, topic in enumerate(topic_labels):
            vector = np.zeros(10, dtype=np.float32)
            vector[sentiment_index] = 5.0
            vector[3 + topic_index] = 5.0
            embeddings.append(vector)
            sentiment_targets.append(sentiment)
            topic_targets.append(topic)

    features = np.asarray(embeddings, dtype=np.float32)
    sentiment_classifier = LogisticRegression(max_iter=2000).fit(features, sentiment_targets)
    topic_classifier = LogisticRegression(max_iter=2000).fit(features, topic_targets)

    joblib.dump(sentiment_classifier, artifacts_dir / "rubert_sentiment_classifier.joblib")
    joblib.dump(topic_classifier, artifacts_dir / "rubert_topic_classifier.joblib")
    (artifacts_dir / "rubert_metadata.json").write_text(
        json.dumps(
            {
                "mode": "RUBERT_EMBEDDINGS",
                "modelVersion": "rubert-embeddings-test",
                "rubertModelName": "cointegrated/rubert-tiny2",
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    return artifacts_dir
