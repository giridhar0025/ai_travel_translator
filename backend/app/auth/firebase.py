import asyncio
import base64
import json
from dataclasses import dataclass
from functools import lru_cache
from typing import Any

import firebase_admin
from firebase_admin import auth as fb_auth
from firebase_admin import credentials

from app.auth.errors import FirebaseVerificationError
from app.config import get_settings


@dataclass(frozen=True)
class FirebaseUser:
    uid: str
    email: str | None
    name: str | None
    picture: str | None
    provider: str | None


@lru_cache(maxsize=1)
def _ensure_initialized() -> None:
    if firebase_admin._apps:  # pragma: no cover  (depends on global state)
        return
    settings = get_settings()
    if settings.firebase_service_account_json:
        decoded = base64.b64decode(settings.firebase_service_account_json)
        service_account_dict = json.loads(decoded)
        cred = credentials.Certificate(service_account_dict)
        firebase_admin.initialize_app(cred, options={"projectId": settings.firebase_project_id})
    else:
        # Permits initialization without a key for offline tests; verify_id_token will then fail.
        firebase_admin.initialize_app(options={"projectId": settings.firebase_project_id})


def _verify_sync(id_token: str) -> dict[str, Any]:
    return fb_auth.verify_id_token(id_token, check_revoked=False)


async def verify_id_token(id_token: str) -> FirebaseUser:
    _ensure_initialized()
    try:
        claims = await asyncio.to_thread(_verify_sync, id_token)
    except Exception as e:
        raise FirebaseVerificationError(str(e)) from e
    return FirebaseUser(
        uid=claims["uid"],
        email=claims.get("email"),
        name=claims.get("name"),
        picture=claims.get("picture"),
        provider=(claims.get("firebase") or {}).get("sign_in_provider"),
    )
