# Phase 4 — Speech-to-text

## Scope
Implement `POST /stt/transcribe`. Backed by `openai/whisper-large-v3` via HF Inference API.

## Deliverables

### 1. Service — `app/services/stt.py`
- `async def transcribe(audio_bytes: bytes, language: str | None) -> TranscriptionResult`:
  - Payload: send raw audio bytes as binary (HF accepts `application/octet-stream` for audio models).
  - If `language` given, pass `parameters={"language": <ISO-639-1>}`.
  - Returns `{transcript, language, confidence, latency_ms}`. Whisper doesn't return confidence directly — derive from `chunks` if available, else `None`.

### 2. Router — `app/routers/stt.py`
- `POST /stt/transcribe` multipart:
  - field `audio: UploadFile`
  - field `language: str | None`
- Validations:
  - 413 if file > 10 MB
  - 415 if content type not in `{audio/wav, audio/x-wav, audio/m4a, audio/mpeg, audio/mp4}`
  - 400 if duration > 30 s (peek with `soundfile.info` on a tempfile — cheap because soundfile reads headers only)
- Reads file fully into memory (capped at 10 MB), passes to service.

### 3. Audio format note
Android `MediaRecorder` defaults vary. Document in `backend/README.md` that the Android client should record AAC/M4A 16 kHz mono. Whisper accepts a wide range so this is a recommendation, not a hard requirement.

## Verification
- Unit tests:
  - oversized file → 413
  - wrong MIME → 415
  - 31 s audio → 400 (use a tiny synth fixture, `soundfile.write` of 31 s of silence to a tempfile)
  - happy path with mocked HF returns expected transcript
- Integration test (gated by `HF_API_TOKEN`): real call with a 2 s "Hello, how are you?" wav returns non-empty transcript.

## Out of scope
Diarization, word-level timestamps, real-time streaming.
