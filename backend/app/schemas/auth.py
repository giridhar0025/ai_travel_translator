from pydantic import BaseModel, Field

from app.schemas.user import UserSchema, UserSettingsSchema


class VerifyTokenRequest(BaseModel):
    id_token: str = Field(..., min_length=1)


class AuthResponse(BaseModel):
    user: UserSchema
    settings: UserSettingsSchema
    token: str
