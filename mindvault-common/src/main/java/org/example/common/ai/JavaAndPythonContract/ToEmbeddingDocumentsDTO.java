package org.example.common.ai.JavaAndPythonContract;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToEmbeddingDocumentsDTO {

    private String id;//资源id
    private String userId;//用户id
    private String sourceType;//资源类型
    private String title;//标题
    private String content;//内容
    private Integer createdAt;//创建时间
}
