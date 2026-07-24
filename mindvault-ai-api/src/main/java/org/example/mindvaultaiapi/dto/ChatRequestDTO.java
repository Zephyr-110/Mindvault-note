package org.example.mindvaultaiapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestDTO {
    private List<ChatMessageDTO> messages;//整个对话历史
    private boolean stream = true;//是否流式返回
    private String userId;//用户id
}