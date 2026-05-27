from sqlalchemy import Boolean, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class Language(Base):
    __tablename__ = "languages"

    code: Mapped[str] = mapped_column(String(8), primary_key=True)  # ISO 639-1
    name: Mapped[str] = mapped_column(String(64), nullable=False)
    flag_emoji: Mapped[str] = mapped_column(String(8), nullable=False)
    stt_supported: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    tts_supported: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    translate_supported: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
