package org.example.note.tag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteTagDTO {
    @NotNull(message = "标签ID不能为空")
    private Long tagId;//标签id，必填
}
