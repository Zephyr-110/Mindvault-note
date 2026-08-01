package org.example.mindvaultaiapi.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestJavaAPIDTO {
    @NotBlank(message = "用户输入的关键词不能为空")
    private String query ;// 用户输入的文本
    private String sessionId ;// 会话id
    private Boolean stream = true;// 是否开启流式输出
}
