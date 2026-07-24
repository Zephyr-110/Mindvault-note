package org.example.mindvaultaiapi.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteSessionDTO {
    @NotNull(message = "会话id不能为空")
    private Long sessionId;
    private Long page = 1L;
    private Long size = 10L;
}
