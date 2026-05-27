from datetime import datetime
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.user import User
from app.deps import get_current_user, get_db_session
from app.repositories.conversation import (
    ConversationRepo,
    MessageRepo,
    NewMessage,
)
from app.schemas.conversation import (
    AppendMessageRequest,
    ConversationDetailResponse,
    ConversationSchema,
    ConversationsListResponse,
    CreateConversationRequest,
    MessageSchema,
)
from app.services.conversations import date_groups

router = APIRouter(prefix="/conversations", tags=["conversations"])

MAX_LIST_LIMIT = 100
MAX_MESSAGES_LIMIT = 200


@router.get("", response_model=ConversationsListResponse)
async def list_conversations(
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
    q: Annotated[str | None, Query(max_length=200)] = None,
    limit: Annotated[int, Query(ge=1, le=MAX_LIST_LIMIT)] = 20,
    cursor: datetime | None = None,
) -> ConversationsListResponse:
    repo = ConversationRepo(session)
    items = await repo.list_for_user(current_user.id, q=q, limit=limit, cursor=cursor)
    next_cursor = items[-1].last_message_at if len(items) == limit else None
    return ConversationsListResponse(
        items=[ConversationSchema.model_validate(c) for c in items],
        next_cursor=next_cursor,
        groups=date_groups(items, user_timezone=current_user.timezone),
    )


@router.post("", response_model=ConversationSchema, status_code=status.HTTP_201_CREATED)
async def create_conversation(
    body: CreateConversationRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> ConversationSchema:
    repo = ConversationRepo(session)
    conv = await repo.create(current_user.id, title=body.title)
    await session.commit()
    await session.refresh(conv)
    return ConversationSchema.model_validate(conv)


@router.get("/{conversation_id}", response_model=ConversationDetailResponse)
async def get_conversation(
    conversation_id: UUID,
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
    limit: Annotated[int, Query(ge=1, le=MAX_MESSAGES_LIMIT)] = 50,
    before_id: UUID | None = None,
) -> ConversationDetailResponse:
    convo_repo = ConversationRepo(session)
    msg_repo = MessageRepo(session)

    conv = await convo_repo.get_for_user(conversation_id, current_user.id)
    if conv is None:
        raise HTTPException(status_code=404, detail="conversation not found")

    before_ts: datetime | None = None
    if before_id is not None:
        anchor = await msg_repo.get(before_id)
        # If the client sent an unknown id we just return the newest page.
        if anchor is not None and anchor.conversation_id == conv.id:
            before_ts = anchor.created_at

    messages = await msg_repo.list_for_conversation(conv.id, limit=limit, before=before_ts)
    next_before = messages[0].created_at if len(messages) == limit else None
    return ConversationDetailResponse(
        conversation=ConversationSchema.model_validate(conv),
        messages=[MessageSchema.model_validate(m) for m in messages],
        next_before=next_before,
    )


@router.delete("/{conversation_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_conversation(
    conversation_id: UUID,
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> Response:
    repo = ConversationRepo(session)
    ok = await repo.soft_delete(conversation_id, current_user.id)
    if not ok:
        raise HTTPException(status_code=404, detail="conversation not found")
    await session.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post(
    "/{conversation_id}/messages",
    response_model=MessageSchema,
    status_code=status.HTTP_201_CREATED,
)
async def append_message(
    conversation_id: UUID,
    body: AppendMessageRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> MessageSchema:
    repo = ConversationRepo(session)
    conv = await repo.get_for_user(conversation_id, current_user.id)
    if conv is None:
        raise HTTPException(status_code=404, detail="conversation not found")

    message = await repo.append_message(
        conv,
        NewMessage(
            role=body.role,
            source_text=body.source_text,
            source_lang=body.source_lang,
            translated_text=body.translated_text,
            target_lang=body.target_lang,
            pronunciation=body.pronunciation,
        ),
    )
    await session.commit()
    await session.refresh(message)
    return MessageSchema.model_validate(message)
