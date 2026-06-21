from typing import Literal

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(max_length=2000)


class ChatContext(BaseModel):
    summoner_name: str | None = Field(default=None, max_length=64)
    tag_line: str | None = Field(default=None, max_length=16)
    stats_summary: str | None = Field(default=None, max_length=512)
    good_traits: list[str] | None = Field(default=None, max_length=10)
    bad_traits: list[str] | None = Field(default=None, max_length=10)
    recent_matches: str | None = Field(default=None, max_length=5000)
    top_champions: list[str] | None = Field(default=None, max_length=10)


class ChatRequest(BaseModel):
    messages: list[ChatMessage] = Field(default_factory=list, max_length=20)
    context: ChatContext | None = None


class ChatResponse(BaseModel):
    reply: str
