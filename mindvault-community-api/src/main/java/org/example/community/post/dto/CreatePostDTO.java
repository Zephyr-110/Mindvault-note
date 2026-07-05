package org.example.community.post.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.community.post.vo.ListDocumentAccessoryVO;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题最长50字")
    private String title;
    @NotBlank(message = "内容不能为空")
    @Size(max = 1000, message = "内容最长1000字")
    private String content;
    private Long visibility = 0L;//默认全部可见
    private Long originalPostId;//可选，为空就是创建新帖
    private List<ListDocumentAccessoryVO> documentAccessories;//笔记附件列表
}
