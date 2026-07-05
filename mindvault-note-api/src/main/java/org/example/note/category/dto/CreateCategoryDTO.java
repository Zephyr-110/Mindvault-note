package org.example.note.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryDTO {

    @NotBlank(message = "目录名不能为空")
    private String name;//必填

    private Long parentId;//父级目录id，如果为null，则该目录为根目录
}
