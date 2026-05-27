"""Service helpers for the conversations feature.

Right now this is just the date-grouping logic for the History screen — the
repo handles all data access and the router is thin. Kept separate from the
router so it can be unit-tested without spinning up FastAPI.
"""

from datetime import date, datetime, timedelta
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from app.db.models.conversation import Conversation
from app.schemas.conversation import GroupInfo


def _tz(timezone: str) -> ZoneInfo:
    try:
        return ZoneInfo(timezone)
    except ZoneInfoNotFoundError:
        return ZoneInfo("UTC")


def _label_for(local_d: date, today: date, yesterday: date) -> str:
    if local_d == today:
        return "Today"
    if local_d == yesterday:
        return "Yesterday"
    return local_d.isoformat()


def date_groups(
    conversations: list[Conversation],
    *,
    user_timezone: str = "UTC",
    now: datetime | None = None,
) -> list[GroupInfo]:
    """Compute `[{label, count}]` buckets for a History list, in user-local TZ.

    Order is preserved from the input list — the caller is expected to have
    already sorted conversations by `last_message_at DESC`.
    """
    tz = _tz(user_timezone)
    now_local = (now or datetime.now(tz)).astimezone(tz)
    today = now_local.date()
    yesterday = today - timedelta(days=1)

    groups: list[GroupInfo] = []
    for conv in conversations:
        local_d = conv.last_message_at.astimezone(tz).date()
        label = _label_for(local_d, today, yesterday)
        if groups and groups[-1].label == label:
            groups[-1] = GroupInfo(label=label, count=groups[-1].count + 1)
        else:
            groups.append(GroupInfo(label=label, count=1))
    return groups
