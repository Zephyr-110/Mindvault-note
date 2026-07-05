package org.example.note.document.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.note.tag.vo.TagVO;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDetailVO {

    private Long id;//文档id
    private String title;//文档标题
    private String content;// Markdown 内容
    private Long categoryId;//所属目录
    private Long userId;//用户id
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//修改时间
    private List<TagVO> tags;// 标签
}
