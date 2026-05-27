from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import aliased

from app.db.models.favorite import Favorite, Recent
from app.repositories.base import BaseRepo


class FavoriteRepo(BaseRepo):
    async def list_for_user(self, user_id: UUID, kind: str | None = None) -> list[Favorite]:
        stmt = select(Favorite).where(Favorite.user_id == user_id)
        if kind is not None:
            stmt = stmt.where(Favorite.kind == kind)
        stmt = stmt.order_by(Favorite.created_at.desc())
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def get_for_user(self, favorite_id: UUID, user_id: UUID) -> Favorite | None:
        result = await self.session.execute(
            select(Favorite).where(Favorite.id == favorite_id, Favorite.user_id == user_id)
        )
        return result.scalar_one_or_none()

    async def add(self, favorite: Favorite) -> Favorite:
        self.session.add(favorite)
        await self.session.flush()
        return favorite


class RecentRepo(BaseRepo):
    async def list_for_user(self, user_id: UUID, limit: int = 20) -> list[Recent]:
        """Most-recent rows, deduped by (source_text, target_lang) keeping newest.

        Uses Postgres ``DISTINCT ON`` in a subquery so we can re-order the deduped
        set by ``created_at DESC`` before slicing the response.
        """
        sub = (
            select(Recent)
            .where(Recent.user_id == user_id)
            .order_by(
                Recent.source_text,
                Recent.target_lang,
                Recent.created_at.desc(),
            )
            .distinct(Recent.source_text, Recent.target_lang)
            .subquery()
        )
        ra = aliased(Recent, sub)
        result = await self.session.execute(select(ra).order_by(ra.created_at.desc()).limit(limit))
        return list(result.scalars().all())

    async def add(self, recent: Recent) -> Recent:
        self.session.add(recent)
        await self.session.flush()
        return recent
