from pydantic import BaseModel


class ToEmbeddingDTO(BaseModel):
    id: str
    user_id: str
    source_type: str
    title: str
    content: str
    created_at: int