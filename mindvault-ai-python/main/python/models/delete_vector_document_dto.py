from pydantic import BaseModel


class DeleteDocumentsEmbeddingDTO(BaseModel):
    id: str
    type: str