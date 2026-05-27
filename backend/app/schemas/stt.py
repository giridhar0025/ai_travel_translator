from pydantic import BaseModel


class TranscriptionResponse(BaseModel):
    transcript: str
    language: str | None
    confidence: float | None
    latency_ms: int
    model: str
