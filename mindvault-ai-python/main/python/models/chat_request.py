from typing import List, Optional
from pydantic import BaseModel, Field

from main.python.models.chat_message import ChatMessage


class ChatRequest(BaseModel):
    messages: List[ChatMessage]
    stream: bool = True
    session_id: Optional[str] = Field(default=None, alias="sessionId")
    user_id: Optional[str] = Field(default=None, alias="userId")
    regenerate: bool = False  # 是否重新生成
    edit_message_id: Optional[str] = None  # 被修改的消息 ID