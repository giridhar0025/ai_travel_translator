# Phase 2 — Auth (Firebase)

## Scope
Verify Firebase ID tokens from the Android app, upsert a `User`, and mint a short-lived backend JWT used as the bearer token for every other route. Support guest mode (no Firebase identity).

## Setup (manual, one-time)
1. Create a Firebase project (e.g. `travel-translator-ai`).
2. In **Authentication → Sign-in method**: enable Google, Apple, Email/Password, Anonymous.
3. **Project settings → Service accounts**: download `service-account.json`.
4. Base64-encode: `base64 -i service-account.json | pbcopy`.
5. Paste into `FIREBASE_SERVICE_ACCOUNT_JSON` env var (Railway dashboard + local `.env`).
6. Put `FIREBASE_PROJECT_ID` next to it.

Apple OAuth requires a paid Apple Developer account + Service ID configured in the Apple Developer portal. If not ready, mark Apple as "later" and ship with Google + Email + Anonymous.

## Deliverables

### 1. Firebase verifier — `app/auth/firebase.py`
- Lazy init: decode the base64 service account JSON, write to a tmp file, init `firebase_admin` once.
- `async def verify_id_token(id_token: str) -> FirebaseUser` — returns `FirebaseUser(uid, email, name, picture, provider)` or raises `AuthError`.
- Wrap `auth.verify_id_token` in `asyncio.to_thread` (firebase-admin is sync).

### 2. Backend JWT — `app/auth/tokens.py`
- HS256, secret from `APP_JWT_SECRET`, claims: `sub=user.id`, `is_guest`, `exp`, `iat`, `iss="travel-translator-api"`.
- `issue_token(user) -> str`, `verify_token(token) -> TokenPayload`.

### 3. Routers — `app/routers/auth.py`
- `POST /auth/verify` body `{id_token: str}`:
  1. `verify_id_token` (Firebase).
  2. Upsert `User` by `firebase_uid`, fill `email`, `display_name`, `photo_url`.
  3. Ensure `UserSettings` row exists (defaults: `default_language="en"`, `playback_speed="normal"`, `offline_enabled=false`).
  4. Return `{user, token}`.
- `POST /auth/guest`:
  1. Create `User(is_guest=True, firebase_uid=None)`.
  2. Same settings init.
  3. Return `{user, token}`.

### 4. Routers — `app/routers/me.py`
- `GET /me` returns `{user, settings}`.
- `DELETE /me` cascades user-owned rows (conversations, messages, favorites, recents, settings) inside a single transaction. Soft-deletes when applicable.

### 5. DI — `app/deps.py::get_current_user`
- Reads `Authorization: Bearer <jwt>`.
- Verifies, loads `User` by `sub`. 401 if missing/invalid/expired/user-not-found.

## Verification
- Unit tests with a mock Firebase verifier:
  - valid token → 200 + JWT
  - expired → 401
  - wrong audience → 401
  - revoked → 401
- `POST /auth/guest` always returns a fresh user.
- `GET /me` with a freshly issued JWT returns the same user.
- `DELETE /me` removes the user and their owned rows.

## Out of scope
Email magic links, password reset flows (Firebase handles password reset on the client). Account merging (guest → authenticated).
