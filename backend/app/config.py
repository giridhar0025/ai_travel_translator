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
    # Translation now runs through an instruction-following LLM on the HF router's
    # OpenAI-compatible chat-completions endpoint (idiomatic, context-aware,
    # streaming). "auto" uses the router's unified endpoint, which auto-selects an
    # available (typically fast) provider for the given HF model id. Pin a concrete
    # provider (e.g. "groq") only with that provider's own model naming.
    hf_model_translate: str = "meta-llama/Llama-3.3-70B-Instruct"
    hf_translate_provider: str = "auto"
    translate_temperature: float = 0.3
    translate_max_tokens: int = 1024
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

    # Groq — OpenAI-compatible chat completions used for translation.
    #
    # The HF Inference router bills per-account credits and returns 402 once the
    # account is out. Groq serves the same Llama family on a free tier, so it is
    # the preferred translation backend when a key is present. `llm_provider`
    # accepts "hf" / "groq" / "auto"; "auto" picks Groq when a key is configured
    # and falls back to the HF router otherwise.
    llm_provider: str = "auto"
    groq_api_key: str = ""
    groq_base_url: str = "https://api.groq.com/openai/v1"
    groq_model_translate: str = "openai/gpt-oss-120b"
    groq_model_stt: str = "whisper-large-v3-turbo"
    groq_timeout_seconds: float = 60.0

    # STT provider selection. HF credits can run out independently of the chat
    # backend, so auto prefers Groq's free Whisper tier when a Groq key exists.
    stt_provider: str = "auto"

    # CORS — comma-separated list
    cors_allowed_origins: str = ""

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_allowed_origins.split(",") if o.strip()]

    @property
    def resolved_llm_provider(self) -> str:
        """Which chat-completions backend translation should use.

        Explicit `llm_provider` wins. Under "auto", a configured Groq key
        selects Groq (free tier, no HF credit dependency); otherwise the HF
        router.
        """
        choice = self.llm_provider.strip().lower()
        if choice in {"hf", "groq"}:
            return choice
        return "groq" if self.groq_api_key.strip() else "hf"

    @property
    def resolved_stt_provider(self) -> str:
        choice = self.stt_provider.strip().lower()
        if choice in {"hf", "groq"}:
            return choice
        return "groq" if self.groq_api_key.strip() else "hf"

    @property
    def translate_model(self) -> str:
        """Model id for the resolved translation provider."""
        return (
            self.groq_model_translate
            if self.resolved_llm_provider == "groq"
            else self.hf_model_translate
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()
