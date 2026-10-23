"""Groq chat-completions client — the credit-free translation backend.

Mirrors the HF client tests: happy path, retry on the free-tier rate limit,
fail-fast on 4xx, and SSE streaming.
"""

import pytest
import respx
from httpx import Response

from app.services.groq_client import GroqClient, GroqInferenceError

BASE_URL = "https://api.groq.com/openai/v1"


def _completion(text: str) -> dict:
    return {"choices": [{"message": {"role": "assistant", "content": text}}]}


@pytest.mark.asyncio
async def test_chat_completion_returns_message_content() -> None:
    client = GroqClient(api_key="k", base_url=BASE_URL, timeout=5.0)
    try:
        with respx.mock(base_url=BASE_URL) as mock:
            mock.post("/chat/completions").mock(
                return_value=Response(200, json=_completion("नमस्ते"))
            )
            result = await client.chat_completion(
                model="llama-3.3-70b-versatile",
                messages=[{"role": "user", "content": "hi"}],
            )
        assert result == "नमस्ते"
    finally:
        await client.aclose()


@pytest.mark.asyncio
async def test_chat_completion_retries_on_429(monkeypatch) -> None:
    import asyncio as _asyncio

    async def _no_sleep(_):
        return None

    monkeypatch.setattr(_asyncio, "sleep", _no_sleep)

    client = GroqClient(api_key="k", base_url=BASE_URL, timeout=5.0)
    try:
        with respx.mock(base_url=BASE_URL) as mock:
            route = mock.post("/chat/completions").mock(
                side_effect=[
                    Response(429, text="rate limited", headers={"retry-after": "0"}),
                    Response(200, json=_completion("ok")),
                ]
            )
            result = await client.chat_completion(
                model="llama-3.3-70b-versatile",
                messages=[{"role": "user", "content": "hi"}],
            )
        assert result == "ok"
        assert route.call_count == 2
    finally:
        await client.aclose()


@pytest.mark.asyncio
async def test_chat_completion_raises_on_401() -> None:
    client = GroqClient(api_key="bad", base_url=BASE_URL, timeout=5.0)
    try:
        with respx.mock(base_url=BASE_URL) as mock:
            mock.post("/chat/completions").mock(return_value=Response(401, text="invalid api key"))
            with pytest.raises(GroqInferenceError, match="401"):
                await client.chat_completion(
                    model="llama-3.3-70b-versatile",
                    messages=[{"role": "user", "content": "hi"}],
                )
    finally:
        await client.aclose()


@pytest.mark.asyncio
async def test_chat_completion_stream_yields_deltas_until_done() -> None:
    body = (
        'data: {"choices":[{"delta":{"content":"नम"}}]}\n\n'
        'data: {"choices":[{"delta":{"content":"स्ते"}}]}\n\n'
        "data: [DONE]\n\n"
    )
    client = GroqClient(api_key="k", base_url=BASE_URL, timeout=5.0)
    try:
        with respx.mock(base_url=BASE_URL) as mock:
            mock.post("/chat/completions").mock(
                return_value=Response(200, text=body, headers={"content-type": "text/event-stream"})
            )
            deltas = [
                d
                async for d in client.chat_completion_stream(
                    model="llama-3.3-70b-versatile",
                    messages=[{"role": "user", "content": "hi"}],
                )
            ]
        assert deltas == ["नम", "स्ते"]
    finally:
        await client.aclose()


@pytest.mark.asyncio
async def test_chat_completion_stream_raises_on_error_status() -> None:
    client = GroqClient(api_key="k", base_url=BASE_URL, timeout=5.0)
    try:
        with respx.mock(base_url=BASE_URL) as mock:
            mock.post("/chat/completions").mock(return_value=Response(402, text="no credits"))
            with pytest.raises(GroqInferenceError, match="402"):
                async for _ in client.chat_completion_stream(
                    model="llama-3.3-70b-versatile",
                    messages=[{"role": "user", "content": "hi"}],
                ):
                    pass
    finally:
        await client.aclose()
