import io

import numpy as np
import pytest
import respx
import soundfile as sf
from fastapi.testclient import TestClient
from httpx import Response

from app.services.tts import CHATTERBOX_VERSION, reset_tts_service_for_tests


@pytest.fixture(autouse=True)
def _reset_singletons():
    reset_tts_service_for_tests()
    yield
    reset_tts_service_for_tests()


def _wav_bytes(duration_seconds: float = 1.0, sample_rate: int = 24_000) -> bytes:
    n = int(duration_seconds * sample_rate)
    y = (0.1 * np.sin(2 * np.pi * 440 * np.arange(n) / sample_rate)).astype("float32")
    buf = io.BytesIO()
    sf.write(buf, y, sample_rate, format="WAV", subtype="PCM_16")
    return buf.getvalue()


def _guest_headers(client: TestClient) -> dict[str, str]:
    body = client.post("/api/v1/auth/guest").json()
    return {"Authorization": f"Bearer {body['token']}"}


def _mock_replicate(mock: respx.MockRouter, *, pred_id: str, wav: bytes) -> None:
    """Stub the three Replicate hops: create → poll-succeeded → CDN download."""
    output_url = f"https://replicate.delivery/test/{pred_id}/output.wav"
    mock.post("https://router.huggingface.co/replicate/v1/predictions").mock(
        return_value=Response(
            201,
            json={
                "id": pred_id,
                "model": "resemble-ai/chatterbox-turbo",
                "version": CHATTERBOX_VERSION,
                "input": {"text": "ignored"},
                "status": "succeeded",  # complete on create — service short-circuits the poll
                "output": output_url,
            },
        )
    )
    mock.get(output_url).mock(
        return_value=Response(200, content=wav, headers={"content-type": "audio/wav"})
    )


def test_synthesize_requires_auth(client: TestClient) -> None:
    r = client.post("/api/v1/tts/synthesize", json={"text": "hi", "language": "en"})
    assert r.status_code == 401


def test_synthesize_english_happy_path(client: TestClient) -> None:
    headers = _guest_headers(client)
    wav = _wav_bytes()
    try:
        with respx.mock(assert_all_called=False) as mock:
            _mock_replicate(mock, pred_id="p_en", wav=wav)
            r = client.post(
                "/api/v1/tts/synthesize",
                headers=headers,
                json={"text": "Hello world", "language": "en", "speed": "normal"},
            )

        assert r.status_code == 200, r.text
        assert r.headers["content-type"] == "audio/wav"
        assert r.headers["x-tts-model"] == CHATTERBOX_VERSION
        assert r.content == wav
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_synthesize_hindi_uses_same_chatterbox_version(client: TestClient) -> None:
    headers = _guest_headers(client)
    wav = _wav_bytes()
    try:
        with respx.mock(assert_all_called=False) as mock:
            create = mock.post("https://router.huggingface.co/replicate/v1/predictions").mock(
                return_value=Response(
                    201,
                    json={
                        "id": "p_hi",
                        "status": "succeeded",
                        "output": "https://replicate.delivery/test/p_hi/output.wav",
                    },
                )
            )
            mock.get("https://replicate.delivery/test/p_hi/output.wav").mock(
                return_value=Response(200, content=wav, headers={"content-type": "audio/wav"})
            )
            r = client.post(
                "/api/v1/tts/synthesize",
                headers=headers,
                json={"text": "नमस्ते", "language": "hi"},
            )

        assert r.status_code == 200, r.text
        assert r.headers["x-tts-model"] == CHATTERBOX_VERSION
        # Confirm we routed through Replicate, not legacy /models/parler-*
        assert create.called
        sent = create.calls[0].request
        assert b'"version"' in sent.content
        assert b"chatterbox-turbo" in sent.content
        assert "नमस्ते".encode() in sent.content
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_synthesize_thai_returns_client_side_required(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/tts/synthesize",
            headers=headers,
            json={"text": "สวัสดี", "language": "th"},
        )
        assert r.status_code == 415
        body = r.json()
        assert body["detail"]["detail"] == "client_side_tts_required"
        assert body["detail"]["language"] == "th"
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_synthesize_telugu_returns_client_side_required(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/tts/synthesize",
            headers=headers,
            json={"text": "నమస్కారం", "language": "te"},
        )
        assert r.status_code == 415
        body = r.json()
        assert body["detail"]["detail"] == "client_side_tts_required"
        assert body["detail"]["language"] == "te"
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_synthesize_unsupported_language_returns_415(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/tts/synthesize",
            headers=headers,
            json={"text": "Bonjour", "language": "xx"},
        )
        assert r.status_code == 415
        body = r.json()
        assert body["detail"]["detail"] == "unsupported_language"
        assert body["detail"]["language"] == "xx"
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_synthesize_empty_text_returns_422(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/tts/synthesize",
            headers=headers,
            json={"text": "", "language": "en"},
        )
        assert r.status_code == 422
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_synthesize_whitespace_only_text_returns_400(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/tts/synthesize",
            headers=headers,
            json={"text": "   ", "language": "en"},
        )
        assert r.status_code == 400
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_synthesize_overlong_text_returns_422(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/tts/synthesize",
            headers=headers,
            json={"text": "x" * 501, "language": "en"},
        )
        assert r.status_code == 422
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_synthesize_slow_speed_returns_longer_audio(client: TestClient) -> None:
    headers = _guest_headers(client)
    original = _wav_bytes(duration_seconds=1.0)
    try:
        with respx.mock(assert_all_called=False) as mock:
            _mock_replicate(mock, pred_id="p_slow", wav=original)
            r = client.post(
                "/api/v1/tts/synthesize",
                headers=headers,
                json={"text": "Hello world", "language": "en", "speed": "slow"},
            )

        assert r.status_code == 200, r.text
        orig_info = sf.info(io.BytesIO(original))
        new_info = sf.info(io.BytesIO(r.content))
        assert new_info.duration > orig_info.duration
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_synthesize_replicate_failure_returns_502(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        with respx.mock(assert_all_called=False) as mock:
            mock.post("https://router.huggingface.co/replicate/v1/predictions").mock(
                return_value=Response(
                    201,
                    json={
                        "id": "p_fail",
                        "status": "failed",
                        "error": "model exploded",
                    },
                )
            )
            r = client.post(
                "/api/v1/tts/synthesize",
                headers=headers,
                json={"text": "Hello", "language": "en"},
            )
        assert r.status_code == 502, r.text
        body = r.json()
        assert body["detail"]["detail"] == "tts_provider_failed"
        assert "model exploded" in body["detail"]["message"]
    finally:
        client.delete("/api/v1/me", headers=headers)
