from datetime import UTC, datetime, timedelta
from uuid import uuid4

import pytest
from jose import jwt

from app.auth.errors import ExpiredTokenError, InvalidTokenError
from app.auth.tokens import ALGORITHM, ISSUER, issue_token, verify_token
from app.config import get_settings


def test_issue_and_verify_roundtrip() -> None:
    user_id = uuid4()
    token = issue_token(user_id, is_guest=False)
    payload = verify_token(token)
    assert payload.user_id == user_id
    assert payload.is_guest is False


def test_guest_flag_preserved() -> None:
    user_id = uuid4()
    token = issue_token(user_id, is_guest=True)
    payload = verify_token(token)
    assert payload.is_guest is True


def test_expired_token_raises() -> None:
    settings = get_settings()
    past = datetime.now(tz=UTC) - timedelta(hours=1)
    token = jwt.encode(
        {
            "sub": str(uuid4()),
            "is_guest": False,
            "iat": int(past.timestamp()) - 60,
            "exp": int(past.timestamp()),
            "iss": ISSUER,
        },
        settings.app_jwt_secret,
        algorithm=ALGORITHM,
    )
    with pytest.raises(ExpiredTokenError):
        verify_token(token)


def test_wrong_signature_raises() -> None:
    token = jwt.encode(
        {
            "sub": str(uuid4()),
            "iss": ISSUER,
            "iat": 0,
            "exp": 9999999999,
        },
        "different-secret",
        algorithm=ALGORITHM,
    )
    with pytest.raises(InvalidTokenError):
        verify_token(token)


def test_wrong_issuer_raises() -> None:
    settings = get_settings()
    token = jwt.encode(
        {
            "sub": str(uuid4()),
            "iss": "some-other-issuer",
            "iat": 0,
            "exp": 9999999999,
        },
        settings.app_jwt_secret,
        algorithm=ALGORITHM,
    )
    with pytest.raises(InvalidTokenError):
        verify_token(token)


def test_malformed_sub_raises() -> None:
    settings = get_settings()
    token = jwt.encode(
        {
            "sub": "not-a-uuid",
            "iss": ISSUER,
            "iat": 0,
            "exp": 9999999999,
        },
        settings.app_jwt_secret,
        algorithm=ALGORITHM,
    )
    with pytest.raises(InvalidTokenError):
        verify_token(token)
