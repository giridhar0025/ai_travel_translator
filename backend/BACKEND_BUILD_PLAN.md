# Backend Build Plan — Travel Translator AI

> Backend for the Android (and later iOS) Travel Translator AI app.
> Lives under `backend/` in this repo. Not coupled to the mobile clients except via the REST contract in §4.

---

## 1. Stack decisions (locked)

| Concern                | Choice                                                       |
| ---------------------- | ------------------------------------------------------------ |
| Language / framework   | **Python 3.11+ / FastAPI**                                   |
| Hosting                | **Railway** (primary), Fly.io / Render as fallbacks          |
| AI inference           | **Hugging Face Inference API** (serverless) — commercial-safe model selection only (MIT / Apache 2.0) |
| Database               | **PostgreSQL** (Railway-managed or Neon)                     |
| Auth                   | **Firebase Auth** — Google, Apple, Email/Password, anonymous |
| Object storage         | **None** — Postgres-only, audio is streamed, not persisted   |
| API style              | REST + JSON (no streaming/SSE in v1)                         |
| ORM / migrations       | SQLAlchemy 2.0 + Alembic                                     |
| Async                  | Async-first (asyncpg, httpx, aiofiles)                       |
| Package management     | `uv` (fast resolver) with `pyproject.toml`                   |
| Test framework         | pytest + pytest-asyncio + httpx test client                  |
| Container              | Docker (multi-stage, slim Python base)                       |
| CI                     | GitHub Actions                                               |

---

## 2. Repo layout

```
backend/
├── BACKEND_BUILD_PLAN.md         # this file
├── BACKEND_BUILD_TRACKER.md      # per-task checklist
├── agents/                       # per-phase agent briefs
│   ├── 00-scaffolding.md
│   ├── 01-database.md
│   ├── 02-auth.md
│   ├── 03-translation.md
│   ├── 04-stt.md
│   ├── 05-tts.md
│   ├── 06-conversations.md
│   ├── 07-phrases.md
│   ├── 08-settings-favorites.md
│   ├── 09-testing.md
│   └── 10-deployment.md
├── app/
│   ├── main.py                   # FastAPI entrypoint
│   ├── config.py                 # pydantic-settings
│   ├── deps.py                   # shared dependencies (db session, auth user)
│   ├── auth/                     # Firebase verification, JWT middleware
│   ├── db/                       # SQLAlchemy models, session, base
│   ├── domain/                   # pure-Python domain types (Language, etc.)
│   ├── routers/                  # FastAPI routers per feature
│   │   ├── auth.py
│   │   ├── translate.py
│   │   ├── stt.py
│   │   ├── tts.py
│   │   ├── conversations.py
│   │   ├── phrases.py
│   │   ├── settings.py
│   │   └── favorites.py
│   ├── services/                 # business logic (HF clients, orchestration)
│   │   ├── translation.py
│   │   ├── stt.py
│   │   ├── tts.py
│   │   ├── conversations.py
│   │   └── phrases.py
│   ├── repositories/             # data access (one per aggregate)
│   └── schemas/                  # Pydantic request/response models
├── alembic/                      # migrations
├── tests/
│   ├── conftest.py
│   ├── integration/
│   └── unit/
├── scripts/
│   ├── seed_phrases.py           # seeds 30 starter phrases × 4 langs
│   └── healthcheck.py
├── Dockerfile
├── docker-compose.yml            # local Postgres + app
├── pyproject.toml
├── uv.lock
├── railway.json                  # deployment config
├── .env.example
└── README.md
```

---

## 3. Architecture (request lifecycle)

```
Android app
   │  HTTPS + Bearer <Firebase ID token>
   ▼
FastAPI (Railway)
   │
   ├── auth middleware ── verifies Firebase ID token via firebase-admin
   │                       (or accepts guest token issued by /auth/guest)
   │
   ├── routers/* ─────── parses Pydantic request schema
   │
   ├── services/* ────── orchestrates HF calls + DB writes
   │       │
   │       ├── translation.py ── httpx → HF Inference API
   │       │                      (facebook/nllb-200-distilled-600M)
   │       │
   │       ├── stt.py ────────── httpx → HF Inference API
   │       │                      (openai/whisper-large-v3)
   │       │
   │       └── tts.py ────────── httpx → HF Inference API
   │                              (facebook/mms-tts-{lang})
   │
   └── repositories/* ── async SQLAlchemy → Postgres
```

Key invariants:
- **Every protected route** depends on `get_current_user` which returns a `User` record (resolved from Firebase UID, lazily created).
- **Guest mode** is supported — `/auth/guest` mints a long-lived backend JWT bound to a `user.is_guest=True` row.
- **HF calls** go through one `HuggingFaceClient` with retry + backoff + per-model timeout.
- **No model is loaded in-process** — keeps the Railway box tiny and CPU-only.

---

## 4. REST contract (v1)

All routes are prefixed `/api/v1`. Auth: `Authorization: Bearer <token>` unless marked Public.

### 4.1 Auth
| Method | Path                  | Auth   | Purpose                                                          |
| ------ | --------------------- | ------ | ---------------------------------------------------------------- |
| POST   | `/auth/verify`        | Public | Verify a Firebase ID token, upsert user, return profile + app JWT |
| POST   | `/auth/guest`         | Public | Create an anonymous user, return app JWT                          |
| GET    | `/me`                 | Yes    | Return current user profile + settings                            |
| DELETE | `/me`                 | Yes    | Delete account + all owned data                                   |

### 4.2 Translation
| Method | Path                  | Body                                              | Returns                                              |
| ------ | --------------------- | ------------------------------------------------- | ---------------------------------------------------- |
| POST   | `/translate`          | `{source, target, text, context?}`                | `{translation, pronunciation, latency_ms, model}`    |
| POST   | `/translate/batch`    | `{source, target, items: [text...]}`              | `{results: [...]}`                                   |
| GET    | `/languages`          | —                                                 | `[{code, name, flag, stt, tts, translate}]`          |

Languages (v1): `en`, `hi`, `te`, `th`. `stt/tts/translate` flags reflect HF model coverage per locale.

### 4.3 Speech-to-Text
| Method | Path             | Body                              | Returns                              |
| ------ | ---------------- | --------------------------------- | ------------------------------------ |
| POST   | `/stt/transcribe`| multipart: `audio` + `language?`  | `{transcript, language, confidence}` |

Audio: 16 kHz mono WAV/M4A preferred. Max 30 s per request (matches HF limits).

### 4.4 Text-to-Speech
| Method | Path             | Body                                    | Returns                  |
| ------ | ---------------- | --------------------------------------- | ------------------------ |
| POST   | `/tts/synthesize`| `{text, language, speed: normal|slow}`  | `audio/wav` binary stream|

### 4.5 Conversations & messages
| Method | Path                                  | Purpose                                                            |
| ------ | ------------------------------------- | ------------------------------------------------------------------ |
| GET    | `/conversations?q=&limit=&cursor=`    | Paginated list grouped by date (Today/Yesterday/older)              |
| POST   | `/conversations`                      | Create empty conversation `{title?}`                                |
| GET    | `/conversations/{id}`                 | Conversation + paginated messages                                   |
| DELETE | `/conversations/{id}`                 | Soft-delete                                                         |
| POST   | `/conversations/{id}/messages`        | Append user/AI message `{role, text, lang, translation?, audio?}`   |

### 4.6 Quick phrases
| Method | Path                                          | Purpose                                                |
| ------ | --------------------------------------------- | ------------------------------------------------------ |
| GET    | `/phrases/categories`                         | 5 categories (Restaurants, Directions, …)              |
| GET    | `/phrases?category={slug}&target={lang}`      | Phrases pre-translated to target (cached)              |

### 4.7 Settings & favorites
| Method | Path              | Body / Returns                                         |
| ------ | ----------------- | ------------------------------------------------------ |
| GET    | `/settings`       | `{defaultLanguage, playbackSpeed, offlineEnabled}`     |
| PATCH  | `/settings`       | Partial update                                          |
| GET    | `/favorites`      | List user-favorited phrases or translations            |
| POST   | `/favorites`      | `{kind: phrase|translation, ref_id, source, target}`   |
| DELETE | `/favorites/{id}` | Unfavorite                                              |
| GET    | `/recents`        | Last N translations per user                            |

---

## 5. Hugging Face model registry (v1) — commercial-safe

Every model below is **MIT** or **Apache 2.0** licensed → safe for paid apps, ads, subscriptions, Play Store distribution.

| Capability  | Model                                | License     | Notes                                                                              |
| ----------- | ------------------------------------ | ----------- | ---------------------------------------------------------------------------------- |
| Translate   | `Helsinki-NLP/opus-mt-en-mul` (en→X) + `Helsinki-NLP/opus-mt-mul-en` (X→en) | CC-BY 4.0 | Per HF Inference router (`router.huggingface.co/hf-inference`), `m2m100`/`nllb` are no longer hf-inference-supported. OPUS-MT is. Cross-language pairs chain via English in two hops. `>>tgt<<` prefix on en→X. |
| STT         | `openai/whisper-large-v3`            | MIT         | Multilingual, ~30 s limit per call. Fallback: `openai/whisper-small`.              |
| TTS — en    | `parler-tts/parler-tts-mini-v1`      | Apache 2.0  | Voice controlled via text description (`"A clear female voice, neutral tone."`).   |
| TTS — hi    | `ai4bharat/indic-parler-tts`         | Apache 2.0  | One model for 21 Indian languages. Pass `language=hindi` in the description.       |
| TTS — te    | `ai4bharat/indic-parler-tts`         | Apache 2.0  | Same model as Hindi. Pass `language=telugu` in the description.                    |
| TTS — th    | **client-side (Android TTS engine)** | —           | No production-grade commercial-safe Thai TTS in open weights. Documented in §5.1.  |
| Pronunciation | `epitran` (Python lib)             | MIT         | IPA transliteration for hi/te/th. Latin script for en.                             |

### 5.1 Thai TTS fallback

The only multilingual open-weight TTS models that include Thai (`facebook/mms-tts-tha`, `facebook/seamless-m4t-v2-large`) are CC-BY-NC and therefore unsafe for a commercial app. Until a permissively-licensed Thai TTS model exists:

- `POST /tts/synthesize` with `language=th` returns **HTTP 415** with body `{"detail": "client_side_tts_required", "language": "th"}`.
- The Android client recognizes this code and falls back to `android.speech.tts.TextToSpeech` with locale `th_TH` (Google's system engine ships a Thai voice on most modern Android devices).
- Behavior is invisible to the user — same UI, same Play button, audio just plays from the device instead of the server.

### 5.2 Voice description prompts (Parler family)

Parler models are controlled by a natural-language description, not speaker IDs. Defaults baked into the service:

| Lang | Description                                                                          |
| ---- | ------------------------------------------------------------------------------------ |
| en   | `"A clear female voice with neutral tone, moderate pace, studio-quality recording."` |
| hi   | `"A clear female voice speaking Hindi with neutral tone and moderate pace."`         |
| te   | `"A clear female voice speaking Telugu with neutral tone and moderate pace."`        |

All HF calls use a single client (`app/services/hf_client.py`) with:
- `httpx.AsyncClient` (HTTP/2)
- 30 s timeout (configurable per call)
- 3 retries with exponential backoff on 503 (HF cold-start)
- Authorization: `Bearer ${HF_API_TOKEN}`

---

## 6. Database schema (v1)

```sql
users               (id, firebase_uid?, email?, display_name?, photo_url?, is_guest, created_at)
user_settings       (user_id PK, default_language, playback_speed, offline_enabled, updated_at)
languages           (code PK, name, flag_emoji, stt_supported, tts_supported, translate_supported)
phrase_categories   (slug PK, name, icon, color, sort_order)
phrases             (id, category_slug, source_lang, source_text, sort_order)
phrase_translations (id, phrase_id, target_lang, translation, pronunciation, cached_at)
conversations       (id, user_id, title, last_message_at, message_count, deleted_at)
messages            (id, conversation_id, role, source_text, source_lang, translated_text, target_lang, pronunciation, created_at)
favorites           (id, user_id, kind, ref_id, source_lang, target_lang, created_at)
recents             (id, user_id, source_text, source_lang, translated_text, target_lang, created_at)
```

Indexes: `conversations(user_id, last_message_at DESC)`, `messages(conversation_id, created_at)`, `phrase_translations(phrase_id, target_lang)` unique.

Soft-delete pattern: `deleted_at IS NULL` filter on read paths.

---

## 7. Phases & agents

Each phase has its own brief in `agents/`. A phase is meant to be a single agent's scope of work — well-defined inputs, deliverables, and a verification step.

| Phase | Title                    | Agent brief                          | Depends on |
| ----- | ------------------------ | ------------------------------------ | ---------- |
| 0     | Scaffolding & infra      | `agents/00-scaffolding.md`           | —          |
| 1     | Database & migrations    | `agents/01-database.md`              | 0          |
| 2     | Auth (Firebase)          | `agents/02-auth.md`                  | 0, 1       |
| 3     | Translation service      | `agents/03-translation.md`           | 0, 2       |
| 4     | Speech-to-text service   | `agents/04-stt.md`                   | 0, 2       |
| 5     | Text-to-speech service   | `agents/05-tts.md`                   | 0, 2       |
| 6     | Conversations & history  | `agents/06-conversations.md`         | 1, 2, 3    |
| 7     | Phrases library          | `agents/07-phrases.md`               | 1, 3       |
| 8     | Settings & favorites     | `agents/08-settings-favorites.md`    | 1, 2       |
| 9     | Testing & CI             | `agents/09-testing.md`               | 0–8        |
| 10    | Deployment (Railway)     | `agents/10-deployment.md`            | 0–9        |

Phases 3, 4, 5 are independent of each other and can run in parallel. So can 7 and 8.

---

## 8. Environment variables

`.env.example`:

```
# App
APP_ENV=development
APP_PORT=8000
APP_JWT_SECRET=<change-me>
APP_JWT_EXPIRY_HOURS=720          # 30 days

# Database
DATABASE_URL=postgresql+asyncpg://postgres:postgres@db:5432/translator

# Firebase
FIREBASE_PROJECT_ID=
FIREBASE_SERVICE_ACCOUNT_JSON=    # base64-encoded service-account.json

# Hugging Face
HF_API_TOKEN=
HF_TIMEOUT_SECONDS=30
# Translation: OPUS-MT routing is hardcoded in app/services/translation.py
# (en↔X via opus-mt-en-mul + opus-mt-mul-en; X↔Y chains via English).
HF_MODEL_STT=openai/whisper-large-v3
HF_MODEL_TTS_EN=parler-tts/parler-tts-mini-v1
HF_MODEL_TTS_HI=ai4bharat/indic-parler-tts
HF_MODEL_TTS_TE=ai4bharat/indic-parler-tts
# HF_MODEL_TTS_TH intentionally absent — Thai TTS handled client-side (see plan §5.1)

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

---

## 9. Non-goals for v1

These are explicitly **not** in scope; we'll add them in v2 if needed:

- Streaming translations (SSE/WebSocket).
- Audio persistence (no S3/R2 storage of user recordings or TTS output).
- OCR / photo translation.
- Group conversations / multi-device sync of in-progress chats.
- Push notifications.
- Admin dashboard.
- Rate limiting beyond per-IP basic throttle (add when scaling).

---

## 10. Definition of done (backend)

- [ ] All 11 phases checked off in `BACKEND_BUILD_TRACKER.md`.
- [ ] `pytest` green locally and in CI.
- [ ] `curl https://<railway-url>/health` returns 200.
- [ ] OpenAPI spec at `/docs` reflects every route in §4.
- [ ] Android app can sign in with Google, translate text, transcribe voice, play TTS audio, save conversations, and view history end-to-end against the deployed backend.
