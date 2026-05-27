# Phase 8 — Settings, favorites, recents

## Scope
User settings (`default_language`, `playback_speed`, `offline_enabled`), favorites (phrases or translations the user starred), and the recents list (last 20 translations).

## Deliverables

### 1. Settings — `app/routers/settings.py`
- `GET /settings` → returns the row, creating with defaults if missing.
- `PATCH /settings` body `{default_language?, playback_speed?, offline_enabled?}`:
  - Validate `default_language` is in supported set.
  - Validate `playback_speed in {"normal", "slow", "very_slow"}`.
  - Partial update — unset fields untouched.

### 2. Favorites — `app/routers/favorites.py`
- `GET /favorites?kind=phrase|translation` → list.
- `POST /favorites` body `{kind, ref_id, source_lang, target_lang}`:
  - If `kind="phrase"`, `ref_id` is the phrase ID.
  - If `kind="translation"`, `ref_id` is a recent.id or message.id (client picks).
  - 409 if already favorited (unique on `(user_id, kind, ref_id, target_lang)`).
- `DELETE /favorites/{id}` → 204. 404 if not owner.

### 3. Recents — `app/routers/recents.py`
- `GET /recents?limit=20` → 20 most recent translations, deduped by `(source_text, target_lang)` keeping the newest.
- No POST: recents are written internally by `/translate` (Phase 3 §4).

### 4. Recents background write — `app/services/translation.py`
- On `/translate` success, schedule `BackgroundTasks.add_task(insert_recent, user_id, payload)`.
- `insert_recent` does an upsert: if a recent with same `(user_id, source_text, target_lang)` exists in last 24 h, just bump `created_at`; else insert.

## Verification
- `GET /settings` on a new user returns defaults.
- `PATCH /settings {default_language: "hi"}` persists; `GET /settings` reflects it.
- Favorite a phrase twice → second POST returns 409.
- Translate same text twice within a minute → `GET /recents` returns one row, not two.

## Out of scope
Cross-device sync semantics (clients refetch on app open in v1).
