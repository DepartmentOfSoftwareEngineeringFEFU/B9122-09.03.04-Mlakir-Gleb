import httpx


def _build_reviews(count: int) -> list[dict[str, str]]:
    return [
        {
            "text": f"Отзыв номер {index}. В общежитии бывают проблемы, но инфраструктура местами удобная.",
            "sentiment": "NEGATIVE" if index % 2 else "POSITIVE",
            "topic": "DORMITORY" if index % 2 else "INFRASTRUCTURE",
        }
        for index in range(1, count + 1)
    ]


def test_insights_returns_fallback_without_api_key(client) -> None:
    response = client.post(
        "/insights",
        json={
            "organizationName": "ДВФУ",
            "reviews": [
                {
                    "text": "В общежитии грязно и шумно.",
                    "sentiment": "NEGATIVE",
                    "topic": "DORMITORY",
                },
                {
                    "text": "Библиотека удобная и хорошо оборудована.",
                    "sentiment": "POSITIVE",
                    "topic": "INFRASTRUCTURE",
                },
            ],
        },
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["modelVersion"] == "fallback-insights-0.1.0"
    assert payload["summary"] == "Отчёт сформирован на основе локальной статистики отзывов."
    assert payload["strengths"]
    assert payload["weaknesses"]
    assert payload["recommendations"]


def test_insights_gemini_json_success(monkeypatch) -> None:
    def fake_post(self, url, params=None, json=None):
        return httpx.Response(
            200,
            json={
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": (
                                        '{"summary":"Общий фон смешанный.","strengths":["Сильные преподаватели","Удобная инфраструктура"],'
                                        '"weaknesses":["Проблемы с общежитием"],"recommendations":["Улучшить условия проживания"]}'
                                    )
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
            "/insights",
            json={
                "organizationName": "ДВФУ",
                "reviews": [
                    {
                        "text": "Преподаватели хорошие, но в общежитии шумно.",
                        "sentiment": "NEGATIVE",
                        "topic": "DORMITORY",
                    }
                ],
            },
        )

    assert response.status_code == 200
    assert response.json() == {
        "summary": "Общий фон смешанный.",
        "strengths": ["Сильные преподаватели", "Удобная инфраструктура"],
        "weaknesses": ["Проблемы с общежитием"],
        "recommendations": ["Улучшить условия проживания"],
        "modelVersion": "gemini-2.5-flash-lite",
    }


def test_insights_gemini_markdown_json_success(monkeypatch) -> None:
    def fake_post(self, url, params=None, json=None):
        return httpx.Response(
            200,
            json={
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": (
                                        "```json\n"
                                        '{"summary":"Есть как сильные стороны, так и проблемы.","strengths":["Активная студенческая жизнь"],'
                                        '"weaknesses":["Административные задержки"],"recommendations":["Упростить внутренние процессы"]}\n'
                                        "```"
                                    )
                                }
                            ]
                        }
                    }
                ]
            },
        )

    monkeypatch.setenv("GOOGLE_AI_API_KEY", "test-key")
    monkeypatch.setattr("app.services.gemini_client._HttpxClient.post", fake_post)

    from fastapi.testclient import TestClient

    from app.main import create_app

    app = create_app()
    with TestClient(app) as test_client:
        response = test_client.post(
            "/insights",
            json={
                "organizationName": "ДВФУ",
                "reviews": [
                    {
                        "text": "В деканате долго отвечают, но студенческие мероприятия интересные.",
                        "sentiment": "NEGATIVE",
                        "topic": "ADMINISTRATION",
                    }
                ],
            },
        )

    assert response.status_code == 200
    assert response.json()["summary"] == "Есть как сильные стороны, так и проблемы."


def test_insights_invalid_gemini_response_falls_back(monkeypatch) -> None:
    def fake_post(self, url, params=None, json=None):
        return httpx.Response(
            200,
            json={"candidates": [{"content": {"parts": [{"text": "not-json"}]}}]},
        )

    monkeypatch.setenv("GOOGLE_AI_API_KEY", "test-key")
    monkeypatch.setattr("app.services.gemini_client._HttpxClient.post", fake_post)

    from fastapi.testclient import TestClient

    from app.main import create_app

    app = create_app()
    with TestClient(app) as test_client:
        response = test_client.post(
            "/insights",
            json={
                "organizationName": "ДВФУ",
                "reviews": [
                    {
                        "text": "В общежитии грязно.",
                        "sentiment": "NEGATIVE",
                        "topic": "DORMITORY",
                    }
                ],
            },
        )

    assert response.status_code == 200
    assert response.json()["modelVersion"] == "fallback-insights-0.1.0"


def test_insights_truncates_prompt_before_sending(monkeypatch) -> None:
    captured_request = {}

    def fake_post(self, url, params=None, json=None):
        captured_request["prompt"] = json["contents"][0]["parts"][0]["text"]
        return httpx.Response(
            200,
            json={
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": '{"summary":"Сводка.","strengths":["Сильная сторона"],"weaknesses":["Слабая сторона"],"recommendations":["Рекомендация"]}'
                                }
                            ]
                        }
                    }
                ]
            },
        )

    monkeypatch.setenv("GOOGLE_AI_API_KEY", "test-key")
    monkeypatch.setenv("INSIGHTS_MAX_INPUT_CHARS", "500")
    monkeypatch.setattr("app.services.gemini_client._HttpxClient.post", fake_post)

    from fastapi.testclient import TestClient

    from app.main import create_app

    app = create_app()
    with TestClient(app) as test_client:
        response = test_client.post(
            "/insights",
            json={
                "organizationName": "ДВФУ",
                "reviews": [
                    {
                        "text": f"Маркер {index}. " + ("Очень длинный отзыв. " * 20),
                        "sentiment": "NEGATIVE",
                        "topic": "DORMITORY",
                    }
                    for index in range(1, 6)
                ],
            },
        )

    assert response.status_code == 200
    assert len(captured_request["prompt"]) <= 500
    assert "Маркер 1." in captured_request["prompt"]
    assert "Маркер 5." not in captured_request["prompt"]


def test_insights_validation_error_for_more_than_100_reviews(client) -> None:
    response = client.post(
        "/insights",
        json={
            "organizationName": "ДВФУ",
            "reviews": _build_reviews(101),
        },
    )

    assert response.status_code == 422
    assert response.json()["errorCode"] == "VALIDATION_ERROR"


def test_old_endpoints_still_work_after_insights_addition(client) -> None:
    summarize_response = client.post("/summarize", json={"text": "Преподаватели хорошие, но расписание неудобное."})
    analyze_response = client.post("/analyze", json={"text": "Очень хорошие преподаватели и интересные лекции"})

    assert summarize_response.status_code == 200
    assert analyze_response.status_code == 200
