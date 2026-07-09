package org.example.user.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.user.user.dto.UpdateUserProfileDTO;
import org.example.user.user.entity.UserProfile;

import java.util.List;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
    //通过用户id查询回显用户信息
    UserProfile selectByUserId(Long userId);

    //更新用户信息
    void updateUserProfile(@Param("userId") Long userId, @Param("dto") UpdateUserProfileDTO dto);

    List<UserProfile> searchByNicknameOrUsername(Page<UserProfile> page, @Param("keyword") String keyword, @Param("size") Long size);

    Long countByNicknameOrUsername(@Param("keyword") String keyword);
}