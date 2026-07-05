package org.example.note.category.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.note.document.vo.DocumentNodeVO;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryTreeNodeVO {
    private Long id;//目录id
    private String name;//目录名
    private Long parentId;//父目录id
    //该目录节点下的子节点VO组成的集合
    private List<CategoryTreeNodeVO> children = new ArrayList<>();
    private List<DocumentNodeVO> documents = new ArrayList<>();
}
