from fastapi.testclient import TestClient


def test_list_languages_returns_seeded_rows(client: TestClient) -> None:
    r = client.get("/api/v1/languages")
    assert r.status_code == 200
    langs = r.json()
    codes = sorted(item["code"] for item in langs)
    assert codes == ["en", "hi", "te", "th"]

    by_code = {item["code"]: item for item in langs}
    assert by_code["th"]["tts_supported"] is False  # Thai TTS is client-side per plan §5.1
    assert by_code["te"]["tts_supported"] is True  # Telugu TTS via Indic-Parler
    assert all(item["translate_supported"] for item in langs)
    assert all(item["stt_supported"] for item in langs)
