package org.example.community.likerecord.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckComment;
import org.example.common.annotation.CheckPost;
import org.example.common.redis.BitmapCounter;
import org.example.community.comment.entity.Comment;
import org.example.community.comment.mapper.CommentMapper;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.community.likerecord.dto.LikeCountDTO;
import org.example.community.likerecord.dto.ToggleLikeDTO;
import org.example.community.likerecord.entity.LikeRecord;
import org.example.community.likerecord.LikeRecordService;
import org.example.community.likerecord.mapper.LikeRecordMapper;
import org.example.community.notification.NotificationService;
import org.example.community.likerecord.vo.LikeCountVO;
import org.example.community.likerecord.vo.LikeVO;
import org.example.community.post.entity.Post;
import org.example.community.post.mapper.PostMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeRecordServiceImpl implements LikeRecordService {
    //缓存键值模板
    private static final String REDIS_LIKE_MEMBERS = "like:members:%s:%d";
    private static final String REDIS_LIKE_COUNT   = "like:count:%s:%d";

    private final LikeRecordMapper likeRecordMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final NotificationService notificationService;
    private final CacheManager cacheManager;
    private final BitmapCounter bitmapCounter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeVO toggleLike(ToggleLikeDTO toggleLikeDTO) {
        //初始化返回值
        LikeVO likeVO = new LikeVO();
        String targetType = toggleLikeDTO.getTargetType();
        Long targetId = toggleLikeDTO.getTargetId();
        Long userId = UserContext.getUserId();
        //如果目标不存在或者不可见，则直接返回刚创建的 LikeVO
        if (!checkTargetVisibility(targetType, targetId)) {
            return likeVO;
        }
        // 拼 Redis key，冷启动兜底
        String membersKey = membersKey(targetType, targetId);
        String countKey = countKey(targetType, targetId);
        bitmapCounter.ensureInit(membersKey, countKey, () ->
                likeRecordMapper.selectList(
                        new LambdaQueryWrapper<LikeRecord>()
                                .eq(LikeRecord::getTargetType, targetType)
                                .eq(LikeRecord::getTargetId, targetId))
                        .stream()
                        .map(LikeRecord::getUserId)
                        .toList()
        );
        // 从 Redis 查当前状态
        boolean alreadyLiked = bitmapCounter.contains(membersKey, userId);
        // 写 DB（DB 持久化）
        boolean newLiked = toggleLikeInternal(targetType, targetId, alreadyLiked);
        // 更新 Redis
        if (newLiked) {
            bitmapCounter.add(membersKey, countKey, userId);
        } else {
            bitmapCounter.remove(membersKey, countKey, userId);
        }
        // 从 Redis 拿计数
        long count = bitmapCounter.getCount(countKey);
        if (newLiked) {
            Long authorId = getTargetAuthorId(targetType, targetId);
            if (authorId != null) {
                notificationService.createNotification(authorId, "like",
                        UserContext.getUserId(), targetId);
            }
        }
        //如果点赞的是帖子，手动清除帖子详情缓存
        if ("post".equals(targetType)) {
            Cache cache = cacheManager.getCache("postDetail");
            if (cache != null) {
                cache.evict(targetId);
            }
        }
        likeVO.setIsLiked(newLiked);
        likeVO.setLikeCount(count);
        return likeVO;
    }

    /**
     * 校验目标是否存在且可见
     * @return true = 校验通过，false = 校验失败（不会抛异常，因为前面已经抛了）
     */
    private boolean checkTargetVisibility(String targetType, Long targetId) {
        if ("post".equals(targetType)) {
            Post post = postMapper.selectById(targetId);
            if (post == null) {
                throw new BusinessException(404, "帖子不存在");
            }
            if (!post.getVisibility().equals(0L)) {
                throw new BusinessException(400, "帖子不可见");
            }
            return true;
        } else if ("comment".equals(targetType)) {
            Comment comment = commentMapper.selectById(targetId);
            if (comment == null) {
                throw new BusinessException(404, "评论不存在");
            }
            Post post = postMapper.selectById(comment.getPostId());
            if (post == null) {
                throw new BusinessException(404, "帖子不存在");
            }
            if (!post.getVisibility().equals(0L)) {
                throw new BusinessException(400, "帖子不可见");
            }
            return true;
        } else {
            throw new BusinessException(400, "点赞目标类型不存在");
        }
    }

    /**
     * 切换点赞状态（新增/删除点赞记录）
     * @param alreadyLiked 从 Redis 查到的当前状态
     * @return 操作后是否已点赞
     */
    private boolean toggleLikeInternal(String targetType, Long targetId, boolean alreadyLiked) {
        Long userId = UserContext.getUserId();

        if (alreadyLiked) {
            if ("post".equals(targetType)) {
                likeRecordMapper.deleteByUserAndPostId(userId, targetId);
                log.info("用户 {} 取消点赞帖子 {}", userId, targetId);
            } else {
                likeRecordMapper.deleteByUserAndCommentId(userId, targetId);
                log.info("用户 {} 取消点赞评论 {}", userId, targetId);
            }
            return false;
        } else {
            LikeRecord record = createLikeRecord(targetType, targetId);
            likeRecordMapper.insert(record);
            if ("post".equals(targetType)) {
                log.info("用户 {} 点赞了帖子 {}", userId, targetId);
            } else {
                log.info("用户 {} 点赞了评论 {}", userId, targetId);
            }
            return true;
        }
    }

    /**
     * 创建LikeRecord基础对象
     */
    private LikeRecord createLikeRecord(String targetType, Long targetId) {
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setTargetType(targetType);
        likeRecord.setTargetId(targetId);
        likeRecord.setUserId(UserContext.getUserId());
        likeRecord.setCreateTime(LocalDateTime.now());
        return likeRecord;
    }

    /**
     * 查询当前用户是否已点赞
     */
    private Long getTargetAuthorId(String targetType, Long targetId) {
        if ("post".equals(targetType)) {
            Post post = postMapper.selectById(targetId);
            return post != null ? post.getAuthorId() : null;
        } else {
            Comment comment = commentMapper.selectById(targetId);
            return comment != null ? comment.getAuthorId() : null;
        }
    }

    private String membersKey(String targetType, Long targetId) {
        return String.format(REDIS_LIKE_MEMBERS, targetType, targetId);
    }

    private String countKey(String targetType, Long targetId) {
        return String.format(REDIS_LIKE_COUNT, targetType, targetId);
    }

    @Override
    public LikeCountVO getLikeCount(LikeCountDTO likeCountDTO) {
        String targetType = likeCountDTO.getTargetType();
        Long targetId = likeCountDTO.getTargetId();
        Long userId = UserContext.getUserId();

        checkTargetVisibility(targetType, targetId);

        // 冷启动兜底
        String membersKey = membersKey(targetType, targetId);
        String countKey = countKey(targetType, targetId);
        bitmapCounter.ensureInit(membersKey, countKey, () ->
                likeRecordMapper.selectList(
                        new LambdaQueryWrapper<LikeRecord>()
                                .eq(LikeRecord::getTargetType, targetType)
                                .eq(LikeRecord::getTargetId, targetId))
                        .stream()
                        .map(LikeRecord::getUserId)
                        .toList()
        );

        LikeCountVO likeCountVO = new LikeCountVO();
        likeCountVO.setTargetType(targetType);
        likeCountVO.setTargetId(targetId);
        likeCountVO.setLikeCount(bitmapCounter.getCount(countKey));
        likeCountVO.setLiked(bitmapCounter.contains(membersKey, userId));
        return likeCountVO;
    }

    // ==================== 批量查询（供 getFeed 调用） ====================

    @Override
    public Set<Long> batchLikedPostIds(Long userId, List<Long> postIds) {
        return postIds.stream()
                .filter(pid -> bitmapCounter.contains(membersKey("post", pid), userId))
                .collect(Collectors.toSet());
    }

    @Override
    public Map<Long, Long> batchLikeCounts(List<Long> postIds) {
        return postIds.stream()
                .collect(Collectors.toMap(
                        pid -> pid,
                        pid -> bitmapCounter.getCount(countKey("post", pid))));
    }
}