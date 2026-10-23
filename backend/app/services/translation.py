"""Translation service backed by an instruction-following LLM on the HF router.

The previous OPUS-MT (Helsinki-NLP) engine could only do literal, word-for-word
seq2seq translation — it ignored the `context` field and flattened idioms. We now
route through an instruction LLM on the HF router's OpenAI-compatible
`/{provider}/v1/chat/completions` endpoint (see `hf_client.chat_completion`),
which produces natural, idiomatic, context-aware translations and supports token
streaming for a fast-feeling UX.

Model + provider are configurable (`hf_model_translate`, `hf_translate_provider`)
so a faster/cheaper model is a one-line change. Per-language IPA pronunciation
guides still come from `epitran` (English returns None). Same-language and
unsupported-language requests raise before any HF call.
"""

import asyncio
import time
from collections import OrderedDict
from collections.abc import AsyncGenerator, AsyncIterator
from dataclasses import dataclass
from functools import lru_cache
from typing import Any, Protocol

from app.config import get_settings
from app.services.groq_client import get_groq_client
from app.services.hf_client import get_hf_client


class ChatCompletionClient(Protocol):
    """The bit of a provider client translation actually needs.

    Implemented by both :class:`HuggingFaceClient` and :class:`GroqClient`, so
    the service is provider-agnostic and `llm_provider` becomes a config flip.
    """

    async def chat_completion(
        self,
        *,
        provider: str,
        model: str,
        messages: list[dict[str, str]],
        temperature: float = ...,
        max_tokens: int = ...,
    ) -> str: ...

    def chat_completion_stream(
        self,
        *,
        provider: str,
        model: str,
        messages: list[dict[str, str]],
        temperature: float = ...,
        max_tokens: int = ...,
    ) -> AsyncIterator[str]: ...


# Cache key: (source, target, text, context)
CacheKey = tuple[str, str, str, str | None]

# Human-readable names fed to the LLM prompt. This is the single source of
# truth for which languages the app supports — the Android AppLanguage list
# and the DB language seed (scripts/seed_phrases.py) must stay in sync with it.
LANGUAGE_NAMES: dict[str, str] = {
    "en": "English",
    "hi": "Hindi",
    "te": "Telugu",
    "ta": "Tamil",
    "bn": "Bengali",
    "mr": "Marathi",
    "th": "Thai",
    "es": "Spanish",
    "fr": "French",
    "de": "German",
    "it": "Italian",
    "pt": "Portuguese",
    "ja": "Japanese",
    "ko": "Korean",
    "zh": "Chinese",
    "ar": "Arabic",
    "ru": "Russian",
    "vi": "Vietnamese",
    "id": "Indonesian",
    "tr": "Turkish",
}

SUPPORTED_LANGUAGES: frozenset[str] = frozenset(LANGUAGE_NAMES)

# epitran ISO mapping for the non-Latin-script targets whose IPA we want.
# Latin-script targets (es/fr/de/...) are readable as-is and intentionally have
# no entry (derive_pronunciation returns None). Japanese and Chinese are also
# absent: epitran can't transliterate mixed kanji/hanzi text without extra
# dictionaries, and _epitran_for degrades to None safely. Arabic is absent too:
# epitran on unvocalized Arabic yields garbled output (raw script chars mixed
# into the IPA), which is worse than no guide at all.
EPITRAN_CODES: dict[str, str] = {
    "hi": "hin-Deva",
    "te": "tel-Telu",
    "ta": "tam-Taml",
    "bn": "ben-Beng",
    "mr": "mar-Deva",
    "th": "tha-Thai",
    "ko": "kor-Hang",
    "ru": "rus-Cyrl",
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


@dataclass(frozen=True)
class StreamDelta:
    """An incremental chunk of translated text streamed to the client."""

    text: str


@dataclass(frozen=True)
class StreamDone:
    """The terminal stream event carrying the assembled result + metadata."""

    result: TranslationResult


StreamEvent = StreamDelta | StreamDone


@lru_cache(maxsize=len(EPITRAN_CODES))
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
        return str(epi.transliterate(text))
    except Exception:
        return None


def _clean_output(text: str) -> str:
    """Strip whitespace and a single layer of wrapping quotes an LLM sometimes
    adds around its answer, so the chat bubble shows clean text only."""
    cleaned = text.strip()
    # ASCII plus curly double/single quotes (built via chr() to keep source ASCII).
    quote_pairs = (
        ('"', '"'),
        ("'", "'"),
        (chr(0x201C), chr(0x201D)),
        (chr(0x2018), chr(0x2019)),
    )
    for open_q, close_q in quote_pairs:
        if len(cleaned) >= 2 and cleaned.startswith(open_q) and cleaned.endswith(close_q):
            cleaned = cleaned[1:-1].strip()
            break
    return cleaned


def build_messages(
    source: str, target: str, text: str, context: str | None
) -> list[dict[str, str]]:
    """Assemble the chat messages for an idiomatic, context-aware translation."""
    source_name = LANGUAGE_NAMES.get(source, source)
    target_name = LANGUAGE_NAMES.get(target, target)

    system = (
        f"You are a professional translator. Translate the user's message from "
        f"{source_name} to {target_name}.\n"
        "Rules:\n"
        "- Produce a natural, idiomatic translation the way a fluent native "
        f"{target_name} speaker would actually say it — never a literal, "
        "word-for-word rendering.\n"
        "- Preserve the meaning, tone, register, and level of formality of the "
        "original.\n"
        "- Use the equivalent common expression or idiom in "
        f"{target_name} when the source is idiomatic.\n"
        "- Keep proper nouns, names, numbers, URLs, and emoji intact.\n"
        "- Output ONLY the translated text. No quotes, no explanations, no "
        "notes, no romanization, and do not repeat the source text."
    )
    if context:
        system += (
            f"\n- Use this context to disambiguate meaning (do NOT translate or "
            f"repeat it): {context}"
        )

    return [
        {"role": "system", "content": system},
        {"role": "user", "content": text},
    ]


class _LRU:
    """Tiny OrderedDict-based LRU. Replaces functools.lru_cache for async values."""

    def __init__(self, maxsize: int = 1000) -> None:
        self.maxsize = maxsize
        self._data: OrderedDict[CacheKey, TranslationResult] = OrderedDict()

    def get(self, key: CacheKey) -> TranslationResult | None:
        if key not in self._data:
            return None
        self._data.move_to_end(key)
        return self._data[key]

    def put(self, key: CacheKey, value: TranslationResult) -> None:
        if key in self._data:
            self._data.move_to_end(key)
        self._data[key] = value
        if len(self._data) > self.maxsize:
            self._data.popitem(last=False)


class TranslationService:
    def __init__(
        self,
        llm: ChatCompletionClient,
        *,
        model: str,
        provider: str,
        temperature: float,
        max_tokens: int,
    ) -> None:
        self.llm = llm
        self.model = model
        self.provider = provider
        self.temperature = temperature
        self.max_tokens = max_tokens
        self._cache = _LRU(maxsize=1000)
        self._batch_concurrency = asyncio.Semaphore(5)

    def _validate(self, source: str, target: str) -> None:
        if source not in SUPPORTED_LANGUAGES:
            raise UnsupportedLanguageError(f"source language {source!r} not supported")
        if target not in SUPPORTED_LANGUAGES:
            raise UnsupportedLanguageError(f"target language {target!r} not supported")
        if source == target:
            raise SameLanguageError("source and target languages must differ")

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

        messages = build_messages(source, target, text, context)
        start = time.monotonic()
        raw = await self.llm.chat_completion(
            provider=self.provider,
            model=self.model,
            messages=messages,
            temperature=self.temperature,
            max_tokens=self.max_tokens,
        )
        translation = _clean_output(raw)
        latency_ms = int((time.monotonic() - start) * 1000)

        result = TranslationResult(
            translation=translation,
            pronunciation=derive_pronunciation(translation, target),
            latency_ms=latency_ms,
            model=self.model,
        )
        self._cache.put(cache_key, result)
        return result

    async def translate_stream(
        self,
        source: str,
        target: str,
        text: str,
        context: str | None = None,
    ) -> AsyncGenerator[StreamEvent, None]:
        """Stream the translation token-by-token, then a terminal event with
        pronunciation + metadata. A cache hit emits the whole translation as a
        single delta followed by the done event."""
        self._validate(source, target)
        cache_key = (source, target, text, context)
        cached = self._cache.get(cache_key)
        if cached is not None:
            yield StreamDelta(text=cached.translation)
            yield StreamDone(result=cached)
            return

        messages = build_messages(source, target, text, context)
        start = time.monotonic()
        parts: list[str] = []
        async for delta in self.llm.chat_completion_stream(
            provider=self.provider,
            model=self.model,
            messages=messages,
            temperature=self.temperature,
            max_tokens=self.max_tokens,
        ):
            parts.append(delta)
            yield StreamDelta(text=delta)

        translation = _clean_output("".join(parts))
        latency_ms = int((time.monotonic() - start) * 1000)
        result = TranslationResult(
            translation=translation,
            pronunciation=derive_pronunciation(translation, target),
            latency_ms=latency_ms,
            model=self.model,
        )
        self._cache.put(cache_key, result)
        yield StreamDone(result=result)

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
        settings = get_settings()
        # `resolved_llm_provider` is "groq" only when a Groq key is configured
        # (or `llm_provider=groq` is set explicitly); otherwise stay on HF.
        llm: ChatCompletionClient = (
            get_groq_client() if settings.resolved_llm_provider == "groq" else get_hf_client()
        )
        _service = TranslationService(
            llm,
            model=settings.translate_model,
            provider=settings.hf_translate_provider,
            temperature=settings.translate_temperature,
            max_tokens=settings.translate_max_tokens,
        )
    return _service


def reset_translation_service_for_tests() -> None:
    global _service
    _service = None
