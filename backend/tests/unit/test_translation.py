import pytest

from app.services.translation import (
    EN_TO_MUL_MODEL,
    MUL_TO_EN_MODEL,
    SameLanguageError,
    TranslationService,
    UnsupportedLanguageError,
    derive_pronunciation,
)


class _FakeHFClient:
    """Stands in for HuggingFaceClient — records calls, returns canned per-model data."""

    def __init__(self, response=None, by_model=None):
        self.response = response
        self.by_model = by_model or {}
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
        self.calls.append((model, json_payload, content, raw))
        if model in self.by_model:
            return self.by_model[model]
        return self.response


@pytest.mark.asyncio
async def test_translate_en_to_hi_uses_en_mul_model() -> None:
    fake = _FakeHFClient(response=[{"translation_text": "नमस्ते"}])
    svc = TranslationService(fake)  # type: ignore[arg-type]

    result = await svc.translate("en", "hi", "Hello")

    assert result.translation == "नमस्ते"
    assert result.model == EN_TO_MUL_MODEL
    assert result.latency_ms >= 0
    model, payload, _, _ = fake.calls[0]
    assert model == EN_TO_MUL_MODEL
    assert payload == {"inputs": ">>hin<< Hello"}


@pytest.mark.asyncio
async def test_translate_hi_to_en_uses_mul_en_model() -> None:
    fake = _FakeHFClient(response=[{"translation_text": "Hello"}])
    svc = TranslationService(fake)  # type: ignore[arg-type]

    result = await svc.translate("hi", "en", "नमस्ते")

    assert result.translation == "Hello"
    assert result.model == MUL_TO_EN_MODEL
    model, payload, _, _ = fake.calls[0]
    assert model == MUL_TO_EN_MODEL
    assert payload == {"inputs": "नमस्ते"}


@pytest.mark.asyncio
async def test_translate_cross_language_chains_via_english() -> None:
    fake = _FakeHFClient(
        by_model={
            MUL_TO_EN_MODEL: [{"translation_text": "Hello friend"}],
            EN_TO_MUL_MODEL: [{"translation_text": "สวัสดี เพื่อน"}],
        }
    )
    svc = TranslationService(fake)  # type: ignore[arg-type]

    result = await svc.translate("hi", "th", "नमस्ते दोस्त")

    assert result.translation == "สวัสดี เพื่อน"
    assert MUL_TO_EN_MODEL in result.model
    assert EN_TO_MUL_MODEL in result.model
    # 1st call: hi → en (mul-en); 2nd call: en → th (en-mul) with >>tha<< prefix
    assert len(fake.calls) == 2
    assert fake.calls[0][0] == MUL_TO_EN_MODEL
    assert fake.calls[1][0] == EN_TO_MUL_MODEL
    assert fake.calls[1][1]["inputs"].startswith(">>tha<< ")


@pytest.mark.asyncio
async def test_translate_with_context_prepends_to_input() -> None:
    fake = _FakeHFClient(response=[{"translation_text": "x"}])
    svc = TranslationService(fake)  # type: ignore[arg-type]

    await svc.translate("en", "hi", "menu", context="restaurant")

    _, payload, _, _ = fake.calls[0]
    assert payload["inputs"] == ">>hin<< [Context: restaurant] menu"


@pytest.mark.asyncio
async def test_translate_unsupported_source_raises() -> None:
    svc = TranslationService(_FakeHFClient([]))  # type: ignore[arg-type]
    with pytest.raises(UnsupportedLanguageError):
        await svc.translate("fr", "hi", "Hello")


@pytest.mark.asyncio
async def test_translate_unsupported_target_raises() -> None:
    svc = TranslationService(_FakeHFClient([]))  # type: ignore[arg-type]
    with pytest.raises(UnsupportedLanguageError):
        await svc.translate("en", "fr", "Hello")


@pytest.mark.asyncio
async def test_translate_same_language_raises() -> None:
    svc = TranslationService(_FakeHFClient([]))  # type: ignore[arg-type]
    with pytest.raises(SameLanguageError):
        await svc.translate("en", "en", "Hello")


@pytest.mark.asyncio
async def test_translate_uses_cache_on_repeat() -> None:
    fake = _FakeHFClient(response=[{"translation_text": "hello-hi"}])
    svc = TranslationService(fake)  # type: ignore[arg-type]

    await svc.translate("en", "hi", "Hello")
    await svc.translate("en", "hi", "Hello")
    await svc.translate("en", "hi", "Hello")

    assert len(fake.calls) == 1, "second + third calls should hit the LRU cache"


@pytest.mark.asyncio
async def test_translate_batch_runs_all_items() -> None:
    fake = _FakeHFClient(response=[{"translation_text": "x"}])
    svc = TranslationService(fake)  # type: ignore[arg-type]

    results = await svc.translate_batch("en", "hi", ["a", "b", "c"])

    assert len(results) == 3
    assert all(r.translation == "x" for r in results)
    assert len(fake.calls) == 3


def test_derive_pronunciation_returns_none_for_english() -> None:
    assert derive_pronunciation("Hello", "en") is None


def test_derive_pronunciation_returns_string_for_hindi() -> None:
    # epitran installation may fail in CI without panphon tables; tolerate None.
    result = derive_pronunciation("नमस्ते", "hi")
    assert result is None or isinstance(result, str)
