from uuid import UUID

from pydantic import BaseModel, ConfigDict


class PhraseCategorySchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    slug: str
    name: str
    icon: str
    color: str
    sort_order: int


class PhraseWithTranslation(BaseModel):
    """A phrase rendered for the History/Quick-phrases screens. `translation`
    is the source text itself when `target_lang == source_lang` (no HF hop)."""

    id: UUID
    category_slug: str
    source_lang: str
    source_text: str
    target_lang: str
    translation: str
    pronunciation: str | None
    sort_order: int
