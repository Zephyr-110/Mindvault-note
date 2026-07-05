package org.example.note.category.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryVO {

    private Long id;//该目录id
    private String name;//该目录名称
    private Long parentId;//该目录的父目录id
    private Long userId;//该目录所属用户id
    private LocalDateTime createTime;//该目录创建时间
}
