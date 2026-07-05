package org.example.community.comment.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteCommentDTO {

    @NotNull(message = "评论id不能为空")
    private Long id;
}
