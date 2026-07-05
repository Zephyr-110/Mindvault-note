package org.example.user.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;//用户名，必填
    @NotBlank(message = "密码不能为空")
    private String password;//密码，必填
}