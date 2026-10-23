"""Thin async client for the Hugging Face Inference API.

Single shared instance per process. Retries on 503 (cold model start) with
exponential backoff. All translation / STT / TTS services route through this.

Two flows are supported:
- `infer(model, ...)` — synchronous JSON/binary in, JSON/binary out, used by
  `hf-inference` provider (translate, STT).
- `predict_replicate(version, ...)` — async prediction flow used by Replicate-
  hosted models (TTS). Creates a prediction, polls until terminal, downloads
  the output URL and returns the bytes.
"""

import asyncio
import json
from collections.abc import AsyncIterator
from typing import Any

import httpx

from app.config import get_settings
from app.services.llm_errors import LLMInferenceError

HF_BASE_URL = "https://router.huggingface.co/hf-inference"
HF_ROUTER_ROOT = "https://router.huggingface.co"


class HFInferenceError(LLMInferenceError):
    """HF returned a non-retryable error response or the request failed terminally."""


def _chat_completions_url(provider: str) -> str:
    """Resolve the chat-completions URL for a provider.

    `"auto"`/empty routes to the router's unified endpoint, which auto-selects
    an available (and typically fast) provider — the model id is the plain HF
    repo id. A concrete provider (e.g. `groq`) pins the provider path, but then
    the model id must be that provider's own naming."""
    if provider in ("", "auto"):
        return f"{HF_ROUTER_ROOT}/v1/chat/completions"
    return f"{HF_ROUTER_ROOT}/{provider}/v1/chat/completions"


def _extract_message_content(data: dict[str, Any]) -> str:
    """Pull the assistant message text out of an OpenAI-style chat response."""
    choices = data.get("choices")
    if isinstance(choices, list) and choices and isinstance(choices[0], dict):
        message = choices[0].get("message")
        if isinstance(message, dict):
            content = message.get("content")
            if isinstance(content, str):
                return content
    raise HFInferenceError(f"chat completion missing content: {str(data)[:200]}")


def _extract_delta_content(chunk: dict[str, Any]) -> str | None:
    """Pull the incremental token text out of an OpenAI-style stream chunk."""
    choices = chunk.get("choices")
    if isinstance(choices, list) and choices and isinstance(choices[0], dict):
        delta = choices[0].get("delta")
        if isinstance(delta, dict):
            content = delta.get("content")
            if isinstance(content, str):
                return content
    return None


class HuggingFaceClient:
    def __init__(self, *, token: str, timeout: float) -> None:
        self._token = token
        self._timeout = timeout
        headers = {"Authorization": f"Bearer {token}"} if token else {}
        self._client = httpx.AsyncClient(
            base_url=HF_BASE_URL,
            headers=headers,
            timeout=timeout,
        )

    async def aclose(self) -> None:
        await self._client.aclose()

    async def predict_replicate(
        self,
        *,
        version: str,
        input_payload: dict[str, Any],
        poll_interval: float = 0.5,
        max_polls: int = 60,
    ) -> bytes:
        """Run a Replicate-hosted model via the HF router and return the
        rendered output bytes.

        Flow: create prediction → poll predictions/{id} until status is
        terminal → download `output` URL (replicate.delivery CDN, public).
        Raises HFInferenceError on any non-2xx from the router or terminal
        status of `failed`/`canceled`/timeout.
        """
        auth = {"Authorization": f"Bearer {self._token}"} if self._token else {}

        create = await self._client.post(
            f"{HF_ROUTER_ROOT}/replicate/v1/predictions",
            json={"input": input_payload, "version": version},
        )
        if create.status_code >= 400:
            raise HFInferenceError(f"replicate create {create.status_code}: {create.text[:200]}")
        pred = create.json()
        pred_id = pred.get("id")
        if not pred_id:
            raise HFInferenceError(f"replicate create returned no id: {pred}")

        # Some predictions complete on the create call already; others come back
        # `starting` or `processing` and we need to poll.
        status = pred.get("status")
        output = pred.get("output")
        error_msg = pred.get("error")

        polls_used = 0
        while status not in {"succeeded", "failed", "canceled"} and polls_used < max_polls:
            await asyncio.sleep(poll_interval)
            polls_used += 1
            poll = await self._client.get(
                f"{HF_ROUTER_ROOT}/replicate/v1/predictions/{pred_id}",
            )
            if poll.status_code >= 400:
                raise HFInferenceError(f"replicate poll {poll.status_code}: {poll.text[:200]}")
            body = poll.json()
            status = body.get("status")
            output = body.get("output")
            error_msg = body.get("error")

        if status in {"failed", "canceled"}:
            raise HFInferenceError(f"replicate prediction {status}: {error_msg}")
        if status != "succeeded":
            raise HFInferenceError(f"replicate prediction timed out (last status={status})")
        if not output or not isinstance(output, str):
            raise HFInferenceError(f"replicate output missing/invalid: {output!r}")

        # Download from the public CDN URL. No HF auth — replicate.delivery is
        # public; passing the bearer here would just be ignored.
        async with httpx.AsyncClient(timeout=self._timeout, headers=auth) as dl:
            audio_resp = await dl.get(output)
        if audio_resp.status_code >= 400:
            raise HFInferenceError(
                f"replicate output fetch {audio_resp.status_code}: {audio_resp.text[:200]}"
            )
        return audio_resp.content

    async def transcribe_openai(
        self,
        *,
        provider: str,
        model: str,
        audio_bytes: bytes,
        content_type: str = "audio/wav",
        filename: str = "audio",
        max_retries: int = 2,
    ) -> dict[str, Any]:
        """Transcribe audio via a provider's OpenAI-compatible
        `/v1/audio/transcriptions` endpoint on the HF router.

        Used instead of the raw-bytes `hf-inference` path because that provider's
        Whisper endpoint hangs and 500s; `together` serves whisper-large-v3
        reliably. Sends multipart (`file` + `model`) and returns the parsed JSON
        (`{"text": ..., "language": ...}`). Retries on 503 (cold start)."""
        url = f"{HF_ROUTER_ROOT}/{provider}/v1/audio/transcriptions"
        backoff = 1.0
        last_text = ""

        for attempt in range(max_retries + 1):
            response = await self._client.post(
                url,
                files={"file": (filename, audio_bytes, content_type)},
                data={"model": model},
            )

            if response.status_code == 503:
                last_text = response.text[:200]
                if attempt < max_retries:
                    await asyncio.sleep(backoff)
                    backoff *= 2
                    continue
                break

            if response.status_code >= 400:
                raise HFInferenceError(f"HF ASR {response.status_code}: {response.text[:200]}")

            return response.json()

        raise HFInferenceError(f"max retries exceeded — last status 503: {last_text}")

    async def chat_completion(
        self,
        *,
        provider: str,
        model: str,
        messages: list[dict[str, str]],
        temperature: float = 0.3,
        max_tokens: int = 1024,
        max_retries: int = 2,
    ) -> str:
        """Call a provider's OpenAI-compatible `/v1/chat/completions` endpoint
        on the HF router and return the assistant message content.

        Used for LLM-backed translation. Retries on 503 (cold start) like the
        other HF paths."""
        url = _chat_completions_url(provider)
        payload: dict[str, Any] = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        backoff = 1.0
        last_text = ""

        for attempt in range(max_retries + 1):
            response = await self._client.post(url, json=payload)

            if response.status_code == 503:
                last_text = response.text[:200]
                if attempt < max_retries:
                    await asyncio.sleep(backoff)
                    backoff *= 2
                    continue
                break

            if response.status_code >= 400:
                raise HFInferenceError(f"HF chat {response.status_code}: {response.text[:200]}")

            return _extract_message_content(response.json())

        raise HFInferenceError(f"max retries exceeded — last status 503: {last_text}")

    async def chat_completion_stream(
        self,
        *,
        provider: str,
        model: str,
        messages: list[dict[str, str]],
        temperature: float = 0.3,
        max_tokens: int = 1024,
    ) -> AsyncIterator[str]:
        """Stream an OpenAI-compatible chat completion from the HF router,
        yielding content deltas as they arrive.

        Parses SSE `data:` lines and stops on `[DONE]`. No 503 retry here —
        by the time bytes stream the upstream has already committed to a
        response; the caller handles connection-level failures."""
        url = _chat_completions_url(provider)
        payload: dict[str, Any] = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
            "stream": True,
        }

        async with self._client.stream("POST", url, json=payload) as response:
            if response.status_code >= 400:
                body = await response.aread()
                raise HFInferenceError(f"HF chat stream {response.status_code}: {body[:200]!r}")
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

    async def infer(
        self,
        model: str,
        *,
        json_payload: dict[str, Any] | None = None,
        content: bytes | None = None,
        content_type: str = "audio/wav",
        raw: bool = False,
        max_retries: int = 3,
    ) -> Any:
        """Call HF Inference for `model`. Either pass `json_payload` for JSON
        models, or `content` (+optional `content_type`) for binary models like
        Whisper / TTS. The HF router rejects `application/octet-stream` so the
        caller must hand us the real audio MIME type."""
        url = f"/models/{model}"
        backoff = 1.0
        last_text: str = ""

        for attempt in range(max_retries + 1):
            if content is not None:
                response = await self._client.post(
                    url,
                    content=content,
                    headers={"Content-Type": content_type},
                )
            else:
                response = await self._client.post(url, json=json_payload)

            # 503 = model is loading. Retry after backoff while we still have attempts.
            if response.status_code == 503:
                last_text = response.text[:200]
                if attempt < max_retries:
                    await asyncio.sleep(backoff)
                    backoff *= 2
                    continue
                break  # exhausted retries

            if response.status_code >= 400:
                raise HFInferenceError(f"HF {response.status_code}: {response.text[:200]}")

            return response.content if raw else response.json()

        raise HFInferenceError(f"max retries exceeded — last status 503: {last_text}")


_client: HuggingFaceClient | None = None


def get_hf_client() -> HuggingFaceClient:
    global _client
    if _client is None:
        settings = get_settings()
        _client = HuggingFaceClient(
            token=settings.hf_api_token,
            timeout=settings.hf_timeout_seconds,
        )
    return _client


def reset_hf_client_for_tests() -> None:
    """Clear the singleton so tests can inject a fake client via monkeypatch."""
    global _client
    _client = None
