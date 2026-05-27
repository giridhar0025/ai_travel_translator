from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.deps import get_db_session
from app.repositories.language import LanguageRepo
from app.repositories.phrase import PhraseRepo
from app.schemas.phrase import PhraseCategorySchema, PhraseWithTranslation
from app.services.phrases import PhrasesService, UnknownCategoryError
from app.services.translation import (
    SameLanguageError,
    UnsupportedLanguageError,
    get_translation_service,
)

router = APIRouter(tags=["phrases"])


@router.get("/phrases/categories", response_model=list[PhraseCategorySchema])
async def list_categories(
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> list[PhraseCategorySchema]:
    repo = PhraseRepo(session)
    rows = await repo.list_categories()
    return [PhraseCategorySchema.model_validate(r) for r in rows]


@router.get("/phrases", response_model=list[PhraseWithTranslation])
async def list_phrases(
    session: Annotated[AsyncSession, Depends(get_db_session)],
    category: Annotated[str, Query(min_length=1, max_length=64)],
    target: Annotated[str, Query(min_length=2, max_length=8)],
) -> list[PhraseWithTranslation]:
    # Reject unsupported targets up-front so we don't burn an HF call only to
    # discover the language isn't in the registry.
    lang_repo = LanguageRepo(session)
    if await lang_repo.get(target) is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"unsupported target language: {target}",
        )

    repo = PhraseRepo(session)
    service = PhrasesService(repo, get_translation_service())
    try:
        rendered = await service.get_phrases(category, target)
    except UnknownCategoryError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except (UnsupportedLanguageError, SameLanguageError) as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    # We just wrote new translations (if any) within the same session — commit
    # so subsequent requests hit the cache.
    await session.commit()

    return [
        PhraseWithTranslation(
            id=r.phrase.id,
            category_slug=r.phrase.category_slug,
            source_lang=r.phrase.source_lang,
            source_text=r.phrase.source_text,
            target_lang=r.target_lang,
            translation=r.translation,
            pronunciation=r.pronunciation,
            sort_order=r.phrase.sort_order,
        )
        for r in rendered
    ]
