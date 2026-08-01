package org.example.common.ai.JavaAndPythonContract;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToEmbeddingMemoriesDTO {

    private String id;
    private String userId;
    private String sessionId;
    private String type;
    private String text;
    private Long timestamp;
}
