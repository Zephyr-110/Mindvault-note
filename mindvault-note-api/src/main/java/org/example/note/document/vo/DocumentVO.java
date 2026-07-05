package org.example.note.document.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentVO {
    private Long id;//该文档id
    private String title;//文档标题
    private String content;//文档内容
    private Long categoryId;//文档目录id
    private Long userId;//文档所属用户id
    private LocalDateTime createTime;//文档创建时间
    private LocalDateTime updateTime;//文档更新时间
    private String preview;//预览片段，可以为空，为空时是没有搜索
}
