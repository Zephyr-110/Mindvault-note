from pydantic import BaseModel, Field


class ToEmbeddingMemoriesDTO(BaseModel):
    id: str
    user_id: str = Field(alias="userId")
    session_id: str = Field(alias="sessionId")
    type: str
    text: str
    timestamp: int