from collections.abc import Iterator
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient


@pytest.fixture
def headers(client: TestClient) -> Iterator[dict[str, str]]:
    body = client.post("/api/v1/auth/guest").json()
    h = {"Authorization": f"Bearer {body['token']}"}
    try:
        yield h
    finally:
        client.delete("/api/v1/me", headers=h)


@pytest.fixture
def other_headers(client: TestClient) -> Iterator[dict[str, str]]:
    body = client.post("/api/v1/auth/guest").json()
    h = {"Authorization": f"Bearer {body['token']}"}
    try:
        yield h
    finally:
        client.delete("/api/v1/me", headers=h)


def _payload(kind: str = "phrase", **overrides: object) -> dict[str, object]:
    base = {
        "kind": kind,
        "ref_id": str(uuid4()),
        "source_lang": "en",
        "target_lang": "hi",
    }
    base.update(overrides)
    return base


def test_favorites_require_auth(client: TestClient) -> None:
    assert client.get("/api/v1/favorites").status_code == 401
    assert client.post("/api/v1/favorites", json=_payload()).status_code == 401


def test_create_favorite_returns_201_and_persists(
    client: TestClient, headers: dict[str, str]
) -> None:
    payload = _payload()
    r = client.post("/api/v1/favorites", headers=headers, json=payload)
    assert r.status_code == 201, r.text
    body = r.json()
    assert body["kind"] == "phrase"
    assert body["ref_id"] == payload["ref_id"]
    assert body["source_lang"] == "en"
    assert body["target_lang"] == "hi"

    listing = client.get("/api/v1/favorites", headers=headers).json()
    assert len(listing) == 1
    assert listing[0]["id"] == body["id"]


def test_create_duplicate_favorite_returns_409(client: TestClient, headers: dict[str, str]) -> None:
    payload = _payload()
    first = client.post("/api/v1/favorites", headers=headers, json=payload)
    assert first.status_code == 201
    dup = client.post("/api/v1/favorites", headers=headers, json=payload)
    assert dup.status_code == 409
    assert "already favorited" in dup.json()["detail"]


def test_create_favorite_rejects_unknown_lang(client: TestClient, headers: dict[str, str]) -> None:
    r = client.post("/api/v1/favorites", headers=headers, json=_payload(target_lang="fr"))
    assert r.status_code == 400
    assert "fr" in r.json()["detail"]


def test_create_favorite_rejects_unknown_kind(client: TestClient, headers: dict[str, str]) -> None:
    r = client.post("/api/v1/favorites", headers=headers, json=_payload(kind="bookmark"))
    assert r.status_code == 422


def test_list_favorites_filters_by_kind(client: TestClient, headers: dict[str, str]) -> None:
    p = client.post("/api/v1/favorites", headers=headers, json=_payload("phrase"))
    t = client.post("/api/v1/favorites", headers=headers, json=_payload("translation"))
    assert p.status_code == t.status_code == 201

    phrases = client.get("/api/v1/favorites?kind=phrase", headers=headers).json()
    assert {row["id"] for row in phrases} == {p.json()["id"]}

    translations = client.get("/api/v1/favorites?kind=translation", headers=headers).json()
    assert {row["id"] for row in translations} == {t.json()["id"]}

    all_rows = client.get("/api/v1/favorites", headers=headers).json()
    assert len(all_rows) == 2


def test_delete_favorite_204_then_404(client: TestClient, headers: dict[str, str]) -> None:
    created = client.post("/api/v1/favorites", headers=headers, json=_payload()).json()
    fav_id = created["id"]

    r = client.delete(f"/api/v1/favorites/{fav_id}", headers=headers)
    assert r.status_code == 204

    r2 = client.delete(f"/api/v1/favorites/{fav_id}", headers=headers)
    assert r2.status_code == 404


def test_delete_other_users_favorite_returns_404(
    client: TestClient,
    headers: dict[str, str],
    other_headers: dict[str, str],
) -> None:
    created = client.post("/api/v1/favorites", headers=headers, json=_payload()).json()
    fav_id = created["id"]

    # second user can't see or delete it
    r = client.delete(f"/api/v1/favorites/{fav_id}", headers=other_headers)
    assert r.status_code == 404

    # original owner still can
    r = client.delete(f"/api/v1/favorites/{fav_id}", headers=headers)
    assert r.status_code == 204
