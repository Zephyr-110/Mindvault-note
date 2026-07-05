package org.example.user.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.user.user.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 根据用户名查询用户
    User selectByUsername(String username);
}