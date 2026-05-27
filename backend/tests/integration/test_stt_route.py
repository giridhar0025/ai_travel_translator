import io

import numpy as np
import pytest
import respx
import soundfile as sf
from fastapi.testclient import TestClient
from httpx import Response

from app.services.stt import reset_stt_service_for_tests


@pytest.fixture(autouse=True)
def _reset_singletons():
    reset_stt_service_for_tests()
    yield
    reset_stt_service_for_tests()


def _wav_bytes(duration_seconds: float = 1.0, sample_rate: int = 16_000) -> bytes:
    n = int(duration_seconds * sample_rate)
    buf = io.BytesIO()
    sf.write(buf, np.zeros(n, dtype="float32"), sample_rate, format="WAV", subtype="PCM_16")
    return buf.getvalue()


def _guest_headers(client: TestClient) -> dict[str, str]:
    body = client.post("/api/v1/auth/guest").json()
    return {"Authorization": f"Bearer {body['token']}"}


def test_transcribe_requires_auth(client: TestClient) -> None:
    r = client.post(
        "/api/v1/stt/transcribe",
        files={"audio": ("test.wav", _wav_bytes(), "audio/wav")},
    )
    assert r.status_code == 401


def test_transcribe_happy_path(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        with respx.mock(base_url="https://router.huggingface.co/hf-inference") as mock:
            mock.post("/models/openai/whisper-large-v3").mock(
                return_value=Response(200, json={"text": " Hello world"})
            )
            r = client.post(
                "/api/v1/stt/transcribe",
                headers=headers,
                files={"audio": ("test.wav", _wav_bytes(), "audio/wav")},
                data={"language": "en"},
            )

        assert r.status_code == 200, r.text
        body = r.json()
        assert body["transcript"] == "Hello world"
        assert body["language"] == "en"
        assert body["model"] == "openai/whisper-large-v3"
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_transcribe_rejects_unsupported_content_type(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/stt/transcribe",
            headers=headers,
            files={"audio": ("x.txt", b"not audio", "text/plain")},
        )
        assert r.status_code == 415
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_transcribe_rejects_oversize_audio(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        # 11 MB of zeros — exceeds 10 MB cap. Send as raw bytes labelled wav.
        big = b"\x00" * (11 * 1024 * 1024)
        r = client.post(
            "/api/v1/stt/transcribe",
            headers=headers,
            files={"audio": ("big.wav", big, "audio/wav")},
        )
        assert r.status_code == 413
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_transcribe_rejects_overlong_audio(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        long_wav = _wav_bytes(duration_seconds=31.0)
        r = client.post(
            "/api/v1/stt/transcribe",
            headers=headers,
            files={"audio": ("long.wav", long_wav, "audio/wav")},
        )
        assert r.status_code == 400
    finally:
        client.delete("/api/v1/me", headers=headers)


def test_transcribe_rejects_empty_audio(client: TestClient) -> None:
    headers = _guest_headers(client)
    try:
        r = client.post(
            "/api/v1/stt/transcribe",
            headers=headers,
            files={"audio": ("empty.wav", b"", "audio/wav")},
        )
        assert r.status_code == 400
    finally:
        client.delete("/api/v1/me", headers=headers)
