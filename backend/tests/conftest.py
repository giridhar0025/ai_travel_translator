"""Test fixtures + env hooks.

Runs before any `app.*` import. If `TEST_DATABASE_URL` is set (e.g. in CI),
we copy it into `DATABASE_URL` so `app.config.Settings` picks up the test DB
without us touching the developer's `.env`.

External I/O is mocked per-test rather than via shared fixtures:
- HF: `respx` blocks at the route level (see `tests/integration/test_*_route.py`).
- Firebase: `unittest.mock.patch` on `app.routers.auth.verify_id_token`.

Tests never need internet.
"""

from __future__ import annotations

import os

# Must run before any `from app...` import below so `Settings()` reads the right URL.
if test_db_url := os.environ.get("TEST_DATABASE_URL"):
    os.environ["DATABASE_URL"] = test_db_url

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)
