package org.example.note.category.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestoreCategoryDTO {
    @NotNull(message = "目录id不能为空")
    private Long id;
}
