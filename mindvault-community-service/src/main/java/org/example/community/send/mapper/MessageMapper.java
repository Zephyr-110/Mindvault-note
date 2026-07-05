package org.example.community.send.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.community.send.entity.Message;


@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
