from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

FavoriteKind = Literal["phrase", "translation"]


class FavoriteSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    user_id: UUID
    kind: str
    ref_id: UUID
    source_lang: str
    target_lang: str
    created_at: datetime


class FavoriteCreate(BaseModel):
    kind: FavoriteKind
    ref_id: UUID
    source_lang: str = Field(min_length=2, max_length=8)
    target_lang: str = Field(min_length=2, max_length=8)


class RecentSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    source_text: str
    source_lang: str
    translated_text: str
    target_lang: str
    created_at: datetime
