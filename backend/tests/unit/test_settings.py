"""Unit tests for the settings schema and the repository's get_or_create.

The router itself (auth + commit semantics) is covered by
`tests/integration/test_settings_routes.py`.
"""

from __future__ import annotations

import pytest
from pydantic import ValidationError

from app.schemas.user import UserSettingsPatch

# ---------- UserSettingsPatch validation ----------


def test_patch_accepts_empty_body() -> None:
    """All fields optional — empty body must validate to all-None."""
    p = UserSettingsPatch()
    assert p.default_language is None
    assert p.playback_speed is None
    assert p.offline_enabled is None


def test_patch_accepts_each_valid_playback_speed() -> None:
    for speed in ("normal", "slow", "very_slow"):
        assert UserSettingsPatch(playback_speed=speed).playback_speed == speed


def test_patch_rejects_unknown_playback_speed() -> None:
    with pytest.raises(ValidationError) as exc:
        UserSettingsPatch(playback_speed="blazing")
    assert "playback_speed" in str(exc.value)


def test_patch_rejects_too_short_default_language() -> None:
    # Field constraint: min_length=2 — protects against `""` or `"x"` slipping
    # through before the registry lookup runs.
    with pytest.raises(ValidationError):
        UserSettingsPatch(default_language="x")


def test_patch_rejects_too_long_default_language() -> None:
    with pytest.raises(ValidationError):
        UserSettingsPatch(default_language="a" * 9)


def test_patch_accepts_partial_combinations() -> None:
    p = UserSettingsPatch(default_language="hi", offline_enabled=True)
    assert p.default_language == "hi"
    assert p.offline_enabled is True
    assert p.playback_speed is None


# ---------- UserSettingsRepo.get_or_create ----------
#
# These tests use the live DB session to verify the repo's idempotent behavior
# without going through HTTP. Marked async so pytest-asyncio drives them.


@pytest.mark.asyncio
async def test_get_or_create_returns_existing_row_without_duplicating() -> None:
    """Two calls in the same session must return the same row, not create
    a second one — the unique PK on `user_id` would 500 otherwise."""
    from app.db.models.user import User
    from app.db.session import AsyncSessionLocal
    from app.repositories.user import UserRepo, UserSettingsRepo

    async with AsyncSessionLocal() as session, session.begin():
        user = User(is_guest=True)
        await UserRepo(session).add(user)
        repo = UserSettingsRepo(session)
        first = await repo.get_or_create(user.id)
        second = await repo.get_or_create(user.id)
        assert first.user_id == second.user_id == user.id
        assert first.default_language == "en"
        assert first.playback_speed == "normal"
        assert first.offline_enabled is False
        # Cleanup happens via session.begin() rolling back at __aexit__ if we
        # raise; commit the deletion explicitly so we don't leak rows.
        await session.delete(user)
