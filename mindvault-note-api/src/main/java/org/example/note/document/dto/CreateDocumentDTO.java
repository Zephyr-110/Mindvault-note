package org.example.note.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDocumentDTO {

    @NotBlank(message = "文档标题不能为空")
    private String title;//文档标题，必填

    private String content;//文档内容

    @NotNull(message = "文档所属目录不能为空")
    private Long categoryId;//文档所属目录ID，必填

    private List<Long> tagIds;//文档标签ID列表
}
