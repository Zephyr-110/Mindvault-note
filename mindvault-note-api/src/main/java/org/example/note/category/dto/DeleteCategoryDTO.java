package org.example.note.category.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteCategoryDTO {

    @NotNull(message = "目录ID不能为空")
    private Long id;//目录id，必填

    private Integer force;//如果有子目录，前端会提醒用户是否强制连下边的目录全部删除，是否强制删除,1为强制删除，0为不强制删除

}
