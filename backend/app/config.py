from functools import lru_cache

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    # App
    app_env: str = "development"
    app_port: int = 8000
    app_jwt_secret: str = "change-me"
    app_jwt_expiry_hours: int = 720

    # Database
    database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5436/translator"

    @field_validator("database_url", mode="before")
    @classmethod
    def _normalize_db_url(cls, v: str) -> str:
        # Railway's Postgres plugin emits `postgres://...`; SQLAlchemy's async
        # engine needs the explicit `postgresql+asyncpg://` driver. Normalize
        # both `postgres://` and the bare `postgresql://` so deploys work
        # without anyone hand-editing the env var.
        if isinstance(v, str):
            if v.startswith("postgres://"):
                return "postgresql+asyncpg://" + v.removeprefix("postgres://")
            if v.startswith("postgresql://") and "+" not in v.split("://", 1)[0]:
                return "postgresql+asyncpg://" + v.removeprefix("postgresql://")
        return v

    # Firebase
    firebase_project_id: str = ""
    firebase_service_account_json: str = ""  # base64-encoded JSON

    # Hugging Face
    hf_api_token: str = ""
    hf_timeout_seconds: float = 90.0
    hf_model_translate: str = "facebook/m2m100_418M"
    hf_model_stt: str = "openai/whisper-large-v3"
    # STT runs through this HF Inference provider's OpenAI-compatible
    # /v1/audio/transcriptions endpoint. The default `hf-inference` provider for
    # Whisper hangs/500s, so we route to `together`, which serves whisper-large-v3
    # reliably (sub-second).
    hf_stt_provider: str = "together"
    # TTS runs via Replicate (HF router) on chatterbox-turbo — see
    # app/services/tts.py. Telugu and Thai have no server-side TTS;
    # the mobile clients fall back to their on-device engines.
    replicate_tts_version: str = "resemble-ai/chatterbox-turbo"

    # CORS — comma-separated list
    cors_allowed_origins: str = ""

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_allowed_origins.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
