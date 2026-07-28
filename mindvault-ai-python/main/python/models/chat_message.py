from typing import Optional, Any

from pydantic import BaseModel


class ChatMessage(BaseModel):
    role: str
    content: str
    tool_calls: Optional[list[dict[str, Any]]] = None
    tool_call_id: Optional[str] = None
    name: Optional[str] = None
