"""Text-to-speech service.

**Provider note (2026-05):** the HF `hf-inference` serverless provider stopped
hosting `parler-tts/parler-tts-mini-v1` and `ai4bharat/indic-parler-tts`.
Phase 5 was rerouted to `ResembleAI/chatterbox` (MIT) via the **Replicate**
provider on the HF router (`https://router.huggingface.co/replicate/...`).
Chatterbox supports English + Hindi (and 18 others) but NOT Telugu, so Telugu
joins Thai in the client-side fallback bucket — the Android/iOS clients use
their built-in TTS engines.
"""

import io
from dataclasses import dataclass
from typing import Literal

from app.config import get_settings
from app.services.hf_client import HuggingFaceClient, get_hf_client
from app.services.translation import SUPPORTED_LANGUAGES

Speed = Literal["normal", "slow"]

# `resemble-ai/chatterbox-turbo` is the Replicate "version" string the router
# expects. Chatterbox is MIT licensed (commercial-safe). Language is inferred
# from the script of the input text — no `language` parameter needed.
CHATTERBOX_VERSION = "resemble-ai/chatterbox-turbo"

# Languages served by chatterbox via Replicate. Expand cautiously — chatterbox
# claims 20 languages but we only ship what we've actually validated.
SERVER_TTS_LANGS: frozenset[str] = frozenset({"en", "hi"})

# Every other supported language has no validated server-side voice; the
# mobile client falls back to its on-device TTS engine when it sees the 415
# `client_side_tts_required` contract.
CLIENT_SIDE_TTS_LANGS: frozenset[str] = SUPPORTED_LANGUAGES - SERVER_TTS_LANGS


class ClientSideTtsRequired(Exception):
    """Raised when the language has no server-side TTS and the client must
    use its on-device engine (Telugu and Thai today).

    Class name is the public 415 detail-code contract clients match on —
    see `pyproject.toml` `[tool.ruff.lint.per-file-ignores]`."""

    def __init__(self, language: str) -> None:
        super().__init__(f"client-side TTS required for language={language}")
        self.language = language


class UnsupportedLanguageError(Exception):
    """Raised when the language is not in any TTS bucket at all."""

    def __init__(self, language: str) -> None:
        super().__init__(f"unsupported TTS language: {language}")
        self.language = language


@dataclass(frozen=True)
class SynthesisResult:
    audio: bytes
    model: str
    content_type: str = "audio/wav"


class TTSService:
    SLOW_RATE = 0.7  # librosa time_stretch rate < 1.0 → slower playback, pitch preserved.

    def __init__(self, hf: HuggingFaceClient, version: str = CHATTERBOX_VERSION) -> None:
        self.hf = hf
        self.version = version

    async def synthesize(
        self, text: str, language: str, speed: Speed = "normal"
    ) -> SynthesisResult:
        if language in CLIENT_SIDE_TTS_LANGS:
            raise ClientSideTtsRequired(language)
        if language not in SERVER_TTS_LANGS:
            raise UnsupportedLanguageError(language)

        audio = await self.hf.predict_replicate(
            version=self.version,
            input_payload={"text": text},
        )
        if not isinstance(audio, bytes):
            # Defensive — predict_replicate is typed to return bytes, but if
            # the CDN ever changes shape surface it cleanly instead of 500'ing.
            raise RuntimeError(f"expected audio bytes from Replicate, got {type(audio).__name__}")

        if speed == "slow":
            audio = self._time_stretch(audio, self.SLOW_RATE)

        return SynthesisResult(audio=audio, model=self.version)

    @staticmethod
    def _time_stretch(wav_bytes: bytes, rate: float) -> bytes:
        try:
            import librosa
            import soundfile as sf
        except ImportError:
            # `librosa` is not installed in the Vercel bundle (its numba /
            # llvmlite / scipy tree alone exceeds the function size limit).
            # Serve the normal-speed audio instead of failing the request.
            return wav_bytes

        y, sr = sf.read(io.BytesIO(wav_bytes), dtype="float32", always_2d=False)
        if y.ndim > 1:
            y = y.mean(axis=1)  # collapse to mono for librosa
        stretched = librosa.effects.time_stretch(y, rate=rate)

        out = io.BytesIO()
        sf.write(out, stretched, sr, format="WAV", subtype="PCM_16")
        return out.getvalue()


_service: TTSService | None = None


def get_tts_service() -> TTSService:
    global _service
    if _service is None:
        version = get_settings().replicate_tts_version or CHATTERBOX_VERSION
        _service = TTSService(get_hf_client(), version=version)
    return _service


def reset_tts_service_for_tests() -> None:
    global _service
    _service = None
