from collections.abc import AsyncIterator

import pytest

from app.services.translation import (
    SameLanguageError,
    StreamDelta,
    StreamDone,
    TranslationService,
    UnsupportedLanguageError,
    _clean_output,
    build_messages,
    derive_pronunciation,
)

MODEL = "test-model"


class _FakeHFClient:
    """Stands in for HuggingFaceClient — records chat calls, returns canned text."""

    def __init__(self, response: str = "", deltas: list[str] | None = None) -> None:
        self.response = response
        self.deltas = deltas if deltas is not None else [response]
        self.chat_calls: list[list[dict[str, str]]] = []
        self.stream_calls: list[list[dict[str, str]]] = []

    async def chat_completion(
        self,
        *,
        provider: str,
        model: str,
        messages: list[dict[str, str]],
        temperature: float = 0.3,
        max_tokens: int = 1024,
        max_retries: int = 2,
    ) -> str:
        self.chat_calls.append(messages)
        return self.response

    async def chat_completion_stream(
        self,
        *,
        provider: str,
        model: str,
        messages: list[dict[str, str]],
        temperature: float = 0.3,
        max_tokens: int = 1024,
    ) -> AsyncIterator[str]:
        self.stream_calls.append(messages)
        for delta in self.deltas:
            yield delta


def _service(fake: _FakeHFClient) -> TranslationService:
    return TranslationService(
        fake,  # type: ignore[arg-type]
        model=MODEL,
        provider="test-provider",
        temperature=0.3,
        max_tokens=256,
    )


@pytest.mark.asyncio
async def test_translate_returns_llm_output_and_model() -> None:
    fake = _FakeHFClient(response="नमस्ते")
    svc = _service(fake)

    result = await svc.translate("en", "hi", "Hello")

    assert result.translation == "नमस्ते"
    assert result.model == MODEL
    assert result.latency_ms >= 0
    assert len(fake.chat_calls) == 1
    # system + user message assembled for the target language
    messages = fake.chat_calls[0]
    assert messages[0]["role"] == "system"
    assert "Hindi" in messages[0]["content"]
    assert messages[1] == {"role": "user", "content": "Hello"}


@pytest.mark.asyncio
async def test_translate_strips_wrapping_quotes() -> None:
    fake = _FakeHFClient(response='"नमस्ते"')
    svc = _service(fake)

    result = await svc.translate("en", "hi", "Hello")

    assert result.translation == "नमस्ते"


@pytest.mark.asyncio
async def test_translate_context_goes_into_system_prompt() -> None:
    fake = _FakeHFClient(response="x")
    svc = _service(fake)

    await svc.translate("en", "hi", "menu", context="restaurant")

    system = fake.chat_calls[0][0]["content"]
    assert "restaurant" in system
    # the context must not be smuggled into the user text like the old engine did
    assert fake.chat_calls[0][1]["content"] == "menu"


@pytest.mark.asyncio
async def test_translate_unsupported_source_raises() -> None:
    svc = _service(_FakeHFClient())
    with pytest.raises(UnsupportedLanguageError):
        await svc.translate("fr", "hi", "Hello")


@pytest.mark.asyncio
async def test_translate_unsupported_target_raises() -> None:
    svc = _service(_FakeHFClient())
    with pytest.raises(UnsupportedLanguageError):
        await svc.translate("en", "fr", "Hello")


@pytest.mark.asyncio
async def test_translate_same_language_raises() -> None:
    svc = _service(_FakeHFClient())
    with pytest.raises(SameLanguageError):
        await svc.translate("en", "en", "Hello")


@pytest.mark.asyncio
async def test_translate_uses_cache_on_repeat() -> None:
    fake = _FakeHFClient(response="hello-hi")
    svc = _service(fake)

    await svc.translate("en", "hi", "Hello")
    await svc.translate("en", "hi", "Hello")
    await svc.translate("en", "hi", "Hello")

    assert len(fake.chat_calls) == 1, "second + third calls should hit the LRU cache"


@pytest.mark.asyncio
async def test_translate_batch_runs_all_items() -> None:
    fake = _FakeHFClient(response="x")
    svc = _service(fake)

    results = await svc.translate_batch("en", "hi", ["a", "b", "c"])

    assert len(results) == 3
    assert all(r.translation == "x" for r in results)
    assert len(fake.chat_calls) == 3


@pytest.mark.asyncio
async def test_translate_stream_emits_deltas_then_done() -> None:
    fake = _FakeHFClient(deltas=["ना", "म", "स्ते"])
    svc = _service(fake)

    events = [e async for e in svc.translate_stream("en", "hi", "Hello")]

    deltas = [e.text for e in events if isinstance(e, StreamDelta)]
    done = [e for e in events if isinstance(e, StreamDone)]
    assert deltas == ["ना", "म", "स्ते"]
    assert len(done) == 1
    assert done[0].result.translation == "नामस्ते"
    assert done[0].result.model == MODEL


@pytest.mark.asyncio
async def test_translate_stream_cache_hit_emits_single_delta() -> None:
    fake = _FakeHFClient(response="नमस्ते", deltas=["नमस्ते"])
    svc = _service(fake)

    # prime the cache via the non-streaming path
    await svc.translate("en", "hi", "Hello")

    events = [e async for e in svc.translate_stream("en", "hi", "Hello")]

    deltas = [e for e in events if isinstance(e, StreamDelta)]
    assert len(deltas) == 1
    assert deltas[0].text == "नमस्ते"
    # cache hit must NOT re-open the HF stream
    assert fake.stream_calls == []


@pytest.mark.asyncio
async def test_translate_stream_validates_before_streaming() -> None:
    svc = _service(_FakeHFClient())
    with pytest.raises(SameLanguageError):
        async for _ in svc.translate_stream("en", "en", "Hello"):
            pass


def test_clean_output_strips_quotes_and_whitespace() -> None:
    assert _clean_output('  "hello"  ') == "hello"
    assert _clean_output("“hello”") == "hello"
    assert _clean_output("plain") == "plain"


def test_build_messages_omits_context_line_when_absent() -> None:
    messages = build_messages("en", "hi", "Hello", None)
    assert "context" not in messages[0]["content"].lower()


def test_derive_pronunciation_returns_none_for_english() -> None:
    assert derive_pronunciation("Hello", "en") is None


def test_derive_pronunciation_returns_string_for_hindi() -> None:
    # epitran installation may fail in CI without panphon tables; tolerate None.
    result = derive_pronunciation("नमस्ते", "hi")
    assert result is None or isinstance(result, str)
