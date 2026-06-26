def test_health_endpoint(client) -> None:
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "mode": "RULE_BASED",
        "requestedMode": "RULE_BASED",
        "degraded": False,
        "degradationReason": None,
        "modelVersion": "rule-based-0.1.0",
    }


def test_info_endpoint(client) -> None:
    response = client.get("/info")

    assert response.status_code == 200
    payload = response.json()
    assert payload["mode"] == "RULE_BASED"
    assert payload["requestedMode"] == "RULE_BASED"
    assert payload["degraded"] is False
    assert payload["degradationReason"] is None
    assert payload["modelVersion"] == "rule-based-0.1.0"
    assert payload["supportedSentiments"] == ["POSITIVE", "NEUTRAL", "NEGATIVE"]
    assert payload["supportedTopics"] == [
        "EDUCATION",
        "TEACHERS",
        "INFRASTRUCTURE",
        "DORMITORY",
        "ADMINISTRATION",
        "STUDENT_LIFE",
        "OTHER",
    ]
