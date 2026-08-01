from pydantic import BaseModel, Field


class ToEmbeddingDocumentsDTO(BaseModel):
    id: str
    user_id: str = Field(alias="userId")
    source_type: str = Field(alias="sourceType")
    title: str
    content: str
    created_at: int = Field(alias="createdAt")