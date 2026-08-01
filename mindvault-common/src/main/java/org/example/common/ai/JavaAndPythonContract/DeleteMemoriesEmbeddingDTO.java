package org.example.common.ai.JavaAndPythonContract;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteMemoriesEmbeddingDTO {

    private String sessionId;//会话id
    private String userId;//用户id
}
