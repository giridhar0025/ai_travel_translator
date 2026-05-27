from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient

from app.auth.firebase import FirebaseUser


def test_guest_login_creates_user_and_returns_token(client: TestClient) -> None:
    r = client.post("/api/v1/auth/guest")
    assert r.status_code == 200
    body = r.json()
    assert "user" in body
    assert "settings" in body
    assert "token" in body
    assert body["user"]["is_guest"] is True
    assert body["user"]["firebase_uid"] is None
    assert body["settings"]["default_language"] == "en"
    assert body["settings"]["playback_speed"] == "normal"

    # Use the token to call /me + then DELETE /me to clean up
    headers = {"Authorization": f"Bearer {body['token']}"}
    me = client.get("/api/v1/me", headers=headers)
    assert me.status_code == 200
    assert me.json()["user"]["id"] == body["user"]["id"]

    deleted = client.delete("/api/v1/me", headers=headers)
    assert deleted.status_code == 204

    # Token still verifies but user no longer exists → 401
    after = client.get("/api/v1/me", headers=headers)
    assert after.status_code == 401


def test_me_requires_bearer_token(client: TestClient) -> None:
    assert client.get("/api/v1/me").status_code == 401
    assert client.get("/api/v1/me", headers={"Authorization": "Basic xyz"}).status_code == 401
    assert client.get("/api/v1/me", headers={"Authorization": "Bearer garbage"}).status_code == 401


def test_verify_creates_user_for_new_firebase_uid(client: TestClient) -> None:
    fake_fb = FirebaseUser(
        uid="firebase-uid-test-1",
        email="newuser@example.com",
        name="New User",
        picture=None,
        provider="google.com",
    )
    with patch("app.routers.auth.verify_id_token", new=AsyncMock(return_value=fake_fb)):
        r = client.post("/api/v1/auth/verify", json={"id_token": "fake-firebase-token"})

    assert r.status_code == 200
    body = r.json()
    assert body["user"]["is_guest"] is False
    assert body["user"]["firebase_uid"] == "firebase-uid-test-1"
    assert body["user"]["email"] == "newuser@example.com"
    assert body["user"]["display_name"] == "New User"

    # Cleanup
    headers = {"Authorization": f"Bearer {body['token']}"}
    assert client.delete("/api/v1/me", headers=headers).status_code == 204


def test_verify_returns_existing_user_for_known_uid(client: TestClient) -> None:
    fake_fb = FirebaseUser(
        uid="firebase-uid-test-2",
        email="first@example.com",
        name="First Name",
        picture=None,
        provider="google.com",
    )
    with patch("app.routers.auth.verify_id_token", new=AsyncMock(return_value=fake_fb)):
        first = client.post("/api/v1/auth/verify", json={"id_token": "fake-firebase-token-1"})
        assert first.status_code == 200
        first_user_id = first.json()["user"]["id"]

        # Second call with the same uid but updated profile → same user, updated fields
        updated_fb = FirebaseUser(
            uid="firebase-uid-test-2",
            email="renamed@example.com",
            name="Second Name",
            picture="https://example.com/p.png",
            provider="google.com",
        )
        with patch("app.routers.auth.verify_id_token", new=AsyncMock(return_value=updated_fb)):
            second = client.post("/api/v1/auth/verify", json={"id_token": "fake-firebase-token-2"})

    assert second.status_code == 200
    body = second.json()
    assert body["user"]["id"] == first_user_id
    assert body["user"]["email"] == "renamed@example.com"
    assert body["user"]["display_name"] == "Second Name"
    assert body["user"]["photo_url"] == "https://example.com/p.png"

    # Cleanup
    headers = {"Authorization": f"Bearer {body['token']}"}
    assert client.delete("/api/v1/me", headers=headers).status_code == 204


def test_verify_returns_401_for_invalid_firebase_token(client: TestClient) -> None:
    from app.auth.errors import FirebaseVerificationError

    with patch(
        "app.routers.auth.verify_id_token",
        new=AsyncMock(side_effect=FirebaseVerificationError("bad token")),
    ):
        r = client.post("/api/v1/auth/verify", json={"id_token": "garbage"})

    assert r.status_code == 401


def test_delete_me_cascades_user_settings(client: TestClient) -> None:
    r = client.post("/api/v1/auth/guest")
    body = r.json()
    headers = {"Authorization": f"Bearer {body['token']}"}

    # Settings exists pre-delete
    assert client.get("/api/v1/me", headers=headers).json()["settings"] is not None

    deleted = client.delete("/api/v1/me", headers=headers)
    assert deleted.status_code == 204

    # /me with same token now 401
    assert client.get("/api/v1/me", headers=headers).status_code == 401
