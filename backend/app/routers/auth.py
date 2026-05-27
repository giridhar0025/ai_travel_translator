from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.errors import AuthError
from app.auth.firebase import verify_id_token
from app.auth.tokens import issue_token
from app.db.models.user import User, UserSettings
from app.deps import get_db_session
from app.repositories.user import UserRepo, UserSettingsRepo
from app.schemas.auth import AuthResponse, VerifyTokenRequest
from app.schemas.user import UserSchema, UserSettingsSchema

router = APIRouter(prefix="/auth", tags=["auth"])


async def _ensure_settings(session: AsyncSession, user_id: UUID) -> UserSettings:
    repo = UserSettingsRepo(session)
    settings_row = await repo.get(user_id)
    if settings_row is None:
        settings_row = UserSettings(
            user_id=user_id,
            default_language="en",
            playback_speed="normal",
            offline_enabled=False,
        )
        await repo.add(settings_row)
    return settings_row


@router.post("/verify", response_model=AuthResponse)
async def verify(
    body: VerifyTokenRequest,
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> AuthResponse:
    try:
        fb_user = await verify_id_token(body.id_token)
    except AuthError as e:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=str(e)) from e

    user_repo = UserRepo(session)
    user = await user_repo.get_by_firebase_uid(fb_user.uid)
    if user is None:
        user = User(
            firebase_uid=fb_user.uid,
            email=fb_user.email,
            display_name=fb_user.name,
            photo_url=fb_user.picture,
            is_guest=False,
        )
        await user_repo.add(user)
    else:
        # Refresh profile fields on every login
        user.email = fb_user.email
        user.display_name = fb_user.name
        user.photo_url = fb_user.picture

    settings_row = await _ensure_settings(session, user.id)
    await session.commit()
    await session.refresh(user)
    await session.refresh(settings_row)

    token = issue_token(user.id, is_guest=False)
    return AuthResponse(
        user=UserSchema.model_validate(user),
        settings=UserSettingsSchema.model_validate(settings_row),
        token=token,
    )


@router.post("/guest", response_model=AuthResponse)
async def guest(
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> AuthResponse:
    user = User(is_guest=True)
    user_repo = UserRepo(session)
    await user_repo.add(user)
    settings_row = await _ensure_settings(session, user.id)
    await session.commit()
    await session.refresh(user)
    await session.refresh(settings_row)

    token = issue_token(user.id, is_guest=True)
    return AuthResponse(
        user=UserSchema.model_validate(user),
        settings=UserSettingsSchema.model_validate(settings_row),
        token=token,
    )
