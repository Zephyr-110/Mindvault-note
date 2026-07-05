package org.example.note.document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestoreDocumentDTO {
    @NotNull(message = "文档ID不能为空")
    private Long documentId;//文档id，必填
}
