from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.favorite import Favorite
from app.db.models.user import User
from app.deps import get_current_user, get_db_session
from app.repositories.favorite import FavoriteRepo
from app.repositories.language import LanguageRepo
from app.schemas.favorite import FavoriteCreate, FavoriteKind, FavoriteSchema

router = APIRouter(tags=["favorites"])


@router.get("/favorites", response_model=list[FavoriteSchema])
async def list_favorites(
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
    kind: Annotated[FavoriteKind | None, Query()] = None,
) -> list[FavoriteSchema]:
    repo = FavoriteRepo(session)
    rows = await repo.list_for_user(current_user.id, kind=kind)
    return [FavoriteSchema.model_validate(r) for r in rows]


@router.post("/favorites", response_model=FavoriteSchema, status_code=status.HTTP_201_CREATED)
async def create_favorite(
    body: FavoriteCreate,
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> FavoriteSchema:
    lang_repo = LanguageRepo(session)
    for code in (body.source_lang, body.target_lang):
        if await lang_repo.get(code) is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"unsupported language: {code}",
            )

    fav = Favorite(
        user_id=current_user.id,
        kind=body.kind,
        ref_id=body.ref_id,
        source_lang=body.source_lang,
        target_lang=body.target_lang,
    )
    session.add(fav)
    try:
        await session.commit()
    except IntegrityError as exc:
        await session.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail="already favorited"
        ) from exc
    await session.refresh(fav)
    return FavoriteSchema.model_validate(fav)


@router.delete("/favorites/{favorite_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_favorite(
    favorite_id: UUID,
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> Response:
    repo = FavoriteRepo(session)
    row = await repo.get_for_user(favorite_id, current_user.id)
    if row is None:
        raise HTTPException(status_code=404, detail="favorite not found")
    await session.delete(row)
    await session.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)
