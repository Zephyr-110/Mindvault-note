package org.example.user.block.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockUserDTO {
    @NotNull(message = "被拉黑用户ID不能为空")
    private Long userId;
}
