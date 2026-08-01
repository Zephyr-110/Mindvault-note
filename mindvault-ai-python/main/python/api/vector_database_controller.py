from fastapi import APIRouter

from models.delete_vector_document_dto import DeleteDocumentsEmbeddingDTO
from models.delete_vector_memories_dto import DeleteConversationMemoryEmbeddingDTO
from models.to_embedding_documents_dto import ToEmbeddingDocumentsDTO
from models.to_embedding_memories_dto import ToEmbeddingMemoriesDTO
from service.vector_database_service import VectorService


vector_router = APIRouter()
vector_service = VectorService()


@vector_router.post("/api/ai/to-documents-embedding")
def set_documents_embedding(dto: ToEmbeddingDocumentsDTO):
    return vector_service.set_documents_embedding(dto)


@vector_router.post("/api/ai/to-conversation_memory-embedding")
def set_conversation_memory_embedding(dto: ToEmbeddingMemoriesDTO):
    return vector_service.set_conversation_memory_embedding(dto)

@vector_router.delete("/api/ai/delete-documents-embedding")
def delete_documents_embedding(dto: DeleteDocumentsEmbeddingDTO):
    return vector_service.delete_documents_embedding(dto)

@vector_router.put("/api/ai/update-documents-embedding")
def update_documents_embedding(dto: ToEmbeddingDocumentsDTO):
    return vector_service.update_documents_embedding(dto)

@vector_router.delete("/api/ai/delete-conversation_memory-embedding")
def delete_conversation_memory_embedding(dto: DeleteConversationMemoryEmbeddingDTO):
    return vector_service.delete_conversation_memory_embedding(dto)

