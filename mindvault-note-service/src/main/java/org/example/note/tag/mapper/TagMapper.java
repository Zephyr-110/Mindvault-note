package org.example.note.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.note.tag.entity.Tag;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    // 根据标签名、用户ID查询标签
    Tag selectByNameAndUserId(@Param("name") String name, @Param("userId") Long userId);
    // 查询标签
    List<Tag> selectByUserId(@Param("userId") Long userId);
    // 根据标签ID删除文档标签关系
    int deleteDocumentTagsByTagId(@Param("tagId") Long tagId);
}