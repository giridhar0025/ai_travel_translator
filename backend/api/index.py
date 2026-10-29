"""Vercel Python function entrypoint.

Vercel looks for a top-level ``app`` object. The FastAPI instance already
defines every API route under ``/api/v1`` plus ``/health``. Vercel passes the
original request path through to ASGI, so the route prefixes must stay as-is.
"""

from app.main import app

__all__ = ["app"]
