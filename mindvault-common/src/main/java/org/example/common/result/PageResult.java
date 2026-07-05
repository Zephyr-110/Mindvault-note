package org.example.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {

    private Long total;//总条数
    private Long page;//当前页是第几页
    private Long size;//每一页的条数
    private List<T> list = new ArrayList<>();//当前页的数据
}
