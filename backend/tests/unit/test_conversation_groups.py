from datetime import UTC, datetime, timedelta
from types import SimpleNamespace

from app.services.conversations import date_groups


def _conv(when: datetime) -> SimpleNamespace:
    """Stand-in for a Conversation row — date_groups only reads
    `last_message_at`, so a SimpleNamespace is enough."""
    return SimpleNamespace(last_message_at=when)


def test_date_groups_today_yesterday_older() -> None:
    now = datetime(2026, 5, 26, 15, 30, tzinfo=UTC)
    convos = [
        _conv(now),  # Today
        _conv(now - timedelta(hours=1)),  # Today
        _conv(now - timedelta(days=1, hours=2)),  # Yesterday
        _conv(now - timedelta(days=4)),  # ISO date
    ]
    groups = date_groups(convos, user_timezone="UTC", now=now)
    assert [(g.label, g.count) for g in groups] == [
        ("Today", 2),
        ("Yesterday", 1),
        ("2026-05-22", 1),
    ]


def test_date_groups_respect_user_timezone() -> None:
    # 2026-05-26 03:00 UTC is 2026-05-25 23:00 in America/New_York (UTC-4 in May).
    # That same instant should fall in "Yesterday" for an NYC user even though
    # it's still "Today" in UTC.
    instant = datetime(2026, 5, 26, 3, 0, tzinfo=UTC)
    now = datetime(2026, 5, 26, 5, 0, tzinfo=UTC)  # 01:00 EDT — today in NYC
    groups = date_groups([_conv(instant)], user_timezone="America/New_York", now=now)
    assert groups[0].label == "Yesterday"
    assert groups[0].count == 1


def test_date_groups_unknown_tz_falls_back_to_utc() -> None:
    now = datetime(2026, 5, 26, 15, 30, tzinfo=UTC)
    groups = date_groups([_conv(now)], user_timezone="Not/A/Real/Zone", now=now)
    assert groups[0].label == "Today"


def test_date_groups_empty() -> None:
    assert date_groups([], user_timezone="UTC") == []
