"""Integration tests for `GET /recents` and the recents background writer.

These tests exercise `insert_recent` directly (the same path `/translate` uses
via BackgroundTasks) so we don't need to mock HF here.
"""

from __future__ import annotations

import asyncio
from collections.abc import Iterator
from datetime import UTC, datetime, timedelta
from uuid import UUID

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.db.models.favorite import Recent
from app.db.session import AsyncSessionLocal
from app.services.recents import insert_recent


@pytest.fixture
def auth(client: TestClient) -> Iterator[tuple[dict[str, str], UUID]]:
    body = client.post("/api/v1/auth/guest").json()
    h = {"Authorization": f"Bearer {body['token']}"}
    uid = UUID(body["user"]["id"])
    try:
        yield h, uid
    finally:
        client.delete("/api/v1/me", headers=h)


def _insert(user_id: UUID, source: str, translated: str, target: str = "hi") -> None:
    asyncio.run(
        insert_recent(
            user_id=user_id,
            source_lang="en",
            target_lang=target,
            source_text=source,
            translated_text=translated,
        )
    )


def test_recents_require_auth(client: TestClient) -> None:
    assert client.get("/api/v1/recents").status_code == 401


def test_recents_empty_for_new_user(client: TestClient, auth: tuple[dict[str, str], UUID]) -> None:
    headers, _ = auth
    r = client.get("/api/v1/recents", headers=headers)
    assert r.status_code == 200
    assert r.json() == []


def test_recents_dedup_within_24h_window(
    client: TestClient, auth: tuple[dict[str, str], UUID]
) -> None:
    headers, user_id = auth

    _insert(user_id, "Where is the hospital?", "अस्पताल कहाँ है?")
    _insert(user_id, "Where is the hospital?", "अस्पताल कहाँ है?")  # dup

    rows = client.get("/api/v1/recents", headers=headers).json()
    assert len(rows) == 1
    assert rows[0]["source_text"] == "Where is the hospital?"


def test_recents_returns_distinct_pairs_newest_first(
    client: TestClient, auth: tuple[dict[str, str], UUID]
) -> None:
    headers, user_id = auth

    _insert(user_id, "Hello", "नमस्ते")
    _insert(user_id, "Thank you", "धन्यवाद")
    _insert(user_id, "Goodbye", "अलविदा")

    rows = client.get("/api/v1/recents", headers=headers).json()
    assert [r["source_text"] for r in rows] == ["Goodbye", "Thank you", "Hello"]


def test_recents_dedup_keeps_newest_translation_for_old_duplicates(
    client: TestClient, auth: tuple[dict[str, str], UUID]
) -> None:
    """If a duplicate slipped past the 24h window, the read-side dedup must
    still collapse the pair and return the newest row."""
    headers, user_id = auth

    # First, insert an "old" row directly with created_at backdated past the
    # 24h dedup window — so the next insert_recent treats it as a fresh row.
    async def setup() -> None:
        async with AsyncSessionLocal() as session, session.begin():
            session.add(
                Recent(
                    user_id=user_id,
                    source_text="Good morning",
                    source_lang="en",
                    translated_text="OLD",
                    target_lang="hi",
                    created_at=datetime.now(tz=UTC) - timedelta(days=2),
                )
            )

    asyncio.run(setup())
    _insert(user_id, "Good morning", "NEW")

    # DB now has two rows for the same (source_text, target_lang)
    async def count_rows() -> int:
        async with AsyncSessionLocal() as session:
            result = await session.execute(
                select(Recent).where(
                    Recent.user_id == user_id,
                    Recent.source_text == "Good morning",
                )
            )
            return len(list(result.scalars().all()))

    assert asyncio.run(count_rows()) == 2

    rows = client.get("/api/v1/recents", headers=headers).json()
    assert len(rows) == 1
    assert rows[0]["translated_text"] == "NEW"


def test_recents_respects_limit_query_param(
    client: TestClient, auth: tuple[dict[str, str], UUID]
) -> None:
    headers, user_id = auth
    for i in range(5):
        _insert(user_id, f"phrase {i}", f"translation {i}")

    rows = client.get("/api/v1/recents?limit=2", headers=headers).json()
    assert len(rows) == 2
