package org.example.note.tag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTagDTO {

    @NotBlank(message = "标签名不能为空")
    private String name;//标签名称，必填
}
