from sqlalchemy import select

from app.db.models.language import Language
from app.repositories.base import BaseRepo


class LanguageRepo(BaseRepo):
    async def list_all(self) -> list[Language]:
        result = await self.session.execute(select(Language).order_by(Language.code))
        return list(result.scalars().all())

    async def get(self, code: str) -> Language | None:
        return await self.session.get(Language, code)
