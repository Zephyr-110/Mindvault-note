package org.example.note.document.dto;


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
    private Long page;//默认1
    private Long size;//默认10
}
