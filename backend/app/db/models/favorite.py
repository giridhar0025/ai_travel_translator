from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import ForeignKey, Index, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base
from app.db.mixins import CreatedAtMixin, UuidPkMixin

if TYPE_CHECKING:
    from app.db.models.user import User


class Favorite(UuidPkMixin, CreatedAtMixin, Base):
    __tablename__ = "favorites"
    __table_args__ = (
        UniqueConstraint(
            "user_id",
            "kind",
            "ref_id",
            "target_lang",
            name="uq_favorites_user_kind_ref_target",
        ),
        Index("ix_favorites_user_created", "user_id", "created_at"),
    )

    user_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    kind: Mapped[str] = mapped_column(String(16), nullable=False)  # "phrase" | "translation"
    ref_id: Mapped[UUID] = mapped_column(PG_UUID(as_uuid=True), nullable=False)
    source_lang: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("languages.code"),
        nullable=False,
    )
    target_lang: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("languages.code"),
        nullable=False,
    )

    user: Mapped["User"] = relationship("User", back_populates="favorites")


class Recent(UuidPkMixin, CreatedAtMixin, Base):
    __tablename__ = "recents"
    __table_args__ = (Index("ix_recents_user_created", "user_id", "created_at"),)

    user_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    source_text: Mapped[str] = mapped_column(String(4096), nullable=False)
    source_lang: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("languages.code"),
        nullable=False,
    )
    translated_text: Mapped[str] = mapped_column(String(4096), nullable=False)
    target_lang: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("languages.code"),
        nullable=False,
    )

    user: Mapped["User"] = relationship("User", back_populates="recents")
