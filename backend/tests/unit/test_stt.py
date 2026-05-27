import pytest

from app.services.stt import STTService


class _FakeHFClient:
    def __init__(self, response):
        self.response = response
        self.calls: list[tuple] = []

    async def infer(
        self,
        model,
        *,
        json_payload=None,
        content=None,
        content_type="audio/wav",
        raw=False,
        max_retries=3,
    ):
        self.calls.append((model, json_payload, content, content_type, raw))
        return self.response


@pytest.mark.asyncio
async def test_transcribe_returns_result() -> None:
    fake = _FakeHFClient(response={"text": " Hello, how are you?"})
    svc = STTService(fake, "openai/whisper-large-v3")  # type: ignore[arg-type]

    result = await svc.transcribe(b"<audio bytes>", language="en")

    assert result.transcript == "Hello, how are you?"  # stripped
    assert result.language == "en"
    assert result.confidence is None
    assert result.model == "openai/whisper-large-v3"
    assert result.latency_ms >= 0
    model, payload, content, content_type, _ = fake.calls[0]
    assert model == "openai/whisper-large-v3"
    assert payload is None
    assert content == b"<audio bytes>"
    assert content_type == "audio/wav"


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
