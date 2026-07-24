package org.example.mindvaultaiapi.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RerankSourceItem {

    private String type;// "post" / "note"
    private String id;// 唯一标识，跳转详情页用
    private String title;// 标题
    private String content;// 正文 / 笔记内容
}
