import json
from collections.abc import AsyncIterator
from typing import Annotated

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from fastapi.responses import StreamingResponse

from app.db.models.user import User
from app.deps import get_current_user
from app.schemas.translation import (
    TranslateBatchRequest,
    TranslateBatchResponse,
    TranslateRequest,
    TranslateResultSchema,
)
from app.services.hf_client import HFInferenceError
from app.services.recents import insert_recent
from app.services.translation import (
    SameLanguageError,
    StreamDelta,
    StreamDone,
    StreamEvent,
    TranslationResult,
    TranslationService,
    UnsupportedLanguageError,
    get_translation_service,
)

router = APIRouter(prefix="/translate", tags=["translate"])


def _bad_request(detail: str) -> HTTPException:
    return HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=detail)


def _sse(payload: dict[str, object]) -> str:
    """Format one Server-Sent Events data frame (JSON, non-ASCII preserved)."""
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


def _format_event(event: StreamEvent) -> str:
    if isinstance(event, StreamDelta):
        return _sse({"delta": event.text})
    return _sse(
        {
            "done": True,
            "translation": event.result.translation,
            "pronunciation": event.result.pronunciation,
            "latency_ms": event.result.latency_ms,
            "model": event.result.model,
        }
    )


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


@router.post("/stream")
async def translate_stream(
    body: TranslateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    service: Annotated[TranslationService, Depends(get_translation_service)],
) -> StreamingResponse:
    """Stream a translation token-by-token as Server-Sent Events.

    Emits `{"delta": "..."}` frames as the LLM generates, then a terminal
    `{"done": true, ...}` frame carrying the assembled translation,
    pronunciation, latency and model. Validation errors surface as HTTP 400
    before the stream body starts; a cache hit emits the whole translation in a
    single delta. The recent is recorded once the stream completes.
    """
    events = service.translate_stream(body.source, body.target, body.text, body.context)

    # Pull the first event eagerly so language-validation errors become a real
    # 400 instead of a mid-stream error frame.
    try:
        first_event: StreamEvent | None = await events.__anext__()
    except (UnsupportedLanguageError, SameLanguageError) as e:
        await events.aclose()
        raise _bad_request(str(e)) from e
    except StopAsyncIteration:
        first_event = None

    async def event_source() -> AsyncIterator[str]:
        final_result: TranslationResult | None = None
        try:
            if first_event is not None:
                yield _format_event(first_event)
                if isinstance(first_event, StreamDone):
                    final_result = first_event.result
            async for event in events:
                yield _format_event(event)
                if isinstance(event, StreamDone):
                    final_result = event.result
        except HFInferenceError as e:
            yield _sse({"error": str(e)})
            return

        if final_result is not None:
            await insert_recent(
                current_user.id,
                body.source,
                body.target,
                body.text,
                final_result.translation,
            )

    return StreamingResponse(
        event_source(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
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
