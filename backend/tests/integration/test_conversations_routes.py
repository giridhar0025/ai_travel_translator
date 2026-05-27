from collections.abc import Iterator

import pytest
from fastapi.testclient import TestClient


def _guest_headers(client: TestClient) -> dict[str, str]:
    body = client.post("/api/v1/auth/guest").json()
    return {"Authorization": f"Bearer {body['token']}"}


@pytest.fixture
def headers(client: TestClient) -> Iterator[dict[str, str]]:
    """Mint a guest user for the test and cascade-delete on teardown."""
    h = _guest_headers(client)
    try:
        yield h
    finally:
        client.delete("/api/v1/me", headers=h)


# ---------- create ----------


def test_create_conversation_returns_201_and_row(
    client: TestClient, headers: dict[str, str]
) -> None:
    r = client.post("/api/v1/conversations", headers=headers, json={"title": "Trip prep"})
    assert r.status_code == 201, r.text
    body = r.json()
    assert body["title"] == "Trip prep"
    assert body["message_count"] == 0
    assert "id" in body and "user_id" in body


def test_create_conversation_requires_auth(client: TestClient) -> None:
    r = client.post("/api/v1/conversations", json={"title": "x"})
    assert r.status_code == 401


# ---------- append + auto-title + counters ----------


def test_append_three_messages_bumps_counter_and_auto_titles(
    client: TestClient, headers: dict[str, str]
) -> None:
    # Untitled conversation — first message should become its title.
    convo = client.post("/api/v1/conversations", headers=headers, json={}).json()
    cid = convo["id"]
    assert convo["title"] is None

    texts = ["Where is the menu?", "How much is this?", "Thank you"]
    last_msg = None
    for t in texts:
        r = client.post(
            f"/api/v1/conversations/{cid}/messages",
            headers=headers,
            json={"role": "user", "source_text": t, "source_lang": "en"},
        )
        assert r.status_code == 201, r.text
        last_msg = r.json()

    detail = client.get(f"/api/v1/conversations/{cid}", headers=headers).json()
    assert detail["conversation"]["message_count"] == 3
    assert detail["conversation"]["title"] == "Where is the menu?"  # auto-title
    assert detail["conversation"]["last_message_at"] == last_msg["created_at"]
    assert [m["source_text"] for m in detail["messages"]] == texts  # oldest-first


def test_append_to_unknown_convo_404s(client: TestClient, headers: dict[str, str]) -> None:
    fake_id = "00000000-0000-0000-0000-000000000000"
    r = client.post(
        f"/api/v1/conversations/{fake_id}/messages",
        headers=headers,
        json={"role": "user", "source_text": "hi", "source_lang": "en"},
    )
    assert r.status_code == 404


def test_append_rejects_empty_source_text(client: TestClient, headers: dict[str, str]) -> None:
    cid = client.post("/api/v1/conversations", headers=headers, json={}).json()["id"]
    r = client.post(
        f"/api/v1/conversations/{cid}/messages",
        headers=headers,
        json={"role": "user", "source_text": "", "source_lang": "en"},
    )
    assert r.status_code == 422


# ---------- list + search ----------


def test_list_excludes_other_users(client: TestClient, headers: dict[str, str]) -> None:
    # Create a convo on a different user — current user must not see it.
    other = _guest_headers(client)
    try:
        other_convo = client.post(
            "/api/v1/conversations", headers=other, json={"title": "Other's"}
        ).json()
        client.post(
            f"/api/v1/conversations/{other_convo['id']}/messages",
            headers=other,
            json={"role": "user", "source_text": "hi", "source_lang": "en"},
        )

        mine = client.post("/api/v1/conversations", headers=headers, json={"title": "Mine"}).json()
        client.post(
            f"/api/v1/conversations/{mine['id']}/messages",
            headers=headers,
            json={"role": "user", "source_text": "hello", "source_lang": "en"},
        )

        listed = client.get("/api/v1/conversations", headers=headers).json()
        ids = [c["id"] for c in listed["items"]]
        assert mine["id"] in ids
        assert other_convo["id"] not in ids
    finally:
        client.delete("/api/v1/me", headers=other)


def test_search_q_matches_message_body(client: TestClient, headers: dict[str, str]) -> None:
    a = client.post("/api/v1/conversations", headers=headers, json={"title": "A"}).json()
    b = client.post("/api/v1/conversations", headers=headers, json={"title": "B"}).json()
    client.post(
        f"/api/v1/conversations/{a['id']}/messages",
        headers=headers,
        json={"role": "user", "source_text": "Where is the menu?", "source_lang": "en"},
    )
    client.post(
        f"/api/v1/conversations/{b['id']}/messages",
        headers=headers,
        json={"role": "user", "source_text": "What time is the train?", "source_lang": "en"},
    )

    hit = client.get("/api/v1/conversations", headers=headers, params={"q": "menu"}).json()
    hit_ids = {c["id"] for c in hit["items"]}
    assert a["id"] in hit_ids and b["id"] not in hit_ids

    miss = client.get("/api/v1/conversations", headers=headers, params={"q": "zzzz"}).json()
    assert miss["items"] == []


def test_search_matches_translated_text(client: TestClient, headers: dict[str, str]) -> None:
    cid = client.post("/api/v1/conversations", headers=headers, json={}).json()["id"]
    client.post(
        f"/api/v1/conversations/{cid}/messages",
        headers=headers,
        json={
            "role": "ai",
            "source_text": "Where is the bathroom?",
            "source_lang": "en",
            "translated_text": "बाथरूम कहाँ है?",
            "target_lang": "hi",
        },
    )
    r = client.get("/api/v1/conversations", headers=headers, params={"q": "बाथरूम"}).json()
    assert any(c["id"] == cid for c in r["items"])


def test_search_matches_title(client: TestClient, headers: dict[str, str]) -> None:
    cid = client.post(
        "/api/v1/conversations", headers=headers, json={"title": "Restaurant phrases"}
    ).json()["id"]
    r = client.get("/api/v1/conversations", headers=headers, params={"q": "restaurant"}).json()
    assert any(c["id"] == cid for c in r["items"])


# ---------- pagination ----------


def test_list_pagination_cursor(client: TestClient, headers: dict[str, str]) -> None:
    # Create 5 convos with at least one message each so last_message_at is set
    # deterministically. We can't sleep so they may share timestamps — pagination
    # still works because the cursor uses `< last_message_at` and we ask for 2.
    ids = []
    for i in range(5):
        c = client.post("/api/v1/conversations", headers=headers, json={"title": f"c{i}"}).json()
        client.post(
            f"/api/v1/conversations/{c['id']}/messages",
            headers=headers,
            json={"role": "user", "source_text": f"msg {i}", "source_lang": "en"},
        )
        ids.append(c["id"])

    page1 = client.get("/api/v1/conversations", headers=headers, params={"limit": 2}).json()
    assert len(page1["items"]) == 2
    assert page1["next_cursor"] is not None

    page2 = client.get(
        "/api/v1/conversations",
        headers=headers,
        params={"limit": 2, "cursor": page1["next_cursor"]},
    ).json()
    # No overlap between pages: the cursor uses strict `<` so consecutive pages
    # share no items.
    p1_ids = {c["id"] for c in page1["items"]}
    p2_ids = {c["id"] for c in page2["items"]}
    assert not (p1_ids & p2_ids)


# ---------- soft delete ----------


def test_soft_delete_excludes_from_list(client: TestClient, headers: dict[str, str]) -> None:
    cid = client.post("/api/v1/conversations", headers=headers, json={"title": "doomed"}).json()[
        "id"
    ]

    r = client.delete(f"/api/v1/conversations/{cid}", headers=headers)
    assert r.status_code == 204

    listed = client.get("/api/v1/conversations", headers=headers).json()
    assert all(c["id"] != cid for c in listed["items"])

    # Deleting again is a 404 (already soft-deleted, no row to update).
    again = client.delete(f"/api/v1/conversations/{cid}", headers=headers)
    assert again.status_code == 404


def test_delete_unknown_404(client: TestClient, headers: dict[str, str]) -> None:
    r = client.delete("/api/v1/conversations/00000000-0000-0000-0000-000000000000", headers=headers)
    assert r.status_code == 404


# ---------- detail ----------


def test_get_detail_for_unknown_404(client: TestClient, headers: dict[str, str]) -> None:
    r = client.get("/api/v1/conversations/00000000-0000-0000-0000-000000000000", headers=headers)
    assert r.status_code == 404


def test_get_detail_messages_paginated_with_before_id(
    client: TestClient, headers: dict[str, str]
) -> None:
    cid = client.post("/api/v1/conversations", headers=headers, json={}).json()["id"]
    msg_ids = []
    for i in range(4):
        m = client.post(
            f"/api/v1/conversations/{cid}/messages",
            headers=headers,
            json={"role": "user", "source_text": f"m{i}", "source_lang": "en"},
        ).json()
        msg_ids.append(m["id"])

    page = client.get(f"/api/v1/conversations/{cid}", headers=headers, params={"limit": 2}).json()
    assert len(page["messages"]) == 2
    assert page["next_before"] is not None

    # Use the third message as the anchor → expect only message 0 + 1 returned
    # (strictly older than message 2).
    anchor = msg_ids[2]
    older = client.get(
        f"/api/v1/conversations/{cid}",
        headers=headers,
        params={"limit": 10, "before_id": anchor},
    ).json()
    assert [m["id"] for m in older["messages"]] == [msg_ids[0], msg_ids[1]]


# ---------- date grouping ----------


def test_date_groups_present(client: TestClient, headers: dict[str, str]) -> None:
    # All convos created right now → all in Today bucket. Real older-date
    # coverage is in the unit test (uses an injectable `now`).
    for i in range(3):
        c = client.post("/api/v1/conversations", headers=headers, json={"title": f"t{i}"}).json()
        client.post(
            f"/api/v1/conversations/{c['id']}/messages",
            headers=headers,
            json={"role": "user", "source_text": "hi", "source_lang": "en"},
        )

    listed = client.get("/api/v1/conversations", headers=headers).json()
    assert listed["groups"], "expected at least one group"
    assert listed["groups"][0]["label"] == "Today"
    assert listed["groups"][0]["count"] == 3
