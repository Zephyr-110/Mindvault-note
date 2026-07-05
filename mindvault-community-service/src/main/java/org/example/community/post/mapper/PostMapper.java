package org.example.community.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.community.post.entity.Post;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    Page<Post> selectFeedPage(Page<Post> page, @Param("visibility") Long visibility);
}