package org.example.note.document.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDocumentDTO {

    @NotNull(message = "文档ID不能为空")
    private Long documentId;//文档ID，必填
    private String title;//文档标题
    private String content;//文档内容
    @NotNull(message = "文档所属目录ID不能为空")
    private Long categoryId;//文档所属目录id，必填
    private List<Long> tagIds;//文档标签id列表
}
