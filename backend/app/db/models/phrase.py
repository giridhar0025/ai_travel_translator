from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import DateTime, ForeignKey, Integer, String, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base
from app.db.mixins import UuidPkMixin

if TYPE_CHECKING:
    pass


class PhraseCategory(Base):
    __tablename__ = "phrase_categories"

    slug: Mapped[str] = mapped_column(String(64), primary_key=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    icon: Mapped[str] = mapped_column(String(16), nullable=False)
    color: Mapped[str] = mapped_column(String(16), nullable=False)  # hex
    sort_order: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    phrases: Mapped[list["Phrase"]] = relationship(
        "Phrase",
        back_populates="category",
        cascade="all, delete-orphan",
    )


class Phrase(UuidPkMixin, Base):
    __tablename__ = "phrases"

    category_slug: Mapped[str] = mapped_column(
        String(64),
        ForeignKey("phrase_categories.slug", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    source_lang: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("languages.code"),
        nullable=False,
    )
    source_text: Mapped[str] = mapped_column(String(512), nullable=False)
    sort_order: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    category: Mapped["PhraseCategory"] = relationship("PhraseCategory", back_populates="phrases")
    translations: Mapped[list["PhraseTranslation"]] = relationship(
        "PhraseTranslation",
        back_populates="phrase",
        cascade="all, delete-orphan",
    )


class PhraseTranslation(UuidPkMixin, Base):
    __tablename__ = "phrase_translations"
    __table_args__ = (
        UniqueConstraint("phrase_id", "target_lang", name="uq_phrase_translations_phrase_target"),
    )

    phrase_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("phrases.id", ondelete="CASCADE"),
        nullable=False,
    )
    target_lang: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("languages.code"),
        nullable=False,
    )
    translation: Mapped[str] = mapped_column(String(1024), nullable=False)
    pronunciation: Mapped[str | None] = mapped_column(String(1024), nullable=True)
    cached_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    phrase: Mapped["Phrase"] = relationship("Phrase", back_populates="translations")
