package org.example.community.likerecord;

import org.example.community.likerecord.dto.LikeCountDTO;
import org.example.community.likerecord.dto.ToggleLikeDTO;
import org.example.community.likerecord.vo.LikeCountVO;
import org.example.community.likerecord.vo.LikeVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LikeRecordService {
    LikeVO toggleLike(ToggleLikeDTO toggleLikeDTO);

    LikeCountVO getLikeCount(LikeCountDTO likeCountDTO);

    /**
     * 批量查询：当前用户点赞了哪些帖子（走 Redis）
     */
    Set<Long> batchLikedPostIds(Long userId, List<Long> postIds);

    /**
     * 批量查询：所有帖子的点赞数（走 Redis）
     */
    Map<Long, Long> batchLikeCounts(List<Long> postIds);
}