"""Smoke tests for `Settings.database_url` normalization.

Railway's Postgres plugin emits `postgres://user:pass@host/db` (sync scheme).
SQLAlchemy's async engine needs `postgresql+asyncpg://...`. The validator on
`Settings.database_url` translates both `postgres://` and bare `postgresql://`
into the async form so the deploy doesn't fail on first cold start.
"""

from __future__ import annotations

from app.config import Settings


def test_postgres_scheme_is_rewritten_to_asyncpg() -> None:
    s = Settings(database_url="postgres://u:p@host:5432/db")  # type: ignore[call-arg]
    assert s.database_url == "postgresql+asyncpg://u:p@host:5432/db"


def test_postgresql_scheme_without_driver_is_rewritten() -> None:
    s = Settings(database_url="postgresql://u:p@host:5432/db")  # type: ignore[call-arg]
    assert s.database_url == "postgresql+asyncpg://u:p@host:5432/db"


def test_asyncpg_scheme_is_left_alone() -> None:
    url = "postgresql+asyncpg://u:p@host:5432/db"
    s = Settings(database_url=url)  # type: ignore[call-arg]
    assert s.database_url == url


def test_psycopg_scheme_is_left_alone() -> None:
    # An explicit non-asyncpg driver (e.g. used by alembic sync runs) stays put.
    url = "postgresql+psycopg://u:p@host:5432/db"
    s = Settings(database_url=url)  # type: ignore[call-arg]
    assert s.database_url == url
