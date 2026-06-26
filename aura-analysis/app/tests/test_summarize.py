import httpx


def test_summarize_uses_fallback_without_api_key(client) -> None:
    response = client.post(
        "/summarize",
        json={"text": "Общежитие грязное. На кухне всегда очередь. До корпуса идти далеко."},
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["modelVersion"] == "fallback-extractive-0.2.0"
    assert payload["summary"]
    assert len(payload["summary"]) <= 700


def test_summarize_gemini_success(monkeypatch) -> None:
    captured_request = {}

    def fake_post(self, url, params=None, json=None):
        captured_request["url"] = url
        captured_request["params"] = params
        captured_request["json"] = json
        return httpx.Response(
            200,
            json={
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": '```"Хорошие преподаватели и сильная программа, но есть проблемы с расписанием."```'
                                }
                            ]
                        }
                    }
                ]
            },
        )

    monkeypatch.setenv("GOOGLE_AI_API_KEY", "test-key")
    monkeypatch.setenv("GOOGLE_AI_MODEL", "gemini-2.5-flash-lite")
    monkeypatch.setattr("app.services.gemini_client._HttpxClient.post", fake_post)

    from fastapi.testclient import TestClient

    from app.main import create_app

    app = create_app()
    with TestClient(app) as test_client:
        response = test_client.post(
            "/summarize",
            json={"text": "Преподаватели хорошие, программа сильная, но расписание часто меняется."},
        )

    assert response.status_code == 200
    assert response.json() == {
        "summary": "Хорошие преподаватели и сильная программа, но есть проблемы с расписанием.",
        "modelVersion": "gemini-2.5-flash-lite",
    }
    assert captured_request["url"] == "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"
    assert captured_request["params"] == {"key": "test-key"}
    prompt = captured_request["json"]["contents"][0]["parts"][0]["text"]
    assert 'Текст отзыва:\n"""' in prompt
    assert "избегай общих фраз" in prompt


def test_summarize_gemini_error_falls_back(monkeypatch) -> None:
    def fake_post(self, url, params=None, json=None):
        return httpx.Response(429, json={"error": {"message": "rate limit"}})

    monkeypatch.setenv("GOOGLE_AI_API_KEY", "test-key")
    monkeypatch.setattr("app.services.gemini_client._HttpxClient.post", fake_post)

    from fastapi.testclient import TestClient

    from app.main import create_app

    app = create_app()
    with TestClient(app) as test_client:
        response = test_client.post(
            "/summarize",
            json={"text": "В общежитии шумно. Заселение затянулось. Сотрудники отвечают медленно."},
        )

    assert response.status_code == 200
    payload = response.json()
    assert payload["modelVersion"] == "fallback-extractive-0.2.0"
    assert payload["summary"]


def test_summarize_truncates_long_text_before_sending(monkeypatch) -> None:
    captured_request = {}

    def fake_post(self, url, params=None, json=None):
        captured_request["prompt"] = json["contents"][0]["parts"][0]["text"]
        return httpx.Response(
            200,
            json={"candidates": [{"content": {"parts": [{"text": "Краткий и точный конспект."}]}}]},
        )

    monkeypatch.setenv("GOOGLE_AI_API_KEY", "test-key")
    monkeypatch.setenv("SUMMARY_MAX_INPUT_LENGTH", "120")
    monkeypatch.setattr("app.services.gemini_client._HttpxClient.post", fake_post)

    from fastapi.testclient import TestClient

    from app.main import create_app

    app = create_app()
    with TestClient(app) as test_client:
        response = test_client.post("/summarize", json={"text": "Очень длинный отзыв. " * 300})

    assert response.status_code == 200
    review_text = captured_request["prompt"].split('"""', maxsplit=2)[1]
    assert len(review_text.strip()) <= 120


def test_summarize_empty_gemini_response_falls_back(monkeypatch) -> None:
    def fake_post(self, url, params=None, json=None):
        return httpx.Response(200, json={"candidates": []})

    monkeypatch.setenv("GOOGLE_AI_API_KEY", "test-key")
    monkeypatch.setattr("app.services.gemini_client._HttpxClient.post", fake_post)

    from fastapi.testclient import TestClient

    from app.main import create_app

    app = create_app()
    with TestClient(app) as test_client:
        response = test_client.post("/summarize", json={"text": "Программа хорошая. Но заселение прошло тяжело."})

    assert response.status_code == 200
    assert response.json()["modelVersion"] == "fallback-extractive-0.2.0"


def test_summarize_validation_error_for_empty_text(client) -> None:
    response = client.post("/summarize", json={"text": "   "})

    assert response.status_code == 422
    assert response.json()["errorCode"] == "VALIDATION_ERROR"


def test_analyze_endpoints_still_work_after_summary_addition(client) -> None:
    single_response = client.post("/analyze", json={"text": "Очень хорошие преподаватели и интересные лекции"})
    batch_response = client.post(
        "/analyze/batch",
        json={"items": [{"text": "В общежитии грязно и шумно"}]},
    )

    assert single_response.status_code == 200
    assert batch_response.status_code == 200
    assert batch_response.json()["items"]
