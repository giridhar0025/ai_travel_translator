from uuid import UUID

from sqlalchemy import select

from app.db.models.user import User, UserSettings
from app.repositories.base import BaseRepo


class UserRepo(BaseRepo):
    async def get(self, user_id: UUID) -> User | None:
        return await self.session.get(User, user_id)

    async def get_by_firebase_uid(self, firebase_uid: str) -> User | None:
        result = await self.session.execute(select(User).where(User.firebase_uid == firebase_uid))
        return result.scalar_one_or_none()

    async def add(self, user: User) -> User:
        self.session.add(user)
        await self.session.flush()
        return user


class UserSettingsRepo(BaseRepo):
    async def get(self, user_id: UUID) -> UserSettings | None:
        return await self.session.get(UserSettings, user_id)

    async def add(self, settings: UserSettings) -> UserSettings:
        self.session.add(settings)
        await self.session.flush()
        return settings

    async def get_or_create(self, user_id: UUID) -> UserSettings:
        row = await self.get(user_id)
        if row is not None:
            return row
        row = UserSettings(
            user_id=user_id,
            default_language="en",
            playback_speed="normal",
            offline_enabled=False,
        )
        return await self.add(row)
