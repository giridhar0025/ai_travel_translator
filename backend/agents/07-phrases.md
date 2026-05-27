# Phase 7 — Phrases library

## Scope
Serve the 5 categories + 30 starter phrases pre-translated to the requested target language. Translations are lazily computed on first request and persisted to `phrase_translations`.

## Deliverables

### 1. Repository — `app/repositories/phrases.py`
- `list_categories() -> [PhraseCategory]` (ordered by `sort_order`).
- `list_by_category(slug) -> [Phrase]` (ordered by `sort_order`).
- `get_translation(phrase_id, target_lang) -> PhraseTranslation | None`.
- `upsert_translation(phrase_id, target_lang, translation, pronunciation)`.

### 2. Service — `app/services/phrases.py`
- `async def get_phrases(category_slug, target_lang) -> list[PhraseWithTranslation]`:
  1. Load phrases for category.
  2. For each, look up cached translation in `phrase_translations`.
  3. For misses, batch-call `TranslationService.translate_batch(items=[phrase.source_text for cache misses], source=phrase.source_lang, target=target_lang)`.
  4. Persist results.
  5. Return combined list in `Phrase.sort_order`.

### 3. Router — `app/routers/phrases.py`
- `GET /phrases/categories` → `[PhraseCategory]`.
- `GET /phrases?category={slug}&target={lang}` → `[PhraseWithTranslation]`.
- 404 if category slug unknown. 400 if `target` unsupported.

## Verification
- Seed populates 5 categories × 6 phrases (en source).
- First call to `GET /phrases?category=restaurants&target=hi` triggers 1 HF batch call for 6 phrases, caches all 6 rows in `phrase_translations`.
- Second call: no HF call (asserted via mocked `HuggingFaceClient`).
- Switching to `target=te` triggers one more batch call.

## Out of scope
User-created phrases, custom categories, phrase reordering.
