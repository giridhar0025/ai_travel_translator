from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.user import User
from app.deps import get_current_user, get_db_session
from app.repositories.language import LanguageRepo
from app.repositories.user import UserSettingsRepo
from app.schemas.user import UserSettingsPatch, UserSettingsSchema

router = APIRouter(tags=["settings"])


@router.get("/settings", response_model=UserSettingsSchema)
async def get_settings(
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> UserSettingsSchema:
    repo = UserSettingsRepo(session)
    row = await repo.get_or_create(current_user.id)
    await session.commit()
    await session.refresh(row)
    return UserSettingsSchema.model_validate(row)


@router.patch("/settings", response_model=UserSettingsSchema)
async def patch_settings(
    body: UserSettingsPatch,
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> UserSettingsSchema:
    repo = UserSettingsRepo(session)
    row = await repo.get_or_create(current_user.id)

    if body.default_language is not None:
        lang = await LanguageRepo(session).get(body.default_language)
        if lang is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"unsupported language: {body.default_language}",
            )
        row.default_language = body.default_language
    if body.playback_speed is not None:
        row.playback_speed = body.playback_speed
    if body.offline_enabled is not None:
        row.offline_enabled = body.offline_enabled

    await session.commit()
    await session.refresh(row)
    return UserSettingsSchema.model_validate(row)
