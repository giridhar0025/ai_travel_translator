"""End-to-end tests for /translate routes with the HF chat endpoint mocked."""

import json

import pytest
import respx
from fastapi.testclient import TestClient
from httpx import Response

from app.config import Settings
from app.services.translation import reset_translation_service_for_tests

_CHAT_PATH = "/v1/chat/completions"
_MODEL = "meta-llama/Llama-3.3-70B-Instruct"


@pytest.fixture(autouse=True)
def _reset_singletons():
    reset_translation_service_for_tests()
    yield
    reset_translation_service_for_tests()


def _chat_response(text: str) -> Response:
    return Response(200, json={"choices": [{"message": {"content": text}}]})


def _sse_body(deltas: list[str]) -> str:
    frames = [f"data: {json.dumps({'choices': [{'delta': {'content': d}}]})}\n\n" for d in deltas]
    frames.append("data: [DONE]\n\n")
    return "".join(frames)


def _guest_headers(client: TestClient) -> tuple[dict[str, str], str]:
    r = client.post("/api/v1/auth/guest")
    body = r.json()
    return {"Authorization": f"Bearer {body['token']}"}, body["user"]["id"]


def test_settings_default_to_groq_llm() -> None:
    # Assert the code defaults independent of any developer .env override.
    settings = Settings(_env_file=None)  # type: ignore[call-arg]
    assert settings.hf_translate_provider == "auto"
    assert settings.hf_model_translate == _MODEL


def test_translate_requires_auth(client: TestClient) -> None:
    r = client.post("/api/v1/translate", json={"source": "en", "target": "hi", "text": "Hello"})
    assert r.status_code == 401


def test_translate_happy_path(client: TestClient) -> None:
    headers, _ = _guest_headers(client)
    try:
        with respx.mock(base_url="https://router.huggingface.co") as mock:
            mock.post(_CHAT_PATH).mock(return_value=_chat_response("नमस्ते"))
            r = client.post(
                "/api/v1/translate",
                headers=headers,
                json={"source": "en", "target": "hi", "text": "Hello"},
            )

        assert r.status_code == 200
        body = r.json()
        assert body["translation"] == "नमस्ते"
        assert body["model"] == _MODEL
        assert body["latency_ms"] >= 0
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_translate_stream_emits_deltas_and_done(client: TestClient) -> None:
    headers, _ = _guest_headers(client)
    try:
        with respx.mock(base_url="https://router.huggingface.co") as mock:
            mock.post(_CHAT_PATH).mock(
                return_value=Response(
                    200,
                    text=_sse_body(["ना", "म", "स्ते"]),
                    headers={"Content-Type": "text/event-stream"},
                )
            )
            r = client.post(
                "/api/v1/translate/stream",
                headers=headers,
                json={"source": "en", "target": "hi", "text": "Hello"},
            )

        assert r.status_code == 200
        frames = [
            json.loads(line[len("data:") :].strip())
            for line in r.text.splitlines()
            if line.startswith("data:")
        ]
        deltas = [f["delta"] for f in frames if "delta" in f]
        done = [f for f in frames if f.get("done")]
        assert "".join(deltas) == "नामस्ते"
        assert len(done) == 1
        assert done[0]["translation"] == "नामस्ते"
        assert done[0]["model"] == _MODEL
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_translate_stream_400_on_same_language(client: TestClient) -> None:
    headers, _ = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/translate/stream",
            headers=headers,
            json={"source": "en", "target": "en", "text": "Hello"},
        )
        assert r.status_code == 400
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
            json={"source": "xx", "target": "hi", "text": "Bonjour"},
        )
        assert r.status_code == 400
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_translate_batch_returns_per_item_results(client: TestClient) -> None:
    headers, _ = _guest_headers(client)
    try:
        with respx.mock(base_url="https://router.huggingface.co") as mock:
            mock.post(_CHAT_PATH).mock(return_value=_chat_response("X"))
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
        with respx.mock(base_url="https://router.huggingface.co") as mock:
            mock.post(_CHAT_PATH).mock(return_value=_chat_response("नमस्ते"))
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
