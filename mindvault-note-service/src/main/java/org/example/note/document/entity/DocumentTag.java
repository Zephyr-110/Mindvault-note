package org.example.note.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("document_tag")
public class DocumentTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Long tagId;
}