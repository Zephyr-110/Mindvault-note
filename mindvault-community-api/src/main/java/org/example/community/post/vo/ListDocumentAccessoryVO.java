package org.example.community.post.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListDocumentAccessoryVO {

    private Long documentId;//笔记附件id
    private String title;//笔记附件标题
}
