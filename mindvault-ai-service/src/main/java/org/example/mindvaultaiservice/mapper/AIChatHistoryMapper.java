package org.example.mindvaultaiservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.mindvaultaiservice.entity.AIChatHistory;

@Mapper
public interface AIChatHistoryMapper extends BaseMapper<AIChatHistory> {
}