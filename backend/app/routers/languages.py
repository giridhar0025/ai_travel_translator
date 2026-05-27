from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.deps import get_db_session
from app.repositories.language import LanguageRepo
from app.schemas.language import LanguageSchema

router = APIRouter(tags=["languages"])


@router.get("/languages", response_model=list[LanguageSchema])
async def list_languages(
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> list[LanguageSchema]:
    repo = LanguageRepo(session)
    rows = await repo.list_all()
    return [LanguageSchema.model_validate(r) for r in rows]
