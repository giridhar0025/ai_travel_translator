"""Translation service backed by the OPUS-MT family on the HF Inference router.

HF deprecated `api-inference.huggingface.co` and routed serverless inference
through `router.huggingface.co/hf-inference`. Not all models are available
there anymore — `facebook/m2m100_*` and `facebook/nllb-*` returned
"Model not supported by provider hf-inference" during Phase 3 live testing.

OPUS-MT (Helsinki-NLP, CC-BY 4.0 — commercial-safe) IS available and covers
our four languages via two multilingual checkpoints:

  • English → {hi, te, th}:  `Helsinki-NLP/opus-mt-en-mul` with `>>tgt<<` prefix
  • {hi, te, th} → English:  `Helsinki-NLP/opus-mt-mul-en`

Cross-language pairs (hi↔te, hi↔th, te↔th) chain via English in two hops.

Per-language IPA pronunciation guides come from `epitran` (English returns None).
Same-language and unsupported-language requests raise before any HF call.
"""

import asyncio
import time
from collections import OrderedDict
from dataclasses import dataclass
from functools import lru_cache
from typing import Any

from app.services.hf_client import HuggingFaceClient, get_hf_client

SUPPORTED_LANGUAGES: frozenset[str] = frozenset({"en", "hi", "te", "th"})

EN_TO_MUL_MODEL = "Helsinki-NLP/opus-mt-en-mul"
MUL_TO_EN_MODEL = "Helsinki-NLP/opus-mt-mul-en"

# ISO 639-1 → 3-letter prefix the opus-mt-en-mul model expects
OPUS_LANG_PREFIX: dict[str, str] = {
    "hi": "hin",
    "te": "tel",
    "th": "tha",
}

# epitran ISO mapping for the langs whose IPA we want
EPITRAN_CODES: dict[str, str] = {
    "hi": "hin-Deva",
    "te": "tel-Telu",
    "th": "tha-Thai",
}


class UnsupportedLanguageError(ValueError):
    pass


class SameLanguageError(ValueError):
    pass


@dataclass(frozen=True)
class TranslationResult:
    translation: str
    pronunciation: str | None
    latency_ms: int
    model: str


@lru_cache(maxsize=4)
def _epitran_for(lang_code: str) -> Any | None:
    code = EPITRAN_CODES.get(lang_code)
    if code is None:
        return None
    try:
        import epitran

        return epitran.Epitran(code)
    except Exception:
        return None


def derive_pronunciation(text: str, target: str) -> str | None:
    """IPA transliteration for non-Latin scripts. Latin/English returns None."""
    if target == "en":
        return None
    epi = _epitran_for(target)
    if epi is None:
        return None
    try:
        return epi.transliterate(text)
    except Exception:
        return None


class _LRU:
    """Tiny OrderedDict-based LRU. Replaces functools.lru_cache for async values."""

    def __init__(self, maxsize: int = 1000) -> None:
        self.maxsize = maxsize
        self._data: OrderedDict[tuple, TranslationResult] = OrderedDict()

    def get(self, key: tuple) -> TranslationResult | None:
        if key not in self._data:
            return None
        self._data.move_to_end(key)
        return self._data[key]

    def put(self, key: tuple, value: TranslationResult) -> None:
        if key in self._data:
            self._data.move_to_end(key)
        self._data[key] = value
        if len(self._data) > self.maxsize:
            self._data.popitem(last=False)


class TranslationService:
    def __init__(self, hf: HuggingFaceClient) -> None:
        self.hf = hf
        self._cache = _LRU(maxsize=1000)
        self._batch_concurrency = asyncio.Semaphore(5)

    def _validate(self, source: str, target: str) -> None:
        if source not in SUPPORTED_LANGUAGES:
            raise UnsupportedLanguageError(f"source language {source!r} not supported")
        if target not in SUPPORTED_LANGUAGES:
            raise UnsupportedLanguageError(f"target language {target!r} not supported")
        if source == target:
            raise SameLanguageError("source and target languages must differ")

    @staticmethod
    def _extract_translation(raw: Any) -> str:
        # OPUS-MT returns [{"translation_text": "..."}]
        if isinstance(raw, list) and raw and isinstance(raw[0], dict):
            return str(raw[0].get("translation_text", raw[0]))
        if isinstance(raw, dict) and "translation_text" in raw:
            return str(raw["translation_text"])
        return str(raw)

    async def _one_hop(self, source: str, target: str, text: str) -> tuple[str, str]:
        """Single HF call. One of source/target must be English. Returns (text, model)."""
        if source == "en":
            prefix = OPUS_LANG_PREFIX[target]
            payload = {"inputs": f">>{prefix}<< {text}"}
            model = EN_TO_MUL_MODEL
        elif target == "en":
            payload = {"inputs": text}
            model = MUL_TO_EN_MODEL
        else:  # pragma: no cover  — caller guarantees one side is English
            raise AssertionError("_one_hop requires English on one side")
        raw = await self.hf.infer(model, json_payload=payload)
        return self._extract_translation(raw), model

    async def translate(
        self,
        source: str,
        target: str,
        text: str,
        context: str | None = None,
    ) -> TranslationResult:
        self._validate(source, target)
        cache_key = (source, target, text, context)
        cached = self._cache.get(cache_key)
        if cached is not None:
            return cached

        prepared = f"[Context: {context}] {text}" if context else text
        start = time.monotonic()

        if source == "en" or target == "en":
            translation, model_used = await self._one_hop(source, target, prepared)
        else:
            # Chain via English: source → en → target
            english, _ = await self._one_hop(source, "en", prepared)
            translation, _ = await self._one_hop("en", target, english)
            model_used = f"{MUL_TO_EN_MODEL}+{EN_TO_MUL_MODEL}"

        latency_ms = int((time.monotonic() - start) * 1000)
        result = TranslationResult(
            translation=translation,
            pronunciation=derive_pronunciation(translation, target),
            latency_ms=latency_ms,
            model=model_used,
        )
        self._cache.put(cache_key, result)
        return result

    async def translate_batch(
        self, source: str, target: str, items: list[str]
    ) -> list[TranslationResult]:
        self._validate(source, target)

        async def _one(text: str) -> TranslationResult:
            async with self._batch_concurrency:
                return await self.translate(source, target, text)

        return await asyncio.gather(*(_one(t) for t in items))


_service: TranslationService | None = None


def get_translation_service() -> TranslationService:
    global _service
    if _service is None:
        _service = TranslationService(get_hf_client())
    return _service


def reset_translation_service_for_tests() -> None:
    global _service
    _service = None
