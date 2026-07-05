package org.example.community.send.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageDTO {

    @NotNull(message = "接收者id不能为空")
    private Long receiverId;//接收者id
    private Long senderId;//发送者id（后端从Token获取，前端传值仅用于WebSocket路由）
    @NotBlank(message = "内容不能为空")
    private String content;//内容
}