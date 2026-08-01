package org.example.common.ai.client;

import lombok.RequiredArgsConstructor;
import org.example.common.ai.JavaAndPythonContract.DeleteDocumentsEmbeddingDTO;
import org.example.common.ai.JavaAndPythonContract.DeleteMemoriesEmbeddingDTO;
import org.example.common.ai.JavaAndPythonContract.ToEmbeddingDocumentsDTO;
import org.example.common.ai.JavaAndPythonContract.ToEmbeddingMemoriesDTO;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PythonVectorClient {

    private final PythonHttpClient pythonHttpClient;

    public void toEmbeddingDocuments(ToEmbeddingDocumentsDTO request) {
        pythonHttpClient.requestJson("/api/ai/to-documents-embedding", request, "POST");
    }

    public void toEmbeddingMemories(ToEmbeddingMemoriesDTO request) {
        pythonHttpClient.requestJson("/api/ai/to-conversation_memory-embedding", request, "POST");
    }

    public void deleteDocumentsEmbedding(DeleteDocumentsEmbeddingDTO request) {
        pythonHttpClient.requestJson("/api/ai/delete-documents-embedding", request, "DELETE");
    }

    public void updateDocumentsEmbedding(ToEmbeddingDocumentsDTO request) {
        pythonHttpClient.requestJson("/api/ai/update-documents-embedding", request, "PUT");
    }

    public void deleteMemoriesEmbedding(DeleteMemoriesEmbeddingDTO request) {
        pythonHttpClient.requestJson("/api/ai/delete-conversation_memory-embedding", request, "DELETE");
    }
}
