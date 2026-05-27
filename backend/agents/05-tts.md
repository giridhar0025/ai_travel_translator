# Phase 5 — Text-to-speech

## Scope
Implement `POST /tts/synthesize`. Backed by the **Parler-TTS family** via HF Inference API (Apache 2.0 — commercial-safe). Supports normal and slow playback. **Thai is handled client-side** because no commercial-safe Thai TTS model exists in open weights.

## Deliverables

### 1. Model + voice registry — `app/services/tts.py`
```python
# Each entry: (hf_model, voice_description)
TTS_REGISTRY: dict[str, tuple[str, str]] = {
    "en": (
        settings.hf_model_tts_en,   # parler-tts/parler-tts-mini-v1
        "A clear female voice with neutral tone, moderate pace, studio-quality recording.",
    ),
    "hi": (
        settings.hf_model_tts_hi,   # ai4bharat/indic-parler-tts
        "A clear female voice speaking Hindi with neutral tone and moderate pace.",
    ),
    "te": (
        settings.hf_model_tts_te,   # ai4bharat/indic-parler-tts
        "A clear female voice speaking Telugu with neutral tone and moderate pace.",
    ),
    # "th" deliberately absent — see §4.
}
```

### 2. Service
- `async def synthesize(text: str, language: str, speed: Literal["normal", "slow"]) -> bytes`:
  - If `language == "th"` → raise `ClientSideTtsRequired` (router maps to 415).
  - If language not in `TTS_REGISTRY` → raise `UnsupportedLanguageError` (router maps to 415).
  - Payload (Parler format): `{"inputs": text, "parameters": {"description": <voice description>}}`.
  - Parler returns raw audio bytes (`audio/wav`, mono, 24 kHz).
  - If `speed=="slow"`:
    - Decode via `soundfile.read(io.BytesIO(audio))` → numpy array.
    - `librosa.effects.time_stretch(y, rate=0.7)` (pitch preserved).
    - Re-encode via `soundfile.write(..., format="WAV", subtype="PCM_16")`.
  - Returns final WAV bytes.

### 3. Router — `app/routers/tts.py`
- `POST /tts/synthesize` body `{text, language, speed}` → `StreamingResponse(content, media_type="audio/wav")`.
- 400 if `text` > 500 chars or empty.
- **415 with body `{"detail": "client_side_tts_required", "language": "th"}` for Thai** — the Android client recognises this code and uses `android.speech.tts.TextToSpeech` locally.
- 415 with body `{"detail": "unsupported_language"}` for anything else not in the registry.

### 4. Thai TTS — client-side fallback (documented contract)

The only multilingual open-weight TTS models that include Thai (`facebook/mms-tts-tha`, `facebook/seamless-m4t-v2-large`) are CC-BY-NC. Until a permissively-licensed Thai TTS model lands, the backend declines and the client handles it on-device.

**Server contract (this phase):**
```json
HTTP/1.1 415 Unsupported Media Type
Content-Type: application/json

{"detail": "client_side_tts_required", "language": "th"}
```

**Android contract (separate ticket in the Android tracker, not this phase):**
- On 415 with `detail == "client_side_tts_required"`, instantiate `TextToSpeech(context, listener)`, set locale `Locale.forLanguageTag("th-TH")`, and `speak(text)`.
- Document this in `backend/README.md` under "Known limitations".

### 5. Telugu validation (critical)
The mobile plan §6 flags Telugu support as platform-dependent. This phase validates the backend can produce Telugu TTS via `ai4bharat/indic-parler-tts` with `language=telugu` in the description. Manual smoke test in `backend/README.md`: call `/tts/synthesize` with `language=te, text=నమస్కారం` and play the result.

## Verification
- Unit tests:
  - language=`fr` → 415 `unsupported_language`
  - language=`th` → 415 `client_side_tts_required`
  - empty text → 400
  - mocked HF: en hits Parler-mini URL, hi/te both hit Indic-Parler URL with the correct description
- Integration test (gated by `HF_API_TOKEN`): real call for each of en/hi/te returns valid WAV (assert `soundfile.info` parses it and duration > 0).
- Manual Telugu test passes audibly.

## License acknowledgements (add to `backend/README.md`)
- `parler-tts/parler-tts-mini-v1` — Apache 2.0
- `ai4bharat/indic-parler-tts` — Apache 2.0
- Android `TextToSpeech` for Thai — Android OS component, no additional licensing for app distribution.

## Out of scope
Voice cloning, pitch shift, custom voices, SSML, server-side Thai TTS.
