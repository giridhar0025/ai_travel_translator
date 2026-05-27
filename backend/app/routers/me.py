from typing import Annotated

from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.user import User
from app.deps import get_current_user, get_db_session
from app.repositories.user import UserSettingsRepo
from app.schemas.user import MeResponse, UserSchema, UserSettingsSchema

router = APIRouter(tags=["me"])


@router.get("/me", response_model=MeResponse)
async def get_me(
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> MeResponse:
    settings_repo = UserSettingsRepo(session)
    settings_row = await settings_repo.get(current_user.id)
    return MeResponse(
        user=UserSchema.model_validate(current_user),
        settings=(UserSettingsSchema.model_validate(settings_row) if settings_row else None),
    )


@router.delete("/me", status_code=status.HTTP_204_NO_CONTENT)
async def delete_me(
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> Response:
    # ON DELETE CASCADE on every FK from users.* removes related rows in the same transaction.
    await session.delete(current_user)
    await session.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)
