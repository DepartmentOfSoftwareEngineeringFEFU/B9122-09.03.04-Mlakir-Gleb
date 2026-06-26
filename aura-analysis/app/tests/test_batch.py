from fastapi.testclient import TestClient

from app.main import create_app
from app.tests.conftest import FakeRubertEmbedder


def test_batch_analyze_endpoint_preserves_order(client) -> None:
    response = client.post(
        "/analyze/batch",
        json={
            "items": [
                {"text": "Очень хорошие преподаватели и прекрасные лекции"},
                {"text": "В общежитии грязно и неудобно"},
            ]
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert len(payload["items"]) == 2
    assert payload["items"][0]["sentiment"] == "POSITIVE"
    assert payload["items"][1]["sentiment"] == "NEGATIVE"
    assert payload["items"][1]["topic"] == "DORMITORY"


def test_batch_validation_error_for_empty_text(client) -> None:
    response = client.post("/analyze/batch", json={"items": [{"text": ""}]})

    assert response.status_code == 422
    assert response.json()["errorCode"] == "VALIDATION_ERROR"


def test_rubert_batch_analyze_endpoint_preserves_order(monkeypatch, rubert_artifacts_dir) -> None:
    monkeypatch.setenv("ANALYSIS_MODE", "RUBERT_EMBEDDINGS")
    monkeypatch.setenv("RUBERT_MODEL_NAME", "cointegrated/rubert-tiny2")
    monkeypatch.setenv(
        "RUBERT_SENTIMENT_CLASSIFIER_PATH",
        str(rubert_artifacts_dir / "rubert_sentiment_classifier.joblib"),
    )
    monkeypatch.setenv(
        "RUBERT_TOPIC_CLASSIFIER_PATH",
        str(rubert_artifacts_dir / "rubert_topic_classifier.joblib"),
    )
    monkeypatch.setenv("RUBERT_METADATA_PATH", str(rubert_artifacts_dir / "rubert_metadata.json"))
    monkeypatch.setattr(
        "app.services.analyzers.rubert_embeddings_analyzer.RubertEmbedder",
        FakeRubertEmbedder,
    )

    app = create_app()
    with TestClient(app) as client:
        response = client.post(
            "/analyze/batch",
            json={
                "items": [
                    {"text": "Очень хорошие преподаватели и прекрасные лекции"},
                    {"text": "В общежитии грязно и шумно"},
                ]
            },
        )

    assert response.status_code == 200
    payload = response.json()
    assert len(payload["items"]) == 2
    assert payload["items"][0]["sentiment"] == "POSITIVE"
    assert payload["items"][0]["topic"] == "TEACHERS"
    assert payload["items"][1]["sentiment"] == "NEGATIVE"
    assert payload["items"][1]["topic"] == "DORMITORY"
