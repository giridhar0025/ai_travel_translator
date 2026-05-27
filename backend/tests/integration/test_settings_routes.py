from collections.abc import Iterator

import pytest
from fastapi.testclient import TestClient


@pytest.fixture
def headers(client: TestClient) -> Iterator[dict[str, str]]:
    body = client.post("/api/v1/auth/guest").json()
    h = {"Authorization": f"Bearer {body['token']}"}
    try:
        yield h
    finally:
        client.delete("/api/v1/me", headers=h)


def test_get_settings_requires_auth(client: TestClient) -> None:
    assert client.get("/api/v1/settings").status_code == 401


def test_get_settings_returns_defaults_for_new_user(
    client: TestClient, headers: dict[str, str]
) -> None:
    r = client.get("/api/v1/settings", headers=headers)
    assert r.status_code == 200, r.text
    body = r.json()
    assert body == {
        "default_language": "en",
        "playback_speed": "normal",
        "offline_enabled": False,
    }


def test_patch_settings_partial_update_persists(
    client: TestClient, headers: dict[str, str]
) -> None:
    r = client.patch("/api/v1/settings", headers=headers, json={"default_language": "hi"})
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["default_language"] == "hi"
    # untouched
    assert body["playback_speed"] == "normal"
    assert body["offline_enabled"] is False

    # GET reflects the change
    body2 = client.get("/api/v1/settings", headers=headers).json()
    assert body2["default_language"] == "hi"


def test_patch_settings_invalid_playback_speed_422(
    client: TestClient, headers: dict[str, str]
) -> None:
    r = client.patch("/api/v1/settings", headers=headers, json={"playback_speed": "blazing"})
    assert r.status_code == 422


def test_patch_settings_unsupported_language_400(
    client: TestClient, headers: dict[str, str]
) -> None:
    r = client.patch("/api/v1/settings", headers=headers, json={"default_language": "fr"})
    assert r.status_code == 400
    assert "fr" in r.json()["detail"]


def test_patch_settings_accepts_each_playback_speed(
    client: TestClient, headers: dict[str, str]
) -> None:
    for speed in ("slow", "very_slow", "normal"):
        r = client.patch("/api/v1/settings", headers=headers, json={"playback_speed": speed})
        assert r.status_code == 200, r.text
        assert r.json()["playback_speed"] == speed


def test_patch_settings_offline_enabled(client: TestClient, headers: dict[str, str]) -> None:
    r = client.patch("/api/v1/settings", headers=headers, json={"offline_enabled": True})
    assert r.status_code == 200
    assert r.json()["offline_enabled"] is True
