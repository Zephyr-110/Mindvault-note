package org.example.mindvaultaiapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListMessagesHistoryDTO {
    @NotNull(message = "会话id不能为空")
    private Long sessionId;
    private Integer page = 1;
    private Integer size = 20;
}
