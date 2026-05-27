from typing import Annotated

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status

from app.db.models.user import User
from app.deps import get_current_user
from app.schemas.translation import (
    TranslateBatchRequest,
    TranslateBatchResponse,
    TranslateRequest,
    TranslateResultSchema,
)
from app.services.recents import insert_recent
from app.services.translation import (
    SameLanguageError,
    TranslationService,
    UnsupportedLanguageError,
    get_translation_service,
)

router = APIRouter(prefix="/translate", tags=["translate"])


def _bad_request(detail: str) -> HTTPException:
    return HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=detail)


@router.post("", response_model=TranslateResultSchema)
async def translate(
    body: TranslateRequest,
    background_tasks: BackgroundTasks,
    current_user: Annotated[User, Depends(get_current_user)],
    service: Annotated[TranslationService, Depends(get_translation_service)],
) -> TranslateResultSchema:
    try:
        result = await service.translate(body.source, body.target, body.text, body.context)
    except (UnsupportedLanguageError, SameLanguageError) as e:
        raise _bad_request(str(e)) from e

    background_tasks.add_task(
        insert_recent,
        current_user.id,
        body.source,
        body.target,
        body.text,
        result.translation,
    )
    return TranslateResultSchema(
        translation=result.translation,
        pronunciation=result.pronunciation,
        latency_ms=result.latency_ms,
        model=result.model,
    )


@router.post("/batch", response_model=TranslateBatchResponse)
async def translate_batch(
    body: TranslateBatchRequest,
    _current_user: Annotated[User, Depends(get_current_user)],
    service: Annotated[TranslationService, Depends(get_translation_service)],
) -> TranslateBatchResponse:
    try:
        results = await service.translate_batch(body.source, body.target, body.items)
    except (UnsupportedLanguageError, SameLanguageError) as e:
        raise _bad_request(str(e)) from e

    return TranslateBatchResponse(
        results=[
            TranslateResultSchema(
                translation=r.translation,
                pronunciation=r.pronunciation,
                latency_ms=r.latency_ms,
                model=r.model,
            )
            for r in results
        ]
    )
