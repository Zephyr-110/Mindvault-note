package org.example.note.document.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListDocumentDTO {

    private Long userId;//可选，指定用户id，为空则取当前用户
    private Long categoryId;//可选
    private String keyword;//可选
    @NotNull @Min(1)
    private Long page = 1L;
    @NotNull @Min(1) @Max(100)
    private Long size = 10L;
}
