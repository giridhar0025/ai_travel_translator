"""Speech-to-text service backed by `openai/whisper-large-v3` via HF Inference.

The HF Inference router accepts raw audio bytes (any whisper-supported format)
and returns `{"text": "..."}`. Whisper auto-detects the spoken language; we
don't try to parse it from the response (the router doesn't return it). The
client passes `language` to be echoed in the response so the UI can pre-set
the subsequent translation direction.
"""

import time
from dataclasses import dataclass

from app.config import get_settings
from app.services.groq_client import GroqClient, get_groq_client
from app.services.hf_client import HuggingFaceClient, get_hf_client


@dataclass(frozen=True)
class TranscriptionResult:
    transcript: str
    language: str | None
    confidence: float | None
    latency_ms: int
    model: str


class STTService:
    def __init__(
        self,
        hf: HuggingFaceClient,
        model: str,
        provider: str = "together",
        *,
        groq: GroqClient | None = None,
        groq_model: str = "whisper-large-v3-turbo",
        stt_provider: str = "hf",
    ) -> None:
        self.hf = hf
        self.model = model
        self.provider = provider
        self.groq = groq
        self.groq_model = groq_model
        self.stt_provider = stt_provider

    async def transcribe(
        self,
        audio_bytes: bytes,
        language: str | None = None,
        content_type: str = "audio/wav",
        filename: str = "audio",
    ) -> TranscriptionResult:
        start = time.monotonic()
        if self.stt_provider == "groq" and self.groq is not None:
            raw = await self.groq.transcribe_openai(
                model=self.groq_model,
                audio_bytes=audio_bytes,
                content_type=content_type,
                filename=filename,
                language=language,
            )
            used_model = self.groq_model
        else:
            raw = await self.hf.transcribe_openai(
                provider=self.provider,
                model=self.model,
                audio_bytes=audio_bytes,
                content_type=content_type,
                filename=filename,
            )
            used_model = self.model
        latency_ms = int((time.monotonic() - start) * 1000)

        # OpenAI-compatible ASR returns {"text": "...", "language": "en", ...}
        transcript = str(raw.get("text", "")).strip() if isinstance(raw, dict) else str(raw).strip()
        detected = raw.get("language") if isinstance(raw, dict) else None

        return TranscriptionResult(
            transcript=transcript,
            # Prefer the client's hint (drives the UI's next translation direction);
            # fall back to the provider's detected language.
            language=language or detected,
            confidence=None,  # not returned by the router
            latency_ms=latency_ms,
            model=used_model,
        )


_service: STTService | None = None


def get_stt_service() -> STTService:
    global _service
    if _service is None:
        settings = get_settings()
        resolved = settings.resolved_stt_provider
        _service = STTService(
            get_hf_client(),
            settings.hf_model_stt,
            settings.hf_stt_provider,
            groq=get_groq_client() if resolved == "groq" else None,
            groq_model=settings.groq_model_stt,
            stt_provider=resolved,
        )
    return _service


def reset_stt_service_for_tests() -> None:
    global _service
    _service = None
