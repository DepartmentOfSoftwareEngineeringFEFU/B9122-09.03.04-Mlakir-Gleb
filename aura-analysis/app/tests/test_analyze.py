from fastapi.testclient import TestClient

from app.main import create_app
from app.tests.conftest import FakeRubertEmbedder


def test_analyze_contract_rule_based(client) -> None:
    response = client.post("/analyze", json={"text": "Очень хорошие преподаватели, интересные лекции и классный курс"})

    assert response.status_code == 200
    payload = response.json()
    assert payload["sentiment"] == "POSITIVE"
    assert payload["topic"] in {"TEACHERS", "EDUCATION"}
    assert payload["modelVersion"] == "rule-based-0.1.0"
    assert 1 <= len(payload["keywords"]) <= 5
    assert 0.0 <= payload["confidence"] <= 1.0


def test_analyze_empty_text_validation(client) -> None:
    response = client.post("/analyze", json={"text": "   "})

    assert response.status_code == 422
    payload = response.json()
    assert payload["errorCode"] == "VALIDATION_ERROR"


def test_rubert_mode_uses_artifacts(monkeypatch, rubert_artifacts_dir) -> None:
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
        health_response = client.get("/health")
        analyze_response = client.post(
            "/analyze",
            json={"text": "Очень хорошие преподаватели, интересные лекции и сильный курс"},
        )

    assert health_response.status_code == 200
    assert health_response.json() == {
        "status": "ok",
        "mode": "RUBERT_EMBEDDINGS",
        "requestedMode": "RUBERT_EMBEDDINGS",
        "degraded": False,
        "degradationReason": None,
        "modelVersion": "rubert-embeddings-test",
    }

    assert analyze_response.status_code == 200
    payload = analyze_response.json()
    assert payload["sentiment"] == "POSITIVE"
    assert payload["topic"] == "TEACHERS"
    assert payload["modelVersion"] == "rubert-embeddings-test"
    assert 1 <= len(payload["keywords"]) <= 5
    assert 0.0 <= payload["confidence"] <= 1.0


def test_rubert_mode_falls_back_to_rule_based_when_artifacts_missing(monkeypatch) -> None:
    monkeypatch.setenv("ANALYSIS_MODE", "RUBERT_EMBEDDINGS")
    monkeypatch.setenv("RUBERT_MODEL_NAME", "cointegrated/rubert-tiny2")
    monkeypatch.setenv("RUBERT_SENTIMENT_CLASSIFIER_PATH", "missing/rubert_sentiment_classifier.joblib")
    monkeypatch.setenv("RUBERT_TOPIC_CLASSIFIER_PATH", "missing/rubert_topic_classifier.joblib")
    monkeypatch.setenv("RUBERT_METADATA_PATH", "missing/rubert_metadata.json")

    app = create_app()
    with TestClient(app) as client:
        health_response = client.get("/health")
        analyze_response = client.post("/analyze", json={"text": "В общежитии грязно"})

    assert health_response.status_code == 200
    assert health_response.json() == {
        "status": "degraded",
        "mode": "RULE_BASED",
        "requestedMode": "RUBERT_EMBEDDINGS",
        "degraded": True,
        "degradationReason": "Requested RUBERT_EMBEDDINGS mode is unavailable; rule-based fallback activated (ModelNotFoundError).",
        "modelVersion": "rule-based-0.1.0",
    }
    assert analyze_response.status_code == 200
    assert analyze_response.json()["modelVersion"] == "rule-based-0.1.0"
