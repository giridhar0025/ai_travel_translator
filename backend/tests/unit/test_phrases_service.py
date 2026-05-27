from dataclasses import dataclass
from uuid import UUID, uuid4

import pytest

from app.repositories.phrase import TranslationRow
from app.services.phrases import PhrasesService, UnknownCategoryError
from app.services.translation import TranslationResult


@dataclass
class _Phrase:
    id: UUID
    category_slug: str
    source_lang: str
    source_text: str
    sort_order: int


class _Category:
    def __init__(self, slug: str) -> None:
        self.slug = slug


class _FakeRepo:
    def __init__(
        self,
        *,
        categories: list[_Category] | None = None,
        phrases: dict[str, list[_Phrase]] | None = None,
        cached: dict[tuple[UUID, str], object] | None = None,
    ):
        self._categories = {c.slug: c for c in (categories or [])}
        self._phrases = phrases or {}
        self._cached = cached or {}
        self.upserts: list[tuple[str, list[TranslationRow]]] = []

    async def get_category(self, slug: str):
        return self._categories.get(slug)

    async def list_by_category(self, slug: str):
        return list(self._phrases.get(slug, []))

    async def list_translations_for_phrases(self, phrase_ids, target_lang: str):
        return {
            pid: self._cached[(pid, target_lang)]
            for pid in phrase_ids
            if (pid, target_lang) in self._cached
        }

    async def bulk_upsert_translations(self, target_lang: str, rows: list[TranslationRow]):
        self.upserts.append((target_lang, list(rows)))


class _FakeTranslationService:
    def __init__(self, results: list[TranslationResult]) -> None:
        self.results = results
        self.calls: list[tuple[str, str, list[str]]] = []

    async def translate_batch(self, source: str, target: str, items: list[str]):
        self.calls.append((source, target, list(items)))
        return self.results[: len(items)]


@pytest.mark.asyncio
async def test_unknown_category_raises() -> None:
    repo = _FakeRepo()
    svc = PhrasesService(repo, _FakeTranslationService([]))  # type: ignore[arg-type]
    with pytest.raises(UnknownCategoryError):
        await svc.get_phrases("ghost", "hi")


@pytest.mark.asyncio
async def test_same_language_short_circuits_hf() -> None:
    """When target == source (en), the service must skip the HF batch and
    return source_text as the translation."""
    pid1, pid2 = uuid4(), uuid4()
    repo = _FakeRepo(
        categories=[_Category("restaurants")],
        phrases={
            "restaurants": [
                _Phrase(pid1, "restaurants", "en", "Hello", 1),
                _Phrase(pid2, "restaurants", "en", "Thanks", 2),
            ]
        },
    )
    tr = _FakeTranslationService([])
    svc = PhrasesService(repo, tr)  # type: ignore[arg-type]

    rendered = await svc.get_phrases("restaurants", "en")

    assert [r.translation for r in rendered] == ["Hello", "Thanks"]
    assert tr.calls == []  # no HF round-trip
    assert repo.upserts == []  # no cache writes


@pytest.mark.asyncio
async def test_cache_miss_triggers_batch_and_persists() -> None:
    pid1, pid2 = uuid4(), uuid4()
    repo = _FakeRepo(
        categories=[_Category("restaurants")],
        phrases={
            "restaurants": [
                _Phrase(pid1, "restaurants", "en", "Hello", 1),
                _Phrase(pid2, "restaurants", "en", "Thanks", 2),
            ]
        },
        cached={},  # cold cache
    )
    tr = _FakeTranslationService(
        [
            TranslationResult("नमस्ते", "n1", latency_ms=10, model="opus"),
            TranslationResult("धन्यवाद", "n2", latency_ms=10, model="opus"),
        ]
    )
    svc = PhrasesService(repo, tr)  # type: ignore[arg-type]

    rendered = await svc.get_phrases("restaurants", "hi")

    assert [r.translation for r in rendered] == ["नमस्ते", "धन्यवाद"]
    assert tr.calls == [("en", "hi", ["Hello", "Thanks"])]
    assert len(repo.upserts) == 1
    target, rows = repo.upserts[0]
    assert target == "hi"
    assert {r.phrase_id for r in rows} == {pid1, pid2}


@pytest.mark.asyncio
async def test_cache_hit_skips_hf() -> None:
    pid1, pid2 = uuid4(), uuid4()
    cached_row = type(
        "Row",
        (),
        {"phrase_id": pid1, "target_lang": "hi", "translation": "नमस्ते", "pronunciation": "n"},
    )()
    cached_row_2 = type(
        "Row",
        (),
        {"phrase_id": pid2, "target_lang": "hi", "translation": "धन्यवाद", "pronunciation": "d"},
    )()
    repo = _FakeRepo(
        categories=[_Category("restaurants")],
        phrases={
            "restaurants": [
                _Phrase(pid1, "restaurants", "en", "Hello", 1),
                _Phrase(pid2, "restaurants", "en", "Thanks", 2),
            ]
        },
        cached={(pid1, "hi"): cached_row, (pid2, "hi"): cached_row_2},
    )
    tr = _FakeTranslationService([])
    svc = PhrasesService(repo, tr)  # type: ignore[arg-type]

    rendered = await svc.get_phrases("restaurants", "hi")

    assert [r.translation for r in rendered] == ["नमस्ते", "धन्यवाद"]
    assert tr.calls == []  # cache hit — no HF
    assert repo.upserts == []


@pytest.mark.asyncio
async def test_partial_cache_only_translates_misses() -> None:
    pid1, pid2 = uuid4(), uuid4()
    cached_row = type(
        "Row",
        (),
        {"phrase_id": pid1, "target_lang": "hi", "translation": "नमस्ते", "pronunciation": "n"},
    )()
    repo = _FakeRepo(
        categories=[_Category("restaurants")],
        phrases={
            "restaurants": [
                _Phrase(pid1, "restaurants", "en", "Hello", 1),
                _Phrase(pid2, "restaurants", "en", "Thanks", 2),
            ]
        },
        cached={(pid1, "hi"): cached_row},
    )
    tr = _FakeTranslationService([TranslationResult("धन्यवाद", "d", latency_ms=10, model="opus")])
    svc = PhrasesService(repo, tr)  # type: ignore[arg-type]

    rendered = await svc.get_phrases("restaurants", "hi")

    # The cached one stays cached; only the missing one was translated.
    assert [r.translation for r in rendered] == ["नमस्ते", "धन्यवाद"]
    assert tr.calls == [("en", "hi", ["Thanks"])]
    # Only the new row is upserted.
    assert len(repo.upserts) == 1
    _, rows = repo.upserts[0]
    assert [r.phrase_id for r in rows] == [pid2]
