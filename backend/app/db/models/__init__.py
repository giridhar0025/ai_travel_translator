"""Import every model here so Alembic's autogenerate sees them."""

from app.db.models.conversation import Conversation, Message
from app.db.models.favorite import Favorite, Recent
from app.db.models.language import Language
from app.db.models.phrase import Phrase, PhraseCategory, PhraseTranslation
from app.db.models.user import User, UserSettings

__all__ = [
    "Conversation",
    "Favorite",
    "Language",
    "Message",
    "Phrase",
    "PhraseCategory",
    "PhraseTranslation",
    "Recent",
    "User",
    "UserSettings",
]
