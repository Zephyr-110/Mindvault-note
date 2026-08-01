package org.example.mindvaultaiapi.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSessionTitleDTO {
    @NotBlank(message = "新标题不能为空")
    private String newTitle;
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;
    private Long page = 1L;
    private Long size = 10L;
}
