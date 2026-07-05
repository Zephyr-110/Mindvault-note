package org.example.user.setting.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserSettingDTO {
    @NotBlank(message = "设置键不能为空")
    private String settingKey; // 设置键
    @NotBlank(message = "设置值不能为空")
    private String settingValue; // 设置值
}
