package org.example.community.likerecord.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToggleLikeDTO {
    @NotBlank(message = "点赞目标类型不能为空")
    private String targetType;
    @NotNull(message = "点赞目标ID不能为空")
    private Long targetId;
}
