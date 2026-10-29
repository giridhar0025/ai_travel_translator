# Deploy — Railway

End-to-end runbook to ship the FastAPI backend to a public URL.
Local artifacts (`railway.json`, Dockerfile, env normalization, deploy guide)
all land in code review. The Railway dashboard steps live here — they touch a
billed account and a public URL, so they're yours to execute.

> Reference: agent brief at `agents/10-deployment.md`, plan at `BACKEND_BUILD_PLAN.md` §7 Phase 10.

---

## What's in code already

- `backend/railway.json` — Dockerfile builder, `/health` healthcheck, restart
  policy, pre-deploy command (`alembic upgrade head && python -m
  scripts.seed_phrases`).
- `backend/Dockerfile` — multi-stage, slim, non-root, runs `uvicorn` on
  `$PORT`.
- `app/config.py` — `Settings.database_url` validator rewrites Railway's
  `postgres://...` and bare `postgresql://...` to `postgresql+asyncpg://...`
  automatically. No env hand-editing needed.
- `android/app/build.gradle.kts` — reads `BACKEND_BASE_URL` from
  `android/local.properties`, exposes it as `BuildConfig.BACKEND_BASE_URL`.

---

## Step 1 — Railway account + project

1. Sign up at <https://railway.app>. The free trial covers initial smoke
   testing; the Hobby plan ($5/mo + usage) is enough for staging traffic.
2. From the dashboard: **New Project → Empty Project**. Name it
   `travel-translator` (or whatever).
3. Inside the project: **+ New → Database → Add PostgreSQL**. Railway
   provisions a managed Postgres. Note the auto-generated `DATABASE_URL` in
   the Postgres service's Variables tab — you'll wire it into the API
   service next.

## Step 2 — Add the API service

1. **+ New → GitHub Repo → Travel Translator AI UI Design**, then in the
   service settings:
   - **Root Directory:** `backend`
   - **Watch Paths:** `backend/**`
   - **Branch:** `main` (or your deploy branch)

   Railway picks up `backend/railway.json` from there automatically.

2. Service Variables — set these (Postgres URL comes from Step 1):

   | Var | Value / Origin |
   | --- | --- |
   | `DATABASE_URL` | **Reference variable** → Postgres service → `DATABASE_URL`. Auto-normalized by `app/config.py`, no manual edit needed. |
   | `APP_JWT_SECRET` | `openssl rand -hex 32` (run locally — never reuse the dev secret) |
   | `APP_JWT_EXPIRY_HOURS` | `720` |
   | `APP_ENV` | `production` |
   | `FIREBASE_PROJECT_ID` | `travel-translator-ai` |
   | `FIREBASE_SERVICE_ACCOUNT_JSON` | base64 of the service-account JSON (same value as local `.env`) |
   | `HF_API_TOKEN` | your HF token |
   | `HF_TIMEOUT_SECONDS` | `90` |
   | `HF_MODEL_TRANSLATE` | `Helsinki-NLP/opus-mt-en-mul` (Phase 3 swapped from m2m100) |
   | `HF_MODEL_STT` | `openai/whisper-large-v3` |
   | `REPLICATE_TTS_VERSION` | `resemble-ai/chatterbox-turbo` |
   | `CORS_ALLOWED_ORIGINS` | leave empty for v1 (mobile clients don't need it) |
   | `PORT` | Railway sets this automatically — don't override |

   Generate a fresh secret on your machine and paste it into the dashboard.
   Do not commit the generated value:
   ```bash
   openssl rand -hex 32
   ```

3. **Networking → Generate Domain** to mint a public URL. Copy it.

## Step 3 — First deploy

Push to `main` (or trigger deploy from the dashboard). Railway:

1. Builds the Dockerfile (~3–4 min cold; ~30 s with cache).
2. Runs the pre-deploy command (`alembic upgrade head &&
   python -m scripts.seed_phrases`) — both binaries live in
   `/app/.venv/bin` which is on `$PATH` in the runner image.
3. Boots `uvicorn` on `$PORT`.
4. Waits for `/health` to return 200 (timeout 30 s).
5. Promotes the deployment.

If the build fails, check the **Deploy Logs** tab. Most likely causes:
- `DATABASE_URL` not set or pointed at the wrong service → `OperationalError`
  before `/health` ever fires.
- HF or Firebase env vars missing → request-time 500s, not deploy failure.

> **Known gap:** the Dockerfile build hasn't been verified locally this
> session (Docker daemon was down). Static trace confirms `alembic` + seed
> are on the runner image's `$PATH` via the copied venv, but the first
> Railway build is your only check that the multi-stage assembly works.

## Step 4 — Smoke tests

Run these from your shell against the public URL. Replace `$URL`.

```bash
URL=https://travel-translator-production.up.railway.app

# 1. Health
curl -s $URL/health
# → {"status":"ok"}

# 2. Guest login
JWT=$(curl -s -X POST $URL/api/v1/auth/guest | jq -r .token)
echo "JWT length: ${#JWT}"

# 3. Translate (cold-start may take 5-15 s; warm should be <2 s)
curl -s -X POST $URL/api/v1/translate \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"source":"en","target":"hi","text":"Where is the hospital?","context":"travel"}'

# 4. Languages + categories (public, no auth)
curl -s $URL/api/v1/languages | jq 'length'
curl -s $URL/api/v1/phrases/categories | jq 'length'

# 5. /docs in a browser → every router from BACKEND_BUILD_PLAN.md §4 listed
open $URL/docs
```

Latency targets per brief: cold P50 < 5 s, warm P50 < 2 s on `/translate`.

## Step 5 — Wire Android at the live URL

```bash
# In android/local.properties, uncomment and set:
BACKEND_BASE_URL=https://your-railway-url.up.railway.app
```

Then rebuild the Android app. `BuildConfig.BACKEND_BASE_URL` flips to the
Railway URL. The networking layer (Retrofit/Ktor — not yet wired) reads from
that constant.

End-to-end smoke checklist:
- Google sign-in → `/auth/verify` → JWT issued
- en→hi text translate < 3 s warm
- Voice button → STT transcript → translate → TTS playback
- New conversation persists; opens after cold-start of the app
- Quick phrases load with cached translations on second open

## Step 6 — Rollback

- **Previous deployment**: Railway dashboard → Deployments tab → click the
  prior successful deploy → **Redeploy**. Database stays put.
- **Bad migration**: write a `downgrade()` and run `alembic downgrade -1`
  via Railway's one-off command runner. Phase 1 migration 0001 is
  destructive on downgrade (drops every table), so this is the prod-safety
  reason `downgrade()` matters going forward.
- **Nuclear**: archive the service, recreate from `railway.json`. DB
  survives if you only archive the API service.

---

## Things deliberately out of scope

- Multi-region, blue/green, autoscaling — bring back if traffic warrants.
- Codecov / coverage gating in deploy CI — local `pytest --cov` is the
  source of truth for now; backend.yml uploads an artifact.
- Pre-commit hooks — optional per Phase 9 brief.
