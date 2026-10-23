"""Groq chat-completions client — the credit-free translation backend.

Why this exists: the Hugging Face Inference router bills per-account credits
and starts returning `402 Payment Required` once they run out, which takes
translation down even though the code is fine. Groq serves the same Llama
family on a free tier through an OpenAI-compatible API, so pointing
translation at Groq removes that single point of failure.

`TranslationService` does not care which backend it holds — this class
deliberately mirrors `HuggingFaceClient.chat_completion` /
`chat_completion_stream` (including the unused `provider` keyword) so the two
are drop-in interchangeable.
"""

import asyncio
import json
from collections.abc import AsyncIterator
from typing import Any

import httpx

from app.config import get_settings
from app.services.hf_client import _extract_delta_content, _extract_message_content
from app.services.llm_errors import LLMInferenceError

# Statuses worth retrying: 429 is Groq's free-tier rate limit, 503 is a
# transient upstream blip. Everything else fails fast.
_RETRYABLE_STATUSES = frozenset({429, 503})


class GroqInferenceError(LLMInferenceError):
    """Groq returned a non-retryable error or could not be reached."""


class GroqClient:
    def __init__(self, api_key: str, base_url: str, timeout: float = 60.0) -> None:
        self._client = httpx.AsyncClient(
            base_url=base_url.rstrip("/"),
            headers={"Authorization": f"Bearer {api_key}"},
            timeout=timeout,
        )

    async def aclose(self) -> None:
        await self._client.aclose()

    async def chat_completion(
        self,
        *,
        provider: str = "",  # accepted for interface parity with the HF client
        model: str,
        messages: list[dict[str, str]],
        temperature: float = 0.3,
        max_tokens: int = 1024,
        max_retries: int = 2,
    ) -> str:
        """Return the assistant message content for a chat completion."""
        payload: dict[str, Any] = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        backoff = 1.0

        for attempt in range(max_retries + 1):
            try:
                response = await self._client.post("/chat/completions", json=payload)
            except httpx.HTTPError as exc:
                raise GroqInferenceError(f"Groq request failed: {exc}") from exc

            if response.status_code in _RETRYABLE_STATUSES and attempt < max_retries:
                await asyncio.sleep(_retry_after_seconds(response, backoff))
                backoff *= 2
                continue

            if response.status_code >= 400:
                raise GroqInferenceError(f"Groq chat {response.status_code}: {response.text[:200]}")

            return _extract_message_content(response.json())

        raise GroqInferenceError("Groq chat retries exhausted")

    async def chat_completion_stream(
        self,
        *,
        provider: str = "",  # accepted for interface parity with the HF client
        model: str,
        messages: list[dict[str, str]],
        temperature: float = 0.3,
        max_tokens: int = 1024,
    ) -> AsyncIterator[str]:
        """Stream content deltas from Groq's SSE endpoint."""
        payload: dict[str, Any] = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
            "stream": True,
        }

        try:
            async with self._client.stream("POST", "/chat/completions", json=payload) as response:
                if response.status_code >= 400:
                    body = await response.aread()
                    raise GroqInferenceError(
                        f"Groq chat stream {response.status_code}: {body[:200]!r}"
                    )
                async for line in response.aiter_lines():
                    if not line.startswith("data:"):
                        continue
                    data = line[len("data:") :].strip()
                    if data == "[DONE]":
                        break
                    try:
                        chunk = json.loads(data)
                    except json.JSONDecodeError:
                        continue
                    delta = _extract_delta_content(chunk)
                    if delta:
                        yield delta
        except httpx.HTTPError as exc:
            raise GroqInferenceError(f"Groq stream failed: {exc}") from exc


def _retry_after_seconds(response: httpx.Response, fallback: float) -> float:
    """Honour `Retry-After` when Groq sends one, else use our backoff."""
    header = response.headers.get("retry-after")
    if not header:
        return fallback
    try:
        return min(float(header), 10.0)
    except ValueError:
        return fallback


_client: GroqClient | None = None


def get_groq_client() -> GroqClient:
    global _client
    if _client is None:
        settings = get_settings()
        _client = GroqClient(
            api_key=settings.groq_api_key,
            base_url=settings.groq_base_url,
            timeout=settings.groq_timeout_seconds,
        )
    return _client


def reset_groq_client_for_tests() -> None:
    global _client
    _client = None
