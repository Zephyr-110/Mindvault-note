package org.example.user.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;//用户名，必填
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 18, message = "密码长度必须在6-18位之间")
    private String password;//密码，必填
}