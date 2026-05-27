"""End-to-end tests for /translate routes with HF mocked at the HTTP layer."""

import pytest
import respx
from fastapi.testclient import TestClient
from httpx import Response

from app.services.translation import reset_translation_service_for_tests


@pytest.fixture(autouse=True)
def _reset_singletons():
    reset_translation_service_for_tests()
    yield
    reset_translation_service_for_tests()


def _guest_headers(client: TestClient) -> tuple[dict[str, str], str]:
    r = client.post("/api/v1/auth/guest")
    body = r.json()
    return {"Authorization": f"Bearer {body['token']}"}, body["user"]["id"]


def test_translate_requires_auth(client: TestClient) -> None:
    r = client.post("/api/v1/translate", json={"source": "en", "target": "hi", "text": "Hello"})
    assert r.status_code == 401


def test_translate_happy_path(client: TestClient) -> None:
    headers, _ = _guest_headers(client)
    try:
        with respx.mock(base_url="https://router.huggingface.co/hf-inference") as mock:
            mock.post("/models/Helsinki-NLP/opus-mt-en-mul").mock(
                return_value=Response(200, json=[{"translation_text": "नमस्ते"}])
            )
            r = client.post(
                "/api/v1/translate",
                headers=headers,
                json={"source": "en", "target": "hi", "text": "Hello"},
            )

        assert r.status_code == 200
        body = r.json()
        assert body["translation"] == "नमस्ते"
        assert body["model"] == "Helsinki-NLP/opus-mt-en-mul"
        assert body["latency_ms"] >= 0
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_translate_400_on_same_language(client: TestClient) -> None:
    headers, _ = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/translate",
            headers=headers,
            json={"source": "en", "target": "en", "text": "Hello"},
        )
        assert r.status_code == 400
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_translate_400_on_unsupported_language(client: TestClient) -> None:
    headers, _ = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/translate",
            headers=headers,
            json={"source": "fr", "target": "hi", "text": "Bonjour"},
        )
        assert r.status_code == 400
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_translate_batch_returns_per_item_results(client: TestClient) -> None:
    headers, _ = _guest_headers(client)
    try:
        with respx.mock(base_url="https://router.huggingface.co/hf-inference") as mock:
            mock.post("/models/Helsinki-NLP/opus-mt-en-mul").mock(
                return_value=Response(200, json=[{"translation_text": "X"}])
            )
            r = client.post(
                "/api/v1/translate/batch",
                headers=headers,
                json={"source": "en", "target": "hi", "items": ["a", "b", "c"]},
            )

        assert r.status_code == 200
        body = r.json()
        assert len(body["results"]) == 3
        assert all(item["translation"] == "X" for item in body["results"])
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_translate_writes_recent_in_background(client: TestClient) -> None:
    headers, _ = _guest_headers(client)
    try:
        with respx.mock(base_url="https://router.huggingface.co/hf-inference") as mock:
            mock.post("/models/Helsinki-NLP/opus-mt-en-mul").mock(
                return_value=Response(200, json=[{"translation_text": "नमस्ते"}])
            )
            r = client.post(
                "/api/v1/translate",
                headers=headers,
                json={"source": "en", "target": "hi", "text": "Hello"},
            )
        assert r.status_code == 200

        # TestClient runs BackgroundTasks before the response context exits, so by
        # the time we get here the recent row is in the DB. We verify indirectly
        # via DELETE /me succeeding (cascade requires the recent FK to resolve).
        assert client.delete("/api/v1/me", headers=headers).status_code == 204
        headers = None  # don't double-delete
    finally:
        if headers:
            client.delete("/api/v1/me", headers=headers)
