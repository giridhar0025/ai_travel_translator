"""Quick-phrases service.

Renders the 5 categories x 6 seeded phrases pre-translated to the requested
target. Translations are cached in `phrase_translations` (one row per
`(phrase_id, target_lang)`) and lazily filled on first request.

The category's phrases all share `source_lang == "en"` per the seed script;
the service short-circuits `target_lang == "en"` to avoid a no-op HF round-trip.
"""

import logging
from dataclasses import dataclass

from app.db.models.phrase import Phrase
from app.repositories.phrase import PhraseRepo, TranslationRow
from app.services.translation import TranslationService, derive_pronunciation

logger = logging.getLogger(__name__)


class UnknownCategoryError(LookupError):
    def __init__(self, slug: str) -> None:
        super().__init__(f"unknown phrase category: {slug}")
        self.slug = slug


@dataclass(frozen=True)
class RenderedPhrase:
    """In-memory shape the router converts to `PhraseWithTranslation`. Kept
    separate so the service doesn't depend on Pydantic schemas."""

    phrase: Phrase
    target_lang: str
    translation: str
    pronunciation: str | None


class PhrasesService:
    def __init__(self, repo: PhraseRepo, translation_service: TranslationService) -> None:
        self.repo = repo
        self.translation_service = translation_service

    async def list_categories(self):
        return await self.repo.list_categories()

    async def get_phrases(self, category_slug: str, target_lang: str) -> list[RenderedPhrase]:
        category = await self.repo.get_category(category_slug)
        if category is None:
            raise UnknownCategoryError(category_slug)

        phrases = await self.repo.list_by_category(category_slug)
        if not phrases:
            return []

        # Same-language path — no HF call, no cache write.
        if all(p.source_lang == target_lang for p in phrases):
            return [
                RenderedPhrase(
                    phrase=p,
                    target_lang=target_lang,
                    translation=p.source_text,
                    pronunciation=derive_pronunciation(p.source_text, target_lang),
                )
                for p in phrases
            ]

        cached = await self.repo.list_translations_for_phrases([p.id for p in phrases], target_lang)

        misses = [p for p in phrases if p.id not in cached]
        new_rows: list[TranslationRow] = []
        new_translations: dict[str, tuple[str, str | None]] = {}

        if misses:
            # All seeded phrases are source_lang=en today; if a category ever
            # mixes source languages we'd need to bucket by `source_lang` here.
            source = misses[0].source_lang
            results = await self.translation_service.translate_batch(
                source=source,
                target=target_lang,
                items=[p.source_text for p in misses],
            )
            for phrase, result in zip(misses, results, strict=True):
                new_translations[phrase.source_text] = (result.translation, result.pronunciation)
                new_rows.append(
                    TranslationRow(
                        phrase_id=phrase.id,
                        translation=result.translation,
                        pronunciation=result.pronunciation,
                    )
                )

            await self.repo.bulk_upsert_translations(target_lang, new_rows)

        rendered: list[RenderedPhrase] = []
        for p in phrases:
            if p.id in cached:
                t = cached[p.id]
                rendered.append(
                    RenderedPhrase(
                        phrase=p,
                        target_lang=target_lang,
                        translation=t.translation,
                        pronunciation=t.pronunciation,
                    )
                )
            else:
                translation, pronunciation = new_translations[p.source_text]
                rendered.append(
                    RenderedPhrase(
                        phrase=p,
                        target_lang=target_lang,
                        translation=translation,
                        pronunciation=pronunciation,
                    )
                )
        return rendered
