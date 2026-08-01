from typing import Optional

from pydantic import BaseModel, Field


class AgentChatResponse(BaseModel):
    content: str
    done: bool = False
    session_id: Optional[str] = Field(default=None, alias="sessionId")
    error: Optional[str] = None