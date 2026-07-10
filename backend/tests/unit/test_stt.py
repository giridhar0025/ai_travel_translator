import pytest

from app.services.stt import STTService


class _FakeHFClient:
    def __init__(self, response):
        self.response = response
        self.calls: list[dict] = []

    async def transcribe_openai(
        self,
        *,
        provider,
        model,
        audio_bytes,
        content_type="audio/wav",
        filename="audio",
        max_retries=2,
    ):
        self.calls.append(
            {
                "provider": provider,
                "model": model,
                "audio_bytes": audio_bytes,
                "content_type": content_type,
                "filename": filename,
            }
        )
        return self.response


@pytest.mark.asyncio
async def test_transcribe_returns_result() -> None:
    fake = _FakeHFClient(response={"text": " Hello, how are you?", "language": "en"})
    svc = STTService(fake, "openai/whisper-large-v3", "together")  # type: ignore[arg-type]

    result = await svc.transcribe(b"<audio bytes>", language="en", content_type="audio/mp4")

    assert result.transcript == "Hello, how are you?"  # stripped
    assert result.language == "en"
    assert result.confidence is None
    assert result.model == "openai/whisper-large-v3"
    assert result.latency_ms >= 0
    call = fake.calls[0]
    assert call["provider"] == "together"
    assert call["model"] == "openai/whisper-large-v3"
    assert call["audio_bytes"] == b"<audio bytes>"
    assert call["content_type"] == "audio/mp4"


@pytest.mark.asyncio
async def test_transcribe_falls_back_to_detected_language() -> None:
    fake = _FakeHFClient(response={"text": "hola", "language": "es"})
    svc = STTService(fake, "m")  # type: ignore[arg-type]

    result = await svc.transcribe(b"x")

    # No client hint → use the provider's detected language.
    assert result.transcript == "hola"
    assert result.language == "es"


@pytest.mark.asyncio
async def test_transcribe_handles_missing_text_field() -> None:
    fake = _FakeHFClient(response={})
    svc = STTService(fake, "m")  # type: ignore[arg-type]

    result = await svc.transcribe(b"x")

    assert result.transcript == ""
    assert result.language is None


@pytest.mark.asyncio
async def test_transcribe_handles_string_response() -> None:
    fake = _FakeHFClient(response="raw string response")
    svc = STTService(fake, "m")  # type: ignore[arg-type]

    result = await svc.transcribe(b"x")

    assert result.transcript == "raw string response"
