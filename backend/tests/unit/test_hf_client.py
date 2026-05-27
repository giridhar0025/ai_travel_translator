import pytest
import respx
from httpx import Response

from app.services.hf_client import HFInferenceError, HuggingFaceClient


@pytest.mark.asyncio
async def test_infer_returns_json_on_200() -> None:
    client = HuggingFaceClient(token="t", timeout=5.0)
    try:
        with respx.mock(base_url="https://router.huggingface.co/hf-inference") as mock:
            mock.post("/models/m").mock(
                return_value=Response(200, json=[{"translation_text": "ok"}])
            )
            result = await client.infer("m", json_payload={"inputs": "x"})
        assert result == [{"translation_text": "ok"}]
    finally:
        await client.aclose()


@pytest.mark.asyncio
async def test_infer_returns_bytes_when_raw() -> None:
    client = HuggingFaceClient(token="t", timeout=5.0)
    try:
        with respx.mock(base_url="https://router.huggingface.co/hf-inference") as mock:
            mock.post("/models/m").mock(return_value=Response(200, content=b"\x01\x02"))
            result = await client.infer("m", json_payload={"inputs": "x"}, raw=True)
        assert result == b"\x01\x02"
    finally:
        await client.aclose()


@pytest.mark.asyncio
async def test_infer_retries_on_503(monkeypatch) -> None:
    # Patch asyncio.sleep so the test doesn't actually wait
    import asyncio as _asyncio

    async def _no_sleep(_):
        return None

    monkeypatch.setattr(_asyncio, "sleep", _no_sleep)

    client = HuggingFaceClient(token="t", timeout=5.0)
    try:
        with respx.mock(base_url="https://router.huggingface.co/hf-inference") as mock:
            route = mock.post("/models/m").mock(
                side_effect=[
                    Response(503, text="loading"),
                    Response(503, text="loading"),
                    Response(200, json={"ok": True}),
                ]
            )
            result = await client.infer("m", json_payload={"inputs": "x"})
        assert result == {"ok": True}
        assert route.call_count == 3
    finally:
        await client.aclose()


@pytest.mark.asyncio
async def test_infer_gives_up_after_max_retries(monkeypatch) -> None:
    import asyncio as _asyncio

    async def _no_sleep(_):
        return None

    monkeypatch.setattr(_asyncio, "sleep", _no_sleep)

    client = HuggingFaceClient(token="t", timeout=5.0)
    try:
        with respx.mock(base_url="https://router.huggingface.co/hf-inference") as mock:
            mock.post("/models/m").mock(return_value=Response(503, text="still loading"))
            with pytest.raises(HFInferenceError, match="max retries exceeded"):
                await client.infer("m", json_payload={"inputs": "x"})
    finally:
        await client.aclose()


@pytest.mark.asyncio
async def test_infer_raises_on_400() -> None:
    client = HuggingFaceClient(token="t", timeout=5.0)
    try:
        with respx.mock(base_url="https://router.huggingface.co/hf-inference") as mock:
            mock.post("/models/m").mock(return_value=Response(400, text="bad input"))
            with pytest.raises(HFInferenceError, match="HF 400"):
                await client.infer("m", json_payload={"inputs": "x"})
    finally:
        await client.aclose()
