import io

import numpy as np
import pytest
import soundfile as sf

from app.services.tts import (
    CHATTERBOX_VERSION,
    ClientSideTtsRequired,
    TTSService,
    UnsupportedLanguageError,
)


class _FakeHFClient:
    """Captures predict_replicate calls and returns a pre-baked WAV blob."""

    def __init__(self, audio: bytes):
        self.audio = audio
        self.calls: list[dict] = []

    async def predict_replicate(self, *, version, input_payload, **kwargs):
        self.calls.append({"version": version, "input": input_payload, **kwargs})
        return self.audio

    # `infer` exists on the real client; provide a stub so test typing stays loose.
    async def infer(self, *args, **kwargs):  # pragma: no cover
        raise NotImplementedError("TTS tests should never call infer()")


def _wav_bytes(duration_seconds: float = 1.0, sample_rate: int = 24_000) -> bytes:
    n = int(duration_seconds * sample_rate)
    y = (0.1 * np.sin(2 * np.pi * 440 * np.arange(n) / sample_rate)).astype("float32")
    buf = io.BytesIO()
    sf.write(buf, y, sample_rate, format="WAV", subtype="PCM_16")
    return buf.getvalue()


@pytest.mark.asyncio
async def test_synthesize_thai_raises_client_side_required() -> None:
    svc = TTSService(_FakeHFClient(audio=b""))  # type: ignore[arg-type]
    with pytest.raises(ClientSideTtsRequired) as exc:
        await svc.synthesize("สวัสดี", language="th")
    assert exc.value.language == "th"


@pytest.mark.asyncio
async def test_synthesize_telugu_raises_client_side_required() -> None:
    # Phase 5 swap: Telugu has no commercial-safe server-side TTS today, so
    # it joins Thai in the client-side bucket. This used to be UnsupportedLanguageError
    # under the (now defunct) Indic-Parler plan.
    svc = TTSService(_FakeHFClient(audio=b""))  # type: ignore[arg-type]
    with pytest.raises(ClientSideTtsRequired) as exc:
        await svc.synthesize("నమస్కారం", language="te")
    assert exc.value.language == "te"


@pytest.mark.asyncio
async def test_synthesize_unsupported_language_raises() -> None:
    svc = TTSService(_FakeHFClient(audio=b""))  # type: ignore[arg-type]
    with pytest.raises(UnsupportedLanguageError) as exc:
        await svc.synthesize("Bonjour", language="fr")
    assert exc.value.language == "fr"


@pytest.mark.asyncio
async def test_synthesize_english_calls_chatterbox() -> None:
    wav = _wav_bytes()
    fake = _FakeHFClient(audio=wav)
    svc = TTSService(fake)  # type: ignore[arg-type]

    result = await svc.synthesize("Hello, world.", language="en")

    assert result.audio == wav
    assert result.model == CHATTERBOX_VERSION
    assert result.content_type == "audio/wav"
    assert len(fake.calls) == 1
    call = fake.calls[0]
    assert call["version"] == CHATTERBOX_VERSION
    assert call["input"] == {"text": "Hello, world."}


@pytest.mark.asyncio
async def test_synthesize_hindi_calls_chatterbox() -> None:
    fake = _FakeHFClient(audio=_wav_bytes())
    svc = TTSService(fake)  # type: ignore[arg-type]

    result = await svc.synthesize("नमस्ते", language="hi")

    assert result.model == CHATTERBOX_VERSION
    call = fake.calls[0]
    assert call["version"] == CHATTERBOX_VERSION
    assert call["input"]["text"] == "नमस्ते"


@pytest.mark.asyncio
async def test_synthesize_slow_time_stretches_audio() -> None:
    original = _wav_bytes(duration_seconds=1.0, sample_rate=24_000)
    fake = _FakeHFClient(audio=original)
    svc = TTSService(fake)  # type: ignore[arg-type]

    result = await svc.synthesize("Hello", language="en", speed="slow")

    orig_info = sf.info(io.BytesIO(original))
    new_info = sf.info(io.BytesIO(result.audio))
    assert new_info.samplerate == orig_info.samplerate
    assert new_info.duration > orig_info.duration  # rate=0.7 → ~1/0.7x longer


@pytest.mark.asyncio
async def test_synthesize_normal_returns_audio_unchanged() -> None:
    original = _wav_bytes()
    fake = _FakeHFClient(audio=original)
    svc = TTSService(fake)  # type: ignore[arg-type]

    result = await svc.synthesize("Hello", language="en", speed="normal")
    assert result.audio == original


@pytest.mark.asyncio
async def test_synthesize_rejects_non_bytes_response() -> None:
    class _BadClient:
        async def predict_replicate(self, **kwargs):
            return {"error": "boom"}  # not bytes — defensive guard should fire

    svc = TTSService(_BadClient())  # type: ignore[arg-type]
    with pytest.raises(RuntimeError):
        await svc.synthesize("Hello", language="en")
