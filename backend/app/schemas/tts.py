from typing import Literal

from pydantic import BaseModel, Field


class SynthesizeRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=500)
    language: str = Field(..., min_length=2, max_length=8)
    speed: Literal["normal", "slow"] = "normal"
