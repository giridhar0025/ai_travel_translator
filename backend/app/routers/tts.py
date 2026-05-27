from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Response, status

from app.db.models.user import User
from app.deps import get_current_user
from app.schemas.tts import SynthesizeRequest
from app.services.hf_client import HFInferenceError
from app.services.tts import (
    ClientSideTtsRequired,
    TTSService,
    UnsupportedLanguageError,
    get_tts_service,
)

router = APIRouter(prefix="/tts", tags=["tts"])


@router.post("/synthesize")
async def synthesize(
    body: SynthesizeRequest,
    _current_user: Annotated[User, Depends(get_current_user)],
    service: Annotated[TTSService, Depends(get_tts_service)],
) -> Response:
    text = body.text.strip()
    if not text:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="text must not be empty",
        )

    try:
        result = await service.synthesize(text, body.language, body.speed)
    except ClientSideTtsRequired as exc:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail={"detail": "client_side_tts_required", "language": exc.language},
        ) from exc
    except UnsupportedLanguageError as exc:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail={"detail": "unsupported_language", "language": exc.language},
        ) from exc
    except HFInferenceError as exc:
        # Upstream provider failed (Replicate/HF returned an error or timed out).
        # 502 maps the situation correctly: our service is up, but the dependency isn't.
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail={"detail": "tts_provider_failed", "message": str(exc)},
        ) from exc

    return Response(
        content=result.audio,
        media_type=result.content_type,
        headers={"X-TTS-Model": result.model},
    )
