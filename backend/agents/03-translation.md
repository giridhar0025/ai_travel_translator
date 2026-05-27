# Phase 3 — Translation service

## Scope
Implement `/translate`, `/translate/batch`, `/languages`. Translation backed by **`facebook/m2m100_418M`** via HF Inference API (MIT license — commercial-safe). Pronunciation guide is derived per-language.

## Deliverables

### 1. Shared HF client — `app/services/hf_client.py`
```python
class HuggingFaceClient:
    def __init__(self, token: str, timeout: float = 30.0):
        self._client = httpx.AsyncClient(
            base_url="https://api-inference.huggingface.co",
            headers={"Authorization": f"Bearer {token}"},
            timeout=timeout,
            http2=True,
        )

    async def infer(self, model: str, payload: dict | bytes, *, raw: bool = False) -> Any:
        # 3 retries on 503 (HF cold start) with exponential backoff 1s, 2s, 4s
        ...
```
- 503 retry loop is the only retry — other errors propagate as `HFInferenceError`.
- Singleton via FastAPI lifespan + dependency.

### 2. Translation service — `app/services/translation.py`
- **No language code remapping needed** — m2m100 uses ISO 639-1 (`en`, `hi`, `te`, `th`) directly. Validate the code is in the supported set and pass through.
- `async def translate(source: str, target: str, text: str, context: str | None) -> TranslationResult`:
  - If `context` provided, prepend as `"[Context: {context}] {text}"` (m2m100 doesn't honor system prompts but the extra signal can improve robustness).
  - Payload: `{"inputs": text, "parameters": {"src_lang": source, "tgt_lang": target}}`
  - Returns `{translation, pronunciation, latency_ms, model}`.
- `pronunciation`:
  - `en` → return `None` (Latin script).
  - `hi`, `te`, `th` → `epitran` IPA transliteration. If `epitran` raises (unsupported chars), fall back to `unidecode` romanization.
- LRU cache via `functools.lru_cache(maxsize=1000)` keyed on `(source, target, text, context)`.

### 3. Router — `app/routers/translate.py`
- `POST /translate` body `{source, target, text, context?}` → `TranslationResult`. 400 if `source==target` or either lang unsupported.
- `POST /translate/batch` body `{source, target, items: [str]}` → `{results: [...]}`. Items run via `asyncio.gather` with a semaphore (max 5 concurrent HF calls).
- `GET /languages` reads from `languages` table, returns the registry.

### 4. Recents side-effect
- On `/translate` success, enqueue an "insert recent" task via `BackgroundTasks` (after-response). Don't block translation latency on the DB write.

## Verification
- Unit tests with `respx` mocking HF endpoint:
  - en→hi happy path
  - source==target → 400
  - HF 503 → retried 3× then 502 surfaced
- Integration test (skipped unless `HF_API_TOKEN` set in env):
  - Real call to m2m100 returns non-empty translation for "Hello" → hi, te, th.
- License acknowledgement note in `backend/README.md`: m2m100 = MIT.
- Latency P50 < 2 s in dev when HF endpoint is warm.

## Out of scope
Quality eval, alternates/synonyms, streaming. Pronunciation TTS is Phase 5.
