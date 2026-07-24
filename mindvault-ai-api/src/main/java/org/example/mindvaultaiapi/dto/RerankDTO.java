package org.example.mindvaultaiapi.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RerankDTO {

    private String query;// 用户搜索的关键词
    private List<RerankSourceItem> sources;// 待排序的文档列表
    private int topK; // 返回的前几条排序结果数量
    private String userId; // 谁在搜索
}
