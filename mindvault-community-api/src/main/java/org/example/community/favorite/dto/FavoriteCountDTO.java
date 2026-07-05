package org.example.community.favorite.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteCountDTO {

    @NotNull(message = "帖子id不能为空")
    private Long postId;
}
