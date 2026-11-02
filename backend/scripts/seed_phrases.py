"""Seed languages, phrase categories, and starter phrases. Idempotent.

Run with:
    uv run python -m scripts.seed_phrases
"""

import asyncio

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.language import Language
from app.db.models.phrase import Phrase, PhraseCategory
from app.db.session import AsyncSessionLocal, engine
from app.services.translation import LANGUAGE_NAMES

# Must cover every code in app.services.translation.LANGUAGE_NAMES — recents,
# favorites and messages carry FK constraints to languages.code, so an unseeded
# language breaks the first translation that tries to record a recent.
# `tts_supported` here means *server-side* TTS (SERVER_TTS_LANGS); everything
# else uses the client's on-device engine.
_LANGUAGE_ROWS: list[tuple[str, str, str, bool]] = [
    # (code, name, flag_emoji, server_tts)
    ("en", "English", "🇬🇧", True),
    ("hi", "Hindi", "🇮🇳", True),
    ("te", "Telugu", "🇮🇳", False),
    ("ta", "Tamil", "🇮🇳", False),
    ("bn", "Bengali", "🇧🇩", False),
    ("mr", "Marathi", "🇮🇳", False),
    ("th", "Thai", "🇹🇭", False),
    ("es", "Spanish", "🇪🇸", False),
    ("fr", "French", "🇫🇷", False),
    ("de", "German", "🇩🇪", False),
    ("it", "Italian", "🇮🇹", False),
    ("pt", "Portuguese", "🇵🇹", False),
    ("ja", "Japanese", "🇯🇵", False),
    ("ko", "Korean", "🇰🇷", False),
    ("zh", "Chinese", "🇨🇳", False),
    ("ar", "Arabic", "🇸🇦", False),
    ("ru", "Russian", "🇷🇺", False),
    ("vi", "Vietnamese", "🇻🇳", False),
    ("id", "Indonesian", "🇮🇩", False),
    ("tr", "Turkish", "🇹🇷", False),
]

LANGUAGES: list[dict[str, object]] = [
    {
        "code": code,
        "name": name,
        "flag_emoji": flag,
        "stt_supported": True,
        "tts_supported": server_tts,
        "translate_supported": True,
    }
    for code, name, flag, server_tts in _LANGUAGE_ROWS
]

# Fail loudly at import if the seed drifts from the translation service's list.
assert {row["code"] for row in LANGUAGES} == set(
    LANGUAGE_NAMES
), "seed_phrases LANGUAGES out of sync with translation.LANGUAGE_NAMES"

CATEGORIES: list[dict[str, object]] = [
    {
        "slug": "restaurants",
        "name": "Restaurants",
        "icon": "🍴",
        "color": "#F59E0B",
        "sort_order": 1,
    },
    {"slug": "directions", "name": "Directions", "icon": "🧭", "color": "#3B82F6", "sort_order": 2},
    {"slug": "transport", "name": "Transport", "icon": "🚗", "color": "#22C55E", "sort_order": 3},
    {"slug": "shopping", "name": "Shopping", "icon": "🛍️", "color": "#A855F7", "sort_order": 4},
    {
        "slug": "emergencies",
        "name": "Emergencies",
        "icon": "⚠️",
        "color": "#EF4444",
        "sort_order": 5,
    },
    {
        "slug": "medical",
        "name": "Medical",
        "icon": "🏥",
        "color": "#0EA5E9",
        "sort_order": 6,
    },
]

PHRASES: dict[str, list[str]] = {
    "restaurants": [
        "Table for two, please",
        "Can I see the menu?",
        "The bill, please",
        "I'm vegetarian",
        "What do you recommend?",
        "Could I have some water?",
    ],
    "directions": [
        "Where is the nearest subway?",
        "How do I get to the city center?",
        "Is it within walking distance?",
        "Should I turn left here?",
        "Where is the bathroom?",
        "Can you show me on a map?",
    ],
    "transport": [
        "One ticket, please",
        "Does this bus go to the airport?",
        "What time does the train leave?",
        "How much is a taxi to downtown?",
        "Where is the bus stop?",
        "I'd like to get off here",
    ],
    "shopping": [
        "How much does this cost?",
        "Can I try this on?",
        "Do you accept credit cards?",
        "Do you have a smaller size?",
        "I'm just looking, thank you",
        "Can I get a discount?",
    ],
    "emergencies": [
        "I need help",
        "Where is the hospital?",
        "Please call the police",
        "I lost my passport",
        "I'm hurt",
        "Is there a pharmacy nearby?",
    ],
    "medical": [
        "I need a doctor",
        "Where is the pharmacy?",
        "I'm allergic to peanuts",
        "I have a medical condition",
        "I need medicine",
        "I am deaf or hard of hearing",
        "Please speak slowly",
    ],
}


async def seed_languages(session: AsyncSession) -> int:
    stmt = pg_insert(Language).values(LANGUAGES)
    stmt = stmt.on_conflict_do_update(
        index_elements=["code"],
        set_={
            "name": stmt.excluded.name,
            "flag_emoji": stmt.excluded.flag_emoji,
            "stt_supported": stmt.excluded.stt_supported,
            "tts_supported": stmt.excluded.tts_supported,
            "translate_supported": stmt.excluded.translate_supported,
        },
    )
    await session.execute(stmt)
    return len(LANGUAGES)


async def seed_categories(session: AsyncSession) -> int:
    stmt = pg_insert(PhraseCategory).values(CATEGORIES)
    stmt = stmt.on_conflict_do_update(
        index_elements=["slug"],
        set_={
            "name": stmt.excluded.name,
            "icon": stmt.excluded.icon,
            "color": stmt.excluded.color,
            "sort_order": stmt.excluded.sort_order,
        },
    )
    await session.execute(stmt)
    return len(CATEGORIES)


async def seed_phrases(session: AsyncSession) -> int:
    inserted = 0
    for slug, phrases in PHRASES.items():
        for order, text in enumerate(phrases, start=1):
            existing = await session.scalar(
                select(Phrase.id).where(
                    Phrase.category_slug == slug,
                    Phrase.source_text == text,
                )
            )
            if existing is not None:
                continue
            session.add(
                Phrase(
                    category_slug=slug,
                    source_lang="en",
                    source_text=text,
                    sort_order=order,
                )
            )
            inserted += 1
    return inserted


async def run() -> None:
    async with AsyncSessionLocal() as session:
        languages = await seed_languages(session)
        categories = await seed_categories(session)
        phrases = await seed_phrases(session)
        await session.commit()
        print(f"Seeded {languages} languages, {categories} categories, {phrases} new phrases.")
    await engine.dispose()


if __name__ == "__main__":
    asyncio.run(run())
