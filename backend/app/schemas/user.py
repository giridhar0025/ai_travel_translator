from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

PlaybackSpeed = Literal["normal", "slow", "very_slow"]


class UserSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    firebase_uid: str | None
    email: str | None
    display_name: str | None
    photo_url: str | None
    is_guest: bool
    timezone: str
    created_at: datetime


class UserSettingsSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    default_language: str
    playback_speed: str
    offline_enabled: bool


class UserSettingsPatch(BaseModel):
    default_language: str | None = Field(default=None, min_length=2, max_length=8)
    playback_speed: PlaybackSpeed | None = None
    offline_enabled: bool | None = None


class MeResponse(BaseModel):
    user: UserSchema
    settings: UserSettingsSchema | None
