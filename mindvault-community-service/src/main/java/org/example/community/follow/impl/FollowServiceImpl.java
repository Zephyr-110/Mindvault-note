package org.example.community.follow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.common.redis.BitmapCounter;
import org.example.user.user.service.UserService;
import org.example.community.follow.dto.ListFolloweeDTO;
import org.example.community.follow.dto.ListFollowerDTO;
import org.example.community.follow.dto.ToggleFollowDTO;
import org.example.community.follow.entity.Follow;
import org.example.community.follow.FollowService;
import org.example.community.follow.mapper.FollowMapper;
import org.example.community.notification.NotificationService;
import org.example.community.follow.vo.FollowUserVO;
import org.example.community.follow.vo.FollowVO;
import org.example.common.result.PageResult;
import org.example.community.send.entity.Message;
import org.example.community.send.mapper.MessageMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private static final String REDIS_FOLLOWER_MEMBERS = "follow:members:follower:%d";
    private static final String REDIS_FOLLOWER_COUNT   = "follow:count:follower:%d";
    private static final String REDIS_FOLLOWEE_MEMBERS = "follow:members:followee:%d";
    private static final String REDIS_FOLLOWEE_COUNT   = "follow:count:followee:%d";

    private final FollowMapper followMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final BitmapCounter bitmapCounter;
    private final MessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "feed", allEntries = true),
            @CacheEvict(value = "postDetail", allEntries = true)
    })
    public FollowVO toggleFollow(ToggleFollowDTO dto) {
        if(dto.getFolloweeId().equals(UserContext.getUserId())){
            throw new BusinessException(400, "不能关注自己");
        }
        if (!userService.existsById(dto.getFolloweeId())) {
            throw new BusinessException(404, "对方用户不存在");
        }

        Long userId = UserContext.getUserId();
        Long followeeId = dto.getFolloweeId();

        // 拼双向 Redis key，冷启动兜底
        String followerMembersKey = followerMembersKey(userId);
        String followerCountKey = followerCountKey(userId);
        String followeeMembersKey = followeeMembersKey(followeeId);
        String followeeCountKey = followeeCountKey(followeeId);

        bitmapCounter.ensureInit(followerMembersKey, followerCountKey,
                () -> followMapper.selectList(
                        new LambdaQueryWrapper<Follow>()
                                .eq(Follow::getFollowerId, userId))
                        .stream().map(Follow::getFolloweeId).toList());
        bitmapCounter.ensureInit(followeeMembersKey, followeeCountKey,
                () -> followMapper.selectList(
                        new LambdaQueryWrapper<Follow>()
                                .eq(Follow::getFolloweeId, followeeId))
                        .stream().map(Follow::getFollowerId).toList());

        // 从 Redis 查当前状态
        boolean alreadyFollowed = bitmapCounter.contains(followerMembersKey, followeeId);

        FollowVO followVO = new FollowVO();

        if (alreadyFollowed) {
            // 取消关注
            followMapper.delete(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowerId, userId)
                    .eq(Follow::getFolloweeId, followeeId));
            log.info("用户 {} 取消关注了用户 {}", userId, followeeId);
            bitmapCounter.remove(followerMembersKey, followerCountKey, followeeId);
            bitmapCounter.remove(followeeMembersKey, followeeCountKey, userId);
            followVO.setFollowed(false);
        } else {
            // 关注前先查DB防止重复关注（兜底Redis不一致的情况）
            Long existCount = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowerId, userId)
                    .eq(Follow::getFolloweeId, followeeId));
            if (existCount > 0) {
                // DB已有记录但Redis未同步，修复Redis后返回已关注
                bitmapCounter.add(followerMembersKey, followerCountKey, followeeId);
                bitmapCounter.add(followeeMembersKey, followeeCountKey, userId);
                followVO.setFollowed(true);
                followVO.setFollowerCount(bitmapCounter.getCount(followeeCountKey));
                followVO.setFollowingCount(bitmapCounter.getCount(followerCountKey));
                return followVO;
            }
            // 关注
            Follow follow = new Follow();
            follow.setFollowerId(userId);
            follow.setFolloweeId(followeeId);
            follow.setCreateTime(LocalDateTime.now());
            followMapper.insert(follow);
            log.info("用户 {} 关注了用户 {}", userId, followeeId);
            notificationService.createNotification(followeeId, "follow", userId, followeeId);
            bitmapCounter.add(followerMembersKey, followerCountKey, followeeId);
            bitmapCounter.add(followeeMembersKey, followeeCountKey, userId);
            followVO.setFollowed(true);

            // 互关后自动发送消息
            if (isMutualFollow(userId, followeeId)) {
                try {
                    Message msg = new Message();
                    msg.setSenderId(followeeId);
                    msg.setReceiverId(userId);
                    msg.setContent("我已经回关你了，我们开始聊天吧！");
                    msg.setIsRead(false);
                    msg.setCreateTime(LocalDateTime.now());
                    messageMapper.insert(msg);
                    log.info("互关消息已发送: {} → {}", followeeId, userId);
                } catch (Exception e) {
                    log.error("互关消息发送失败: {} → {}, 原因: {}", followeeId, userId, e.getMessage());
                }
            }
        }

        followVO.setFollowerCount(bitmapCounter.getCount(followeeCountKey));
        followVO.setFollowingCount(bitmapCounter.getCount(followerCountKey));
        return followVO;
    }

    //我关注了谁
    @Override
    public PageResult<FollowUserVO> listFollowee(ListFolloweeDTO dto){
        Long targetUserId = dto.getUserId() != null ? dto.getUserId() : UserContext.getUserId();
        //查询分页列表
        Page<Follow> page = new Page<>(dto.getPage(), dto.getSize());
        Page<Follow> result  = followMapper.selectPage(page,
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, targetUserId)
                        .orderByDesc(Follow::getCreateTime));
        //组装返回值列表返回
        List<FollowUserVO> voList = result.getRecords().stream().map(follow -> {
                FollowUserVO vo = new FollowUserVO();
                vo.setUserId(follow.getFolloweeId());
                vo.setNickname(userService.getNickname(follow.getFolloweeId()));
                vo.setAvatar(userService.getAvatar(follow.getFolloweeId()));
                vo.setBio(null);
                //判断对方是否回关
                Follow ifFollow = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId,follow.getFolloweeId() )
                        .eq(Follow::getFolloweeId, UserContext.getUserId()));
                vo.setFollowed(ifFollow != null);
                vo.setCreateTime(follow.getCreateTime());
                return vo;
            }).toList();
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), voList);
    }

    @Override
    public PageResult<FollowUserVO> listFollower(ListFollowerDTO dto) {
        Long targetUserId = dto.getUserId() != null ? dto.getUserId() : UserContext.getUserId();
        Page<Follow> page = new Page<>(dto.getPage(), dto.getSize());
        Page<Follow> result = followMapper.selectPage(page,
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFolloweeId, targetUserId)
                        .orderByDesc(Follow::getCreateTime));
        //组装返回值列表返回
        List<FollowUserVO> voList = result.getRecords().stream().map(follow -> {
                FollowUserVO vo = new FollowUserVO();
                vo.setUserId(follow.getFollowerId());
                vo.setNickname(userService.getNickname(follow.getFollowerId()));
                vo.setAvatar(userService.getAvatar(follow.getFollowerId()));
                //这个后期得处理
                vo.setBio(null);
                //判断自己是否回关
                Follow ifFollow = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId,UserContext.getUserId() )
                        .eq(Follow::getFolloweeId, follow.getFollowerId() ));
                vo.setFollowed(ifFollow != null);
                vo.setCreateTime(follow.getCreateTime());
                return vo;
        }).toList();
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), voList);
    }


    @Override
    public Set<Long> batchFollowedUserIds(Long userId, List<Long> targetUserIds) {
        //批量查询：当前用户关注了哪些目标用户（走 Redis）
        String membersKey = followerMembersKey(userId);
        String countKey = followerCountKey(userId);
        bitmapCounter.ensureInit(membersKey, countKey,
                () -> followMapper.selectList(
                        new LambdaQueryWrapper<Follow>()
                                .eq(Follow::getFollowerId, userId))
                        .stream().map(Follow::getFolloweeId).toList());
        return targetUserIds.stream()
                .filter(uid -> bitmapCounter.contains(membersKey, uid))
                .collect(Collectors.toSet());
    }

    private String followerMembersKey(Long userId) {
        return String.format(REDIS_FOLLOWER_MEMBERS, userId);
    }

    private String followerCountKey(Long userId) {
        return String.format(REDIS_FOLLOWER_COUNT, userId);
    }

    private String followeeMembersKey(Long userId) {
        return String.format(REDIS_FOLLOWEE_MEMBERS, userId);
    }

    private String followeeCountKey(Long userId) {
        return String.format(REDIS_FOLLOWEE_COUNT, userId);
    }

    @Override
    public boolean isMutualFollow(Long userA, Long userB) {
        // 确保Redis有数据（冷启动兜底）
        String keyA = followerMembersKey(userA);
        String countKeyA = followerCountKey(userA);
        String keyB = followerMembersKey(userB);
        String countKeyB = followerCountKey(userB);
        bitmapCounter.ensureInit(keyA, countKeyA,
                () -> followMapper.selectList(
                        new LambdaQueryWrapper<Follow>()
                                .eq(Follow::getFollowerId, userA))
                        .stream().map(Follow::getFolloweeId).toList());
        bitmapCounter.ensureInit(keyB, countKeyB,
                () -> followMapper.selectList(
                        new LambdaQueryWrapper<Follow>()
                                .eq(Follow::getFollowerId, userB))
                        .stream().map(Follow::getFolloweeId).toList());
        return bitmapCounter.contains(keyA, userB)
                && bitmapCounter.contains(keyB, userA);
    }
}