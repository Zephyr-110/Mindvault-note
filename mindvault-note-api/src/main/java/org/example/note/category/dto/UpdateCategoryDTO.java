package org.example.note.category.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCategoryDTO {//重命名目录
    @NotNull(message = "重命名目录id不能为空")
    private Long id;//目录id，必填
    @NotBlank(message = "新目录名不能为空")
    private String name;//新名字，必填
}
