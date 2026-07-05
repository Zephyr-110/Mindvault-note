package org.example.user.block.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IsBlockedDTO {
    @NotNull(message = "用户id不能为空")
    private Long userId;
}
