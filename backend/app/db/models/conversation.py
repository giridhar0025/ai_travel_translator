from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String, func
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base
from app.db.mixins import CreatedAtMixin, UuidPkMixin

if TYPE_CHECKING:
    from app.db.models.user import User


class Conversation(UuidPkMixin, CreatedAtMixin, Base):
    __tablename__ = "conversations"
    __table_args__ = (
        Index(
            "ix_conversations_user_last_message",
            "user_id",
            "last_message_at",
            postgresql_using="btree",
        ),
    )

    user_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    title: Mapped[str | None] = mapped_column(String(256), nullable=True)
    last_message_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )
    message_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    deleted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )

    user: Mapped["User"] = relationship("User", back_populates="conversations")
    messages: Mapped[list["Message"]] = relationship(
        "Message",
        back_populates="conversation",
        cascade="all, delete-orphan",
        order_by="Message.created_at",
    )


class Message(UuidPkMixin, CreatedAtMixin, Base):
    __tablename__ = "messages"
    __table_args__ = (Index("ix_messages_conversation_created", "conversation_id", "created_at"),)

    conversation_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("conversations.id", ondelete="CASCADE"),
        nullable=False,
    )
    role: Mapped[str] = mapped_column(String(16), nullable=False)  # "user" | "ai"
    source_text: Mapped[str] = mapped_column(String(4096), nullable=False)
    source_lang: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("languages.code"),
        nullable=False,
    )
    translated_text: Mapped[str | None] = mapped_column(String(4096), nullable=True)
    target_lang: Mapped[str | None] = mapped_column(
        String(8),
        ForeignKey("languages.code"),
        nullable=True,
    )
    pronunciation: Mapped[str | None] = mapped_column(String(4096), nullable=True)

    conversation: Mapped["Conversation"] = relationship("Conversation", back_populates="messages")
