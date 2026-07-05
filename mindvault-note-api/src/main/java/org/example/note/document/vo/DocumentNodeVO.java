package org.example.note.document.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentNodeVO {

    private Long documentId;//文档id
    private String title;//文档标题
}
