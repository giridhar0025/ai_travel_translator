# Phase 0 — Scaffolding & infra

## Scope
Stand up a minimal FastAPI project under `backend/` that boots, has `/health`, runs in Docker locally with a Postgres sidecar, and has all dev tooling wired (lint, format, test, type-check).

## Deliverables
1. `backend/pyproject.toml` — managed by `uv`. Dependencies:
   - Runtime: `fastapi`, `uvicorn[standard]`, `sqlalchemy[asyncio]>=2.0`, `asyncpg`, `alembic`, `httpx`, `pydantic-settings`, `firebase-admin`, `python-jose[cryptography]`, `python-multipart`, `epitran`, `librosa`, `soundfile`, `numpy`.
   - Dev: `pytest`, `pytest-asyncio`, `pytest-cov`, `ruff`, `black`, `mypy`, `respx` (httpx mocking), `testcontainers[postgres]`.
2. `backend/app/main.py`:
   - Creates FastAPI app with title, version from `pyproject.toml`.
   - Mounts a `/health` route returning `{"status": "ok"}`.
   - Adds CORS middleware reading `CORS_ALLOWED_ORIGINS` from config.
3. `backend/app/config.py` — `Settings(BaseSettings)` reading from `.env`. Every var from plan §8.
4. `backend/.env.example` — every variable, no real secrets.
5. `backend/Dockerfile` — multi-stage:
   - `builder`: install `uv`, run `uv sync --frozen --no-dev`.
   - `runner`: `python:3.11-slim`, copy `.venv` from builder, non-root user, `EXPOSE 8000`, `CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]`.
6. `backend/docker-compose.yml` — `db` (postgres:16-alpine, named volume) + `api` (build from Dockerfile, depends_on db, env from `.env`).
7. `backend/README.md` — quick start: `cp .env.example .env`, `uv sync`, `docker compose up db`, `uv run alembic upgrade head`, `uv run uvicorn app.main:app --reload`.
8. `backend/pyproject.toml [tool.ruff]` + `[tool.black]` + `[tool.pytest.ini_options]` configured.

## Verification
- `uv sync` succeeds with `uv.lock` committed.
- `uv run uvicorn app.main:app` → `curl localhost:8000/health` → `{"status":"ok"}`.
- `docker compose up` brings up Postgres + API.
- `uv run pytest` exits 0 on empty test suite.
- `uv run ruff check .` clean.

## Out of scope
Any routes beyond `/health`. DB models, migrations, auth — these belong to later phases.
