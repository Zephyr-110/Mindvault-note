from pymilvus import MilvusClient

from models.delete_vector_document_dto import DeleteDocumentsEmbeddingDTO
from models.delete_vector_memories_dto import DeleteConversationMemoryEmbeddingDTO
from models.to_embedding_documents_dto import ToEmbeddingDocumentsDTO
from models.to_embedding_memories_dto import ToEmbeddingMemoriesDTO
from main.python.common.vector.vector_util import VectorUtil


class VectorService:

    def __init__(self):
        self.client = MilvusClient()
        self.vector_util = VectorUtil()
        self._init_collections()

    def _init_collections(self):
        index_params = self.client.prepare_index_params()
        index_params.add_index(
            field_name="vector",
            index_type="AUTOINDEX",
            metric_type="COSINE"
        )
        for collection_name in ["documents", "conversation_memory"]:
            try:
                self.client.create_index(collection_name=collection_name, index_params=index_params)
            except Exception:
                pass
        self.client.load_collection(collection_name="documents")
        self.client.load_collection(collection_name="conversation_memory")

    def set_documents_embedding(self, dto: ToEmbeddingDocumentsDTO):
        text = f"{dto.title}\n{dto.content}"
        text = self.vector_util._truncate_by_bytes(text, 500)
        vector = self.vector_util.get_embedding(text)
        data = {
            "id": f"note_{dto.id}",
            "vector": vector,
            "user_id": str(dto.user_id),
            "source_type": dto.source_type,
            "text": text,
            "created_at": dto.created_at
        }
        try:
            self.client.insert(collection_name="documents", data=[data])
            print(f"[VectorService] 插入文档成功: id={dto.id}")
        except Exception as e:
            print(f"[VectorService] 插入文档失败: id={dto.id}, error={e}")
            raise

    def set_conversation_memory_embedding(self, dto: ToEmbeddingMemoriesDTO):
        text = self.vector_util._truncate_by_bytes(dto.text, 500)
        vector = self.vector_util.get_embedding(text)
        data = {
            "id": f"memory_{dto.id}",
            "vector": vector,
            "user_id": str(dto.user_id),
            "session_id": dto.session_id,
            "type": dto.type,
            "text": text,
            "timestamp": dto.timestamp,
        }
        try:
            self.client.insert(collection_name="conversation_memory", data=[data])
            print(f"[VectorService] 插入记忆成功: id={dto.id}")
        except Exception as e:
            print(f"[VectorService] 插入记忆失败: id={dto.id}, error={e}")
            raise

    def delete_documents_embedding(self, dto: DeleteDocumentsEmbeddingDTO):
        try:
            if dto.type == "note":
                self.client.delete(collection_name="documents", filter=f"id == 'note_{dto.id}'")
            elif dto.type == "memory":
                self.client.delete(collection_name="conversation_memory", filter=f"id == 'memory_{dto.id}'")
            print(f"[VectorService] 删除成功: id={dto.id}, type={dto.type}")
        except Exception as e:
            print(f"[VectorService] 删除失败: id={dto.id}, type={dto.type}, error={e}")
            raise

    def update_documents_embedding(self, dto: ToEmbeddingDocumentsDTO):
        text = f"{dto.title}\n{dto.content}"
        text = self.vector_util._truncate_by_bytes(text, 500)
        vector = self.vector_util.get_embedding(text)
        data = {
            "id": f"note_{dto.id}",
            "vector": vector,
            "user_id": str(dto.user_id),
            "source_type": dto.source_type,
            "text": text,
            "created_at": dto.created_at
        }
        try:
            self.client.upsert(collection_name="documents", data=[data])
            print(f"[VectorService] 更新文档成功: id={dto.id}")
        except Exception as e:
            print(f"[VectorService] 更新文档失败: id={dto.id}, error={e}")
            raise

    def delete_conversation_memory_embedding(self, dto: DeleteConversationMemoryEmbeddingDTO):
        try:
            conditions = []
            if dto.user_id:
                conditions.append(f'user_id == "{dto.user_id}"')
            if dto.session_id:
                conditions.append(f'session_id == "{dto.session_id}"')
            expr = " and ".join(conditions) if conditions else None
            if not expr:
                print("[VectorService] 删除记忆失败: 缺少 user_id 或 session_id")
                return
            self.client.delete(collection_name="conversation_memory", filter=expr)
            print(f"[VectorService] 批量删除记忆成功: expr={expr}")
        except Exception as e:
            print(f"[VectorService] 批量删除记忆失败: {e}")
            raise