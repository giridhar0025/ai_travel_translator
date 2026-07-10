from fastapi.testclient import TestClient

from app.services.translation import LANGUAGE_NAMES
from app.services.tts import SERVER_TTS_LANGS


def test_list_languages_returns_seeded_rows(client: TestClient) -> None:
    r = client.get("/api/v1/languages")
    assert r.status_code == 200
    langs = r.json()
    codes = sorted(item["code"] for item in langs)
    assert codes == sorted(LANGUAGE_NAMES)

    by_code = {item["code"]: item for item in langs}
    # tts_supported means *server-side* TTS; everything else is on-device.
    for code, item in by_code.items():
        assert item["tts_supported"] is (code in SERVER_TTS_LANGS)
    assert all(item["translate_supported"] for item in langs)
    assert all(item["stt_supported"] for item in langs)
