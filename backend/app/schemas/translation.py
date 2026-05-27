from pydantic import BaseModel, Field


class TranslateRequest(BaseModel):
    source: str = Field(..., min_length=2, max_length=8)
    target: str = Field(..., min_length=2, max_length=8)
    text: str = Field(..., min_length=1, max_length=4000)
    context: str | None = Field(None, max_length=200)


class TranslateBatchRequest(BaseModel):
    source: str = Field(..., min_length=2, max_length=8)
    target: str = Field(..., min_length=2, max_length=8)
    items: list[str] = Field(..., min_length=1, max_length=50)


class TranslateResultSchema(BaseModel):
    translation: str
    pronunciation: str | None
    latency_ms: int
    model: str


class TranslateBatchResponse(BaseModel):
    results: list[TranslateResultSchema]
