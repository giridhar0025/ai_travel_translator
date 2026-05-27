from collections.abc import Iterable
from dataclasses import dataclass
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import selectinload

from app.db.models.phrase import Phrase, PhraseCategory, PhraseTranslation
from app.repositories.base import BaseRepo


@dataclass(frozen=True)
class TranslationRow:
    """Row payload for bulk_upsert_translations — kept narrow on purpose."""

    phrase_id: UUID
    translation: str
    pronunciation: str | None


class PhraseRepo(BaseRepo):
    async def list_categories(self) -> list[PhraseCategory]:
        result = await self.session.execute(
            select(PhraseCategory).order_by(PhraseCategory.sort_order)
        )
        return list(result.scalars().all())

    async def get_category(self, slug: str) -> PhraseCategory | None:
        return await self.session.get(PhraseCategory, slug)

    async def list_by_category(self, slug: str) -> list[Phrase]:
        result = await self.session.execute(
            select(Phrase)
            .where(Phrase.category_slug == slug)
            .order_by(Phrase.sort_order)
            .options(selectinload(Phrase.translations))
        )
        return list(result.scalars().all())

    async def get_translation(self, phrase_id: UUID, target_lang: str) -> PhraseTranslation | None:
        result = await self.session.execute(
            select(PhraseTranslation).where(
                PhraseTranslation.phrase_id == phrase_id,
                PhraseTranslation.target_lang == target_lang,
            )
        )
        return result.scalar_one_or_none()

    async def list_translations_for_phrases(
        self, phrase_ids: Iterable[UUID], target_lang: str
    ) -> dict[UUID, PhraseTranslation]:
        ids = list(phrase_ids)
        if not ids:
            return {}
        result = await self.session.execute(
            select(PhraseTranslation).where(
                PhraseTranslation.phrase_id.in_(ids),
                PhraseTranslation.target_lang == target_lang,
            )
        )
        return {row.phrase_id: row for row in result.scalars().all()}

    async def bulk_upsert_translations(self, target_lang: str, rows: list[TranslationRow]) -> None:
        """Upsert via the unique `(phrase_id, target_lang)` constraint —
        concurrent first-callers won't deadlock or duplicate rows."""
        if not rows:
            return
        payload = [
            {
                "phrase_id": r.phrase_id,
                "target_lang": target_lang,
                "translation": r.translation,
                "pronunciation": r.pronunciation,
            }
            for r in rows
        ]
        stmt = pg_insert(PhraseTranslation).values(payload)
        stmt = stmt.on_conflict_do_update(
            constraint="uq_phrase_translations_phrase_target",
            set_={
                "translation": stmt.excluded.translation,
                "pronunciation": stmt.excluded.pronunciation,
            },
        )
        await self.session.execute(stmt)
