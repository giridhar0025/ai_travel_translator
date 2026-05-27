# Phase 10 — Deployment (Railway)

## Scope
Deploy the backend to Railway with managed Postgres, wire env vars, run migrations on release, and smoke-test the Android app against the live URL.

## Deliverables

### 1. Railway config — `backend/railway.json`
```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile"
  },
  "deploy": {
    "startCommand": "uvicorn app.main:app --host 0.0.0.0 --port $PORT",
    "healthcheckPath": "/health",
    "healthcheckTimeout": 30,
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 3
  }
}
```

### 2. Release command (migrations)
Set Railway's "Pre-deploy command" to:
```
uv run alembic upgrade head && uv run python -m scripts.seed_phrases
```

### 3. Services
- **api** — built from `backend/Dockerfile`. Exposes `$PORT`.
- **postgres** — Railway Postgres plugin. Copy `DATABASE_URL` into the api service env. **Convert** `postgres://` → `postgresql+asyncpg://` (Railway emits the sync URL; SQLAlchemy async needs the driver suffix).

### 4. Environment variables (Railway dashboard)
Mirror `.env.example` (plan §8). The ones that must NOT live in git:
- `APP_JWT_SECRET` (generate with `openssl rand -hex 32`)
- `FIREBASE_SERVICE_ACCOUNT_JSON` (base64 of the JSON file)
- `HF_API_TOKEN`

### 5. CORS
For now, set `CORS_ALLOWED_ORIGINS` to the empty string — Android requests don't need CORS. Add `https://...` only when a web client appears.

### 6. Smoke tests
Manual checklist before flipping the Android app's base URL:
- [ ] `curl https://<url>/health` → 200
- [ ] `curl -X POST https://<url>/auth/guest` → 200 with JWT
- [ ] `curl -X POST -H "Authorization: Bearer <jwt>" https://<url>/api/v1/translate -d '{"source":"en","target":"hi","text":"Hello"}' -H "Content-Type: application/json"` → 200 with translation
- [ ] `/docs` (OpenAPI) lists every route from `BACKEND_BUILD_PLAN.md` §4
- [ ] Cold-start P50 < 5 s, warm P50 < 2 s for `/translate`

### 7. Android client wiring
- Add `BACKEND_BASE_URL` to Android `local.properties` and `BuildConfig`.
- Run the Android app against the deployed URL; verify:
  - Sign in with Google works (Firebase ID token round-trip).
  - Voice → STT → translate → TTS playback works end-to-end.
  - History persists across cold starts.

### 8. Rollback plan
- Railway keeps the previous build. `railway down` or pin to the prior deployment from the dashboard.
- Migrations are forward-only — write a `downgrade()` for any change touching prod data.

## Verification
- The CI badge is green.
- `/health` returns 200 from the public URL.
- The Android app, pointed at the public URL, completes the end-to-end flow listed in `BACKEND_BUILD_TRACKER.md` Phase 10.

## Out of scope
Multi-region, blue/green, autoscaling rules. Bring back if traffic warrants.
