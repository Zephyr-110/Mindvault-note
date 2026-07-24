package org.example.mindvaultaiapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    /*
    system = 系统提示词，告诉 AI 你是谁、要做什么、给它上下文（用户笔记）
    user = 用户说的话
    assistant = AI 回复的话
     */
    private String role;//角色：system / user / assistant
    private String content;//消息内容
}