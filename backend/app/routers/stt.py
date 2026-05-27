import io
from typing import Annotated

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status

from app.db.models.user import User
from app.deps import get_current_user
from app.schemas.stt import TranscriptionResponse
from app.services.stt import STTService, get_stt_service

router = APIRouter(prefix="/stt", tags=["stt"])

MAX_BYTES = 10 * 1024 * 1024  # 10 MB
MAX_DURATION_SECONDS = 30.0

# Mobile clients can record in any of these — we don't trans-encode, Whisper handles it.
ALLOWED_CONTENT_TYPES = {
    "audio/wav",
    "audio/x-wav",
    "audio/wave",
    "audio/mp4",
    "audio/m4a",
    "audio/x-m4a",
    "audio/aac",
    "audio/mpeg",
    "audio/mp3",
    "audio/flac",
    "audio/ogg",
    "audio/webm",
}


def _check_duration_if_possible(content: bytes) -> None:
    """For formats `soundfile` can parse (wav/flac/ogg), reject > 30 s.
    Other formats (m4a/mp3/aac) pass through; Whisper's 30 s cap will surface there."""
    try:
        import soundfile as sf

        info = sf.info(io.BytesIO(content))
    except Exception:
        return
    if info.duration > MAX_DURATION_SECONDS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"audio duration {info.duration:.1f}s exceeds {MAX_DURATION_SECONDS}s limit",
        )


@router.post("/transcribe", response_model=TranscriptionResponse)
async def transcribe(
    _current_user: Annotated[User, Depends(get_current_user)],
    service: Annotated[STTService, Depends(get_stt_service)],
    audio: Annotated[UploadFile, File()],
    language: Annotated[str | None, Form()] = None,
) -> TranscriptionResponse:
    if audio.content_type and audio.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail=f"unsupported audio content-type: {audio.content_type}",
        )

    content = await audio.read()
    if len(content) > MAX_BYTES:
        raise HTTPException(
            status_code=413,  # Content Too Large
            detail=f"audio exceeds {MAX_BYTES // (1024 * 1024)} MB",
        )
    if not content:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="empty audio payload",
        )

    _check_duration_if_possible(content)

    result = await service.transcribe(
        content,
        language,
        content_type=audio.content_type or "audio/wav",
    )
    return TranscriptionResponse(
        transcript=result.transcript,
        language=result.language,
        confidence=result.confidence,
        latency_ms=result.latency_ms,
        model=result.model,
    )
