# Travel Translator — Backend

FastAPI backend for the Travel Translator AI Android (and later iOS) apps.

Stack: Python 3.11 + FastAPI + SQLAlchemy 2 (async) + Alembic + PostgreSQL + Firebase Auth + Hugging Face Inference API. Deployed on Railway.

See `BACKEND_BUILD_PLAN.md` for the full architecture and `BACKEND_BUILD_TRACKER.md` for the per-phase checklist. Per-phase agent briefs live in `agents/`.

---

## Prerequisites

- [`uv`](https://docs.astral.sh/uv/) — Python package manager (`brew install uv`)
- Docker + Docker Compose
- A `.env` file (copy from `.env.example`)

```bash
cp .env.example .env
# Edit .env — at minimum set APP_JWT_SECRET and (later) HF_API_TOKEN, FIREBASE_*
```

---

## Quick start (local dev)

```bash
# Install deps (uv will download Python 3.11 if needed)
uv sync

# Start Postgres
docker compose up -d db

# (Phase 1+) Apply migrations
uv run alembic upgrade head

# Run the API with auto-reload
uv run uvicorn app.main:app --reload --port 8123

# In another terminal
curl http://localhost:8123/health
# → {"status":"ok"}
```

OpenAPI docs at <http://localhost:8123/docs>.

---

## Run the full stack in Docker

```bash
docker compose up --build
```

---

## Tests & lint

```bash
uv run pytest                 # full suite
uv run ruff check .           # lint
uv run black --check .        # format check
uv run mypy app               # type check
```

---

## Project layout

```
backend/
├── app/                      # FastAPI application
│   ├── main.py               # entrypoint
│   └── config.py             # pydantic-settings
├── tests/                    # pytest suite
├── alembic/                  # DB migrations (Phase 1+)
├── scripts/                  # one-off scripts (seed, etc.)
├── BACKEND_BUILD_PLAN.md     # strategy
├── BACKEND_BUILD_TRACKER.md  # per-phase checklist
├── agents/                   # per-phase agent briefs
├── Dockerfile
├── docker-compose.yml
└── pyproject.toml
```

---

## Acknowledgements (open-source models used)

| Capability  | Model                                        | License     | Provider                              |
| ----------- | -------------------------------------------- | ----------- | ------------------------------------- |
| Translation | `Helsinki-NLP/opus-mt-en-mul` + `opus-mt-mul-en` | CC-BY 4.0   | HF `hf-inference`                     |
| STT         | `openai/whisper-large-v3`                    | MIT         | HF `hf-inference`                     |
| TTS (en/hi) | `ResembleAI/chatterbox` (`chatterbox-turbo`) | MIT         | HF `replicate` (async predictions)    |
| TTS (te/th) | Client-side: Android `TextToSpeech` / iOS `AVSpeechSynthesizer` | OS         | on-device                             |

Pronunciation guides use [`epitran`](https://github.com/dmort27/epitran) (MIT).

---

## Manual TTS smoke test

After `uv run uvicorn app.main:app`, mint a guest token and synthesize Hindi — verifies `chatterbox-turbo` is reachable via the HF→Replicate router. Requires `HF_API_TOKEN`.

```bash
TOKEN=$(curl -s -X POST http://localhost:8123/api/v1/auth/guest | jq -r .token)

curl -s -X POST http://localhost:8123/api/v1/tts/synthesize \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"text":"नमस्ते, यह एक परीक्षण है।","language":"hi","speed":"normal"}' \
  -o /tmp/hi.wav && afplay /tmp/hi.wav   # macOS; use `aplay` on Linux
```

`language=te` and `language=th` both return `HTTP 415 {"detail":"client_side_tts_required","language":...}` — by design (see Known limitations).

---

## Known limitations

- **Telugu and Thai TTS are handled client-side**, not on the server. `POST /tts/synthesize` returns `HTTP 415 {"detail": "client_side_tts_required", "language": ...}` for both; the Android client falls back to `android.speech.tts.TextToSpeech` (iOS uses `AVSpeechSynthesizer`). Reasons: (a) HF's serverless inference dropped Parler-TTS / Indic-Parler in early 2026 and no other commercial-safe Telugu TTS is hosted on the HF router; (b) the only multilingual Thai TTS models (MMS, Seamless-M4T) are CC-BY-NC.
- HF Inference API has a free tier; expect cold-start latency on first request after idle.
- Replicate predictions are async — TTS responses include a small poll overhead (~0.5-2 s) on top of the model's render time.
