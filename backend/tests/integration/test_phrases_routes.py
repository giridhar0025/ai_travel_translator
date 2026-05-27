"""End-to-end /phrases tests.

The phrases route caches translations in `phrase_translations`. Each test
owns its target-language cache state via the `clean_translations` fixture so
the "first call hits HF, second doesn't" assertion is deterministic.
"""

import asyncio
from collections.abc import Iterator

import pytest
import respx
from fastapi.testclient import TestClient
from httpx import Response
from sqlalchemy import delete

from app.db.models.phrase import PhraseTranslation
from app.db.session import AsyncSessionLocal
from app.services.hf_client import reset_hf_client_for_tests
from app.services.translation import reset_translation_service_for_tests

HF_BASE = "https://router.huggingface.co/hf-inference"
EN_TO_MUL = "/models/Helsinki-NLP/opus-mt-en-mul"


async def _delete_translations(target_lang: str) -> None:
    async with AsyncSessionLocal() as s:
        await s.execute(
            delete(PhraseTranslation).where(PhraseTranslation.target_lang == target_lang)
        )
        await s.commit()


@pytest.fixture(autouse=True)
def _reset_singletons() -> Iterator[None]:
    reset_translation_service_for_tests()
    reset_hf_client_for_tests()
    yield
    reset_translation_service_for_tests()
    reset_hf_client_for_tests()


@pytest.fixture
def clean_hi() -> Iterator[None]:
    asyncio.run(_delete_translations("hi"))
    yield
    asyncio.run(_delete_translations("hi"))


@pytest.fixture
def clean_te() -> Iterator[None]:
    asyncio.run(_delete_translations("te"))
    yield
    asyncio.run(_delete_translations("te"))


# ---------- /phrases/categories ----------


def test_list_categories_returns_seeded_five(client: TestClient) -> None:
    r = client.get("/api/v1/phrases/categories")
    assert r.status_code == 200
    body = r.json()
    slugs = [c["slug"] for c in body]
    assert slugs == ["restaurants", "directions", "transport", "shopping", "emergencies"]
    assert all("icon" in c and "color" in c for c in body)


# ---------- /phrases ----------


def test_phrases_first_call_triggers_one_batch_then_caches(
    client: TestClient, clean_hi: None
) -> None:
    # Six Hindi translations, one per phrase in the restaurants category. The
    # batch fans out via asyncio.gather so respx sees 6 individual calls — the
    # assertion is on the total call count.
    translations = [
        "दो लोगों के लिए मेज़, कृपया",
        "क्या मैं मेन्यू देख सकता हूँ?",
        "बिल, कृपया",
        "मैं शाकाहारी हूँ",
        "आप क्या सुझाते हैं?",
        "क्या मुझे थोड़ा पानी मिल सकता है?",
    ]
    with respx.mock(base_url=HF_BASE) as mock:
        route = mock.post(EN_TO_MUL).mock(
            side_effect=[Response(200, json=[{"translation_text": t}]) for t in translations]
        )

        r1 = client.get("/api/v1/phrases", params={"category": "restaurants", "target": "hi"})

        assert r1.status_code == 200, r1.text
        body = r1.json()
        assert len(body) == 6
        assert {p["target_lang"] for p in body} == {"hi"}
        assert {p["source_lang"] for p in body} == {"en"}
        # Each phrase round-trips through HF on first call.
        assert route.call_count == 6
        assert sorted([p["sort_order"] for p in body]) == [1, 2, 3, 4, 5, 6]

    # Second call (no HF mock active) — should be served entirely from cache.
    r2 = client.get("/api/v1/phrases", params={"category": "restaurants", "target": "hi"})
    assert r2.status_code == 200, r2.text
    body2 = r2.json()
    assert [p["translation"] for p in body2] == [p["translation"] for p in r1.json()]


def test_phrases_target_en_short_circuits_no_hf(
    client: TestClient,
) -> None:
    # All seeded phrases are source_lang=en; target=en must not call HF and must
    # not write any cache rows.
    with respx.mock(base_url=HF_BASE, assert_all_called=False) as mock:
        route = mock.post(EN_TO_MUL).mock(return_value=Response(200, json=[]))
        r = client.get("/api/v1/phrases", params={"category": "restaurants", "target": "en"})

        assert r.status_code == 200
        body = r.json()
        assert len(body) == 6
        assert [p["translation"] == p["source_text"] for p in body] == [True] * 6
        assert route.call_count == 0


def test_phrases_switching_target_triggers_new_batch(
    client: TestClient, clean_hi: None, clean_te: None
) -> None:
    hi_translations = ["नमस्ते"] * 6
    te_translations = ["నమస్కారం"] * 6

    with respx.mock(base_url=HF_BASE) as mock:
        route = mock.post(EN_TO_MUL).mock(
            side_effect=[Response(200, json=[{"translation_text": t}]) for t in hi_translations]
        )
        r_hi = client.get("/api/v1/phrases", params={"category": "restaurants", "target": "hi"})
        assert r_hi.status_code == 200
        assert route.call_count == 6

    # Switching target → fresh batch (te has its own row per phrase).
    with respx.mock(base_url=HF_BASE) as mock:
        route = mock.post(EN_TO_MUL).mock(
            side_effect=[Response(200, json=[{"translation_text": t}]) for t in te_translations]
        )
        r_te = client.get("/api/v1/phrases", params={"category": "restaurants", "target": "te"})
        assert r_te.status_code == 200
        assert route.call_count == 6

    # hi cache still warm — no HF calls for hi.
    with respx.mock(base_url=HF_BASE, assert_all_called=False) as mock:
        route = mock.post(EN_TO_MUL).mock(return_value=Response(200, json=[]))
        r_hi2 = client.get("/api/v1/phrases", params={"category": "restaurants", "target": "hi"})
        assert r_hi2.status_code == 200
        assert route.call_count == 0


def test_phrases_unknown_category_returns_404(client: TestClient) -> None:
    r = client.get("/api/v1/phrases", params={"category": "ghost", "target": "hi"})
    assert r.status_code == 404


def test_phrases_unsupported_target_returns_400(client: TestClient) -> None:
    r = client.get("/api/v1/phrases", params={"category": "restaurants", "target": "fr"})
    assert r.status_code == 400


def test_phrases_missing_required_params_returns_422(client: TestClient) -> None:
    assert client.get("/api/v1/phrases", params={"target": "hi"}).status_code == 422
    assert client.get("/api/v1/phrases", params={"category": "restaurants"}).status_code == 422
