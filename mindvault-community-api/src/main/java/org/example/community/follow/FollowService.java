package org.example.community.follow;

import org.example.community.follow.dto.ListFolloweeDTO;
import org.example.community.follow.dto.ListFollowerDTO;
import org.example.community.follow.dto.ToggleFollowDTO;
import org.example.community.follow.vo.FollowUserVO;
import org.example.community.follow.vo.FollowVO;
import org.example.common.result.PageResult;

import java.util.List;
import java.util.Set;

public interface FollowService {
    FollowVO toggleFollow(ToggleFollowDTO toggleFollowDTO);

    PageResult<FollowUserVO> listFollowee(ListFolloweeDTO listFolloweeDTO);

    PageResult<FollowUserVO> listFollower(ListFollowerDTO listFollowerDTO);

    /**
     * 批量查询：当前用户关注了哪些目标用户（走 Redis）
     */
    Set<Long> batchFollowedUserIds(Long userId, List<Long> targetUserIds);

    boolean isMutualFollow(Long userA, Long userB);
}