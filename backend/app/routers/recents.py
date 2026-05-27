from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.user import User
from app.deps import get_current_user, get_db_session
from app.repositories.favorite import RecentRepo
from app.schemas.favorite import RecentSchema

router = APIRouter(tags=["recents"])

MAX_LIMIT = 100


@router.get("/recents", response_model=list[RecentSchema])
async def list_recents(
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
    limit: Annotated[int, Query(ge=1, le=MAX_LIMIT)] = 20,
) -> list[RecentSchema]:
    repo = RecentRepo(session)
    rows = await repo.list_for_user(current_user.id, limit=limit)
    return [RecentSchema.model_validate(r) for r in rows]
