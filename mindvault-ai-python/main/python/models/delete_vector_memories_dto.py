from typing import Optional

from pydantic import BaseModel, Field


class DeleteConversationMemoryEmbeddingDTO(BaseModel):
    session_id: Optional[str] = Field(default=None, alias="sessionId")
    user_id: Optional[str] = Field(default=None, alias="userId")
