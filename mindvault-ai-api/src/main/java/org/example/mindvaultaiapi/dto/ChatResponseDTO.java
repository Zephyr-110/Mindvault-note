package org.example.mindvaultaiapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponseDTO {
    private String content;//AI生成的内容
    private boolean done;//是否生成完毕
    private String error;//错误信息
}