package org.example.community.post.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeletePostDTO {

    @NotNull(message = "帖子ID不能为空")
    private Long postId;
}
