package org.example.community.comment.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListCommentDTO {

    @NotNull(message = "帖子id不能为空")
    private Long postId;//帖子id
    @NotNull @Min(1)
    private Long page = 1L;//页码
    @NotNull @Min(1) @Max(100)
    private Long size = 10L;//每页评论条数
}
