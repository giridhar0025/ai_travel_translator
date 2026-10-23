"""Errors shared by the interchangeable LLM chat-completion backends.

Translation can be served by the Hugging Face Inference router or by Groq.
Both raise a subclass of :class:`LLMInferenceError` so routers can map any
upstream provider failure to a single 502 without knowing which one is wired.
"""


class LLMInferenceError(Exception):
    """An upstream LLM provider returned a non-retryable error response."""
