package org.example.community.send.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadDTO {
    @NotNull(message = "对方id不能为空")
    private Long withUserId;//与谁聊天
}
