from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from uuid import UUID

from jose import jwt
from jose.exceptions import ExpiredSignatureError, JWTError

from app.auth.errors import ExpiredTokenError, InvalidTokenError
from app.config import get_settings

ALGORITHM = "HS256"
ISSUER = "travel-translator-api"


@dataclass(frozen=True)
class TokenPayload:
    user_id: UUID
    is_guest: bool


def issue_token(user_id: UUID, *, is_guest: bool) -> str:
    settings = get_settings()
    now = datetime.now(tz=UTC)
    payload = {
        "sub": str(user_id),
        "is_guest": is_guest,
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(hours=settings.app_jwt_expiry_hours)).timestamp()),
        "iss": ISSUER,
    }
    return jwt.encode(payload, settings.app_jwt_secret, algorithm=ALGORITHM)


def verify_token(token: str) -> TokenPayload:
    settings = get_settings()
    try:
        claims = jwt.decode(
            token,
            settings.app_jwt_secret,
            algorithms=[ALGORITHM],
            issuer=ISSUER,
        )
    except ExpiredSignatureError as e:
        raise ExpiredTokenError("token expired") from e
    except JWTError as e:
        raise InvalidTokenError(str(e)) from e
    try:
        user_id = UUID(claims["sub"])
    except (KeyError, ValueError) as e:
        raise InvalidTokenError("invalid sub claim") from e
    return TokenPayload(user_id=user_id, is_guest=bool(claims.get("is_guest", False)))
