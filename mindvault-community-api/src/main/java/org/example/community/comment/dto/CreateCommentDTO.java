package org.example.community.comment.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCommentDTO {

    @NotNull(message = "帖子id不能为空")
    private Long postId;//帖子id
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论最长1000字")
    private String content;//评论内容
    private Long parentId;//父评论id，如果为null则为根评论
}
