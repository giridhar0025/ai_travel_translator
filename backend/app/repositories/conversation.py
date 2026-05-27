from dataclasses import dataclass
from datetime import datetime
from uuid import UUID

from sqlalchemy import exists, func, or_, select, update

from app.db.models.conversation import Conversation, Message
from app.repositories.base import BaseRepo

AUTO_TITLE_MAX_CHARS = 40


@dataclass(frozen=True)
class NewMessage:
    role: str
    source_text: str
    source_lang: str
    translated_text: str | None = None
    target_lang: str | None = None
    pronunciation: str | None = None


class ConversationRepo(BaseRepo):
    async def get(self, conversation_id: UUID) -> Conversation | None:
        return await self.session.get(Conversation, conversation_id)

    async def get_for_user(
        self, conversation_id: UUID, user_id: UUID, *, include_deleted: bool = False
    ) -> Conversation | None:
        """Fetch a conversation that belongs to `user_id`. Returns None if not found
        or owned by someone else — callers map None to 404."""
        stmt = select(Conversation).where(
            Conversation.id == conversation_id,
            Conversation.user_id == user_id,
        )
        if not include_deleted:
            stmt = stmt.where(Conversation.deleted_at.is_(None))
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def list_for_user(
        self,
        user_id: UUID,
        *,
        q: str | None = None,
        limit: int = 20,
        cursor: datetime | None = None,
    ) -> list[Conversation]:
        """List non-deleted conversations ordered by `last_message_at DESC`.

        Trigram search (`q`) matches the conversation title OR any of its
        messages' source/translated text via the GIN trgm indexes (see migration
        0001). Keyset pagination uses `last_message_at < cursor`.
        """
        stmt = select(Conversation).where(
            Conversation.user_id == user_id,
            Conversation.deleted_at.is_(None),
        )

        if q:
            pattern = f"%{q}%"
            msg_match = exists().where(
                Message.conversation_id == Conversation.id,
                or_(
                    Message.source_text.ilike(pattern),
                    Message.translated_text.ilike(pattern),
                ),
            )
            stmt = stmt.where(or_(Conversation.title.ilike(pattern), msg_match))

        if cursor is not None:
            stmt = stmt.where(Conversation.last_message_at < cursor)

        stmt = stmt.order_by(Conversation.last_message_at.desc()).limit(limit)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def create(self, user_id: UUID, title: str | None = None) -> Conversation:
        conv = Conversation(user_id=user_id, title=title)
        self.session.add(conv)
        await self.session.flush()
        await self.session.refresh(conv)
        return conv

    async def add(self, conversation: Conversation) -> Conversation:
        self.session.add(conversation)
        await self.session.flush()
        return conversation

    async def soft_delete(self, conversation_id: UUID, user_id: UUID) -> bool:
        """Mark deleted_at = now() if owned by user and not already deleted.
        Returns True if a row was updated."""
        stmt = (
            update(Conversation)
            .where(
                Conversation.id == conversation_id,
                Conversation.user_id == user_id,
                Conversation.deleted_at.is_(None),
            )
            .values(deleted_at=func.now())
            .returning(Conversation.id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none() is not None

    async def append_message(self, conversation: Conversation, msg: NewMessage) -> Message:
        """Transactional append: insert message, bump last_message_at +
        message_count, auto-title if the conversation has none."""
        message = Message(
            conversation_id=conversation.id,
            role=msg.role,
            source_text=msg.source_text,
            source_lang=msg.source_lang,
            translated_text=msg.translated_text,
            target_lang=msg.target_lang,
            pronunciation=msg.pronunciation,
        )
        self.session.add(message)
        await self.session.flush()
        await self.session.refresh(message)

        conversation.last_message_at = message.created_at
        conversation.message_count = (conversation.message_count or 0) + 1
        if not conversation.title:
            snippet = (msg.source_text or "").strip()
            conversation.title = snippet[:AUTO_TITLE_MAX_CHARS] or "Conversation"
        await self.session.flush()
        return message


class MessageRepo(BaseRepo):
    async def add(self, message: Message) -> Message:
        self.session.add(message)
        await self.session.flush()
        return message

    async def list_for_conversation(
        self,
        conversation_id: UUID,
        *,
        limit: int = 50,
        before: datetime | None = None,
    ) -> list[Message]:
        """List messages oldest-first. If `before` is given, only messages
        with `created_at < before` are returned (keyset pagination for the
        'load older' UI affordance)."""
        stmt = select(Message).where(Message.conversation_id == conversation_id)
        if before is not None:
            stmt = stmt.where(Message.created_at < before)
        stmt = stmt.order_by(Message.created_at).limit(limit)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def get(self, message_id: UUID) -> Message | None:
        return await self.session.get(Message, message_id)
