"""Background helper to record a translation into the `recents` table.

Opens a fresh session because the request session is closed by the time
BackgroundTasks runs the callable.
"""

from datetime import UTC, datetime, timedelta
from uuid import UUID

from sqlalchemy import select

from app.db.models.favorite import Recent
from app.db.session import AsyncSessionLocal

DEDUP_WINDOW = timedelta(hours=24)


async def insert_recent(
    user_id: UUID,
    source_lang: str,
    target_lang: str,
    source_text: str,
    translated_text: str,
) -> None:
    cutoff = datetime.now(tz=UTC) - DEDUP_WINDOW
    async with AsyncSessionLocal() as session, session.begin():
        existing = await session.scalar(
            select(Recent)
            .where(
                Recent.user_id == user_id,
                Recent.source_text == source_text,
                Recent.target_lang == target_lang,
                Recent.created_at >= cutoff,
            )
            .order_by(Recent.created_at.desc())
            .limit(1)
        )
        if existing is not None:
            existing.created_at = datetime.now(tz=UTC)
            existing.translated_text = translated_text
            return
        session.add(
            Recent(
                user_id=user_id,
                source_text=source_text,
                source_lang=source_lang,
                translated_text=translated_text,
                target_lang=target_lang,
            )
        )
