# Phase 9 — Testing & CI

## Scope
A test suite that runs offline (no HF, no Firebase, no internet) plus a CI workflow that gates PRs.

## Deliverables

### 1. Fixtures — `tests/conftest.py`
- `event_loop` (session-scoped, asyncio).
- `postgres_url` (session-scoped) — testcontainers Postgres if `TEST_POSTGRES=container` (the default in CI), else read `TEST_DATABASE_URL` for local fast loop.
- `db_session` (function-scoped) — transactional fixture: begin → yield → rollback.
- `app` — FastAPI app wired to the test DB.
- `client` — `httpx.AsyncClient(app=app)`.
- `fake_hf` — `respx` router intercepting `https://api-inference.huggingface.co/*` with default canned responses; tests can override.
- `fake_firebase` — monkeypatches `app.auth.firebase.verify_id_token` to return a fixed `FirebaseUser`.
- `auth_headers(user)` — issues a backend JWT and returns `{"Authorization": f"Bearer {jwt}"}`.

### 2. Unit tests (`tests/unit/`)
- One file per service: `test_translation.py`, `test_stt.py`, `test_tts.py`, `test_conversations.py`, `test_phrases.py`, `test_settings.py`, `test_auth_tokens.py`, `test_hf_client.py`.
- Each covers happy + at least one error path. Mock all external I/O.

### 3. Integration tests (`tests/integration/`)
- One file per router. Exercise the full stack against the test DB.
- Cover the auth happy path → translate → conversation flow end-to-end.

### 4. CI — `.github/workflows/ci.yml`
```yaml
name: backend-ci
on:
  pull_request:
    paths: ["backend/**"]
  push:
    branches: [main]
    paths: ["backend/**"]
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: translator_test
        ports: ["5432:5432"]
        options: >-
          --health-cmd "pg_isready -U postgres"
          --health-interval 10s --health-timeout 5s --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: astral-sh/setup-uv@v3
      - run: uv sync --frozen
        working-directory: backend
      - run: uv run ruff check .
        working-directory: backend
      - run: uv run black --check .
        working-directory: backend
      - run: uv run pytest --cov=app --cov-report=term-missing
        working-directory: backend
        env:
          TEST_DATABASE_URL: postgresql+asyncpg://postgres:postgres@localhost:5432/translator_test
          APP_JWT_SECRET: ci-secret
```

### 5. Pre-commit (optional)
- `.pre-commit-config.yaml` runs ruff + black + mypy on staged files.

## Verification
- `uv run pytest` green locally with no internet (HF + Firebase mocked).
- CI passes on a clean PR.
- Coverage ≥ 80% on `app/services/` and `app/routers/`.

## Out of scope
Load tests, contract tests against the Android client, snapshot tests.
