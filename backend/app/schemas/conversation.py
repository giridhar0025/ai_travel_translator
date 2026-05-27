from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

MessageRole = Literal["user", "ai"]


class MessageSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    conversation_id: UUID
    role: str
    source_text: str
    source_lang: str
    translated_text: str | None
    target_lang: str | None
    pronunciation: str | None
    created_at: datetime


class ConversationSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    user_id: UUID
    title: str | None
    last_message_at: datetime
    message_count: int
    created_at: datetime


class GroupInfo(BaseModel):
    """Date bucket for the History screen. `label` is either `"Today"`,
    `"Yesterday"`, or an ISO date `YYYY-MM-DD` computed in the user's
    timezone."""

    label: str
    count: int


class ConversationsListResponse(BaseModel):
    items: list[ConversationSchema]
    next_cursor: datetime | None
    groups: list[GroupInfo]


class CreateConversationRequest(BaseModel):
    title: str | None = Field(default=None, max_length=256)


class AppendMessageRequest(BaseModel):
    role: MessageRole
    source_text: str = Field(min_length=1, max_length=4096)
    source_lang: str = Field(min_length=2, max_length=8)
    translated_text: str | None = Field(default=None, max_length=4096)
    target_lang: str | None = Field(default=None, min_length=2, max_length=8)
    pronunciation: str | None = Field(default=None, max_length=4096)


class ConversationDetailResponse(BaseModel):
    conversation: ConversationSchema
    messages: list[MessageSchema]
    # `next_before` is the `created_at` of the oldest returned message — pass
    # it back as `?before=` to load the next older page.
    next_before: datetime | None
