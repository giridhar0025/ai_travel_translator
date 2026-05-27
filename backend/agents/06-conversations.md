# Phase 6 — Conversations & history

## Scope
CRUD + search for conversations and messages, plus the date-grouped list view the History screen renders.

## Deliverables

### 1. Repository — `app/repositories/conversations.py`
- `list_for_user(user_id, q: str | None, limit: int, cursor: datetime | None)`:
  - WHERE `user_id=$1 AND deleted_at IS NULL`
  - If `q`: trigram match on `messages.source_text OR messages.translated_text OR title` (use `WHERE EXISTS` subquery).
  - ORDER BY `last_message_at DESC`.
  - Keyset pagination by `last_message_at` (return next cursor in response).
- `create(user_id, title) -> Conversation`.
- `get_with_messages(conversation_id, user_id, limit, before_id)`.
- `append_message(conversation_id, msg_data)` — transactional, bumps `last_message_at` + `message_count`.
- `soft_delete(conversation_id, user_id)`.

### 2. Router — `app/routers/conversations.py`
- `GET /conversations?q=&limit=20&cursor=...` → `{items: [...], next_cursor, groups}` where `groups` is a server-computed `[{label: "Today"|"Yesterday"|"YYYY-MM-DD", count}]` to match the Compose History screen.
- `POST /conversations` body `{title?}` → `Conversation`.
- `GET /conversations/{id}?limit=50&before_id=...` → `{conversation, messages, next_before_id}`.
- `DELETE /conversations/{id}` → 204.
- `POST /conversations/{id}/messages` body `{role: "user"|"ai", source_text, source_lang, translated_text?, target_lang?, pronunciation?}` → `Message`.

### 3. Date grouping
- Compute in the service using user's local TZ from `User.timezone` (add column in Phase 1 if missing — default `UTC`).
- "Today" = same calendar day. "Yesterday" = prior day. Older = ISO date label.

### 4. Auto-title
- When the first message is appended to a titleless conversation, set `title` to the first 40 chars of `source_text` (or `"Conversation"` if blank).

## Verification
- Integration test: create → append 3 messages → GET → verify `message_count=3`, `last_message_at` is latest message timestamp.
- Search: append messages with the word "menu" → search `q=menu` returns the conversation; search `q=zzz` returns empty.
- Date grouping: insert one message dated today, one yesterday, one 3 days ago → list returns 3 groups.
- Soft-delete: deleted conversations excluded from list but rows remain.

## Out of scope
Hard delete, export, share, pinning.
