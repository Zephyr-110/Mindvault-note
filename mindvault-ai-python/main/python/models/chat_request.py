from typing import List, Optional
from pydantic import BaseModel, Field

from main.python.models.chat_message import ChatMessage


class ChatRequest(BaseModel):
    messages: List[ChatMessage]
    stream: bool = True
    user_id: Optional[str] = Field(default=None, alias="userId")