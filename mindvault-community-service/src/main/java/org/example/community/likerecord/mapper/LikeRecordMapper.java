package org.example.community.likerecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.community.likerecord.entity.LikeRecord;

@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {

    LikeRecord selectByUserAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    int deleteByUserAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    long selectCountByPostId(@Param("postId") Long postId);

    LikeRecord selectByUserAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    int deleteByUserAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    long selectCountByCommentId(@Param("commentId") Long commentId);
}