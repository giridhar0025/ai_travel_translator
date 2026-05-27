from unittest.mock import patch

import pytest

from app.auth.errors import FirebaseVerificationError
from app.auth.firebase import FirebaseUser, verify_id_token


@pytest.mark.asyncio
async def test_verify_id_token_returns_firebase_user() -> None:
    fake_claims = {
        "uid": "abc123",
        "email": "user@example.com",
        "name": "Test User",
        "picture": "https://example.com/a.png",
        "firebase": {"sign_in_provider": "google.com"},
    }
    with (
        patch("app.auth.firebase._verify_sync", return_value=fake_claims),
        patch("app.auth.firebase._ensure_initialized"),
    ):
        fb_user = await verify_id_token("fake-token")

    assert isinstance(fb_user, FirebaseUser)
    assert fb_user.uid == "abc123"
    assert fb_user.email == "user@example.com"
    assert fb_user.name == "Test User"
    assert fb_user.picture == "https://example.com/a.png"
    assert fb_user.provider == "google.com"


@pytest.mark.asyncio
async def test_verify_id_token_raises_on_firebase_error() -> None:
    with (
        patch("app.auth.firebase._verify_sync", side_effect=ValueError("bad token")),
        patch("app.auth.firebase._ensure_initialized"),
        pytest.raises(FirebaseVerificationError),
    ):
        await verify_id_token("garbage")


@pytest.mark.asyncio
async def test_verify_id_token_handles_missing_optional_claims() -> None:
    fake_claims = {"uid": "abc123"}  # no email, name, picture, firebase
    with (
        patch("app.auth.firebase._verify_sync", return_value=fake_claims),
        patch("app.auth.firebase._ensure_initialized"),
    ):
        fb_user = await verify_id_token("fake-token")

    assert fb_user.uid == "abc123"
    assert fb_user.email is None
    assert fb_user.name is None
    assert fb_user.picture is None
    assert fb_user.provider is None
