from pydantic import BaseModel, ConfigDict


class LanguageSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    code: str
    name: str
    flag_emoji: str
    stt_supported: bool
    tts_supported: bool
    translate_supported: bool
