package org.example.user.block.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.common.result.PageResult;
import org.example.user.block.dto.BlockUserDTO;
import org.example.user.block.dto.IsBlockedDTO;
import org.example.user.block.dto.ListUserBlocksDTO;
import org.example.user.block.entity.UserBlock;
import org.example.user.block.mapper.UserBlockMapper;
import org.example.user.block.service.UserBlockService;
import org.example.user.block.vo.UserBlockVO;
import org.example.user.user.entity.UserProfile;
import org.example.user.user.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {

    private final UserBlockMapper userBlockMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public void blockUser(BlockUserDTO dto) {
        log.info("用户{}拉黑了用户{}", UserContext.getUserId(), dto.getUserId());
        UserBlock block = userBlockMapper.selectOne(
                new LambdaQueryWrapper<UserBlock>()
                        .eq(UserBlock::getUserId, UserContext.getUserId())
                        .eq(UserBlock::getBlockedUserId, dto.getUserId())
        );
        if(block != null){
            throw new BusinessException(400, "用户已拉黑");
        }
        block = new UserBlock();
        block.setUserId(UserContext.getUserId());
        block.setBlockedUserId(dto.getUserId());
        block.setCreateTime(LocalDateTime.now());
        userBlockMapper.insert(block);
    }

    @Override
    public void unblockUser(BlockUserDTO dto) {
        log.info("用户{}取消拉黑用户{}", UserContext.getUserId(), dto.getUserId());
        userBlockMapper.delete(
                new LambdaQueryWrapper<UserBlock>()
                        .eq(UserBlock::getUserId, UserContext.getUserId())
                        .eq(UserBlock::getBlockedUserId, dto.getUserId()));
    }

    @Override
    public PageResult<UserBlockVO> listBlockedUsers(ListUserBlocksDTO dto) {
        Page<UserBlock> page = new Page<>(dto.getPage(), dto.getSize());
        Page<UserBlock> result = userBlockMapper.selectPage(page,
                new LambdaQueryWrapper<UserBlock>()
                        .eq(UserBlock::getUserId, UserContext.getUserId())
        );
        Page<UserProfile> userPage = new Page<>(dto.getPage(), dto.getSize());
        Page<UserProfile> userResult = userProfileMapper.selectPage(userPage,
                new LambdaQueryWrapper<UserProfile>()
                        .in(UserProfile::getId, result.getRecords().stream().map(UserBlock::getBlockedUserId).toList())
        );
        List<UserBlockVO> list = userResult.getRecords().stream().map(userProfile -> {
            UserBlockVO vo = new UserBlockVO();
            vo.setUserId(userProfile.getId());
            vo.setNickname(userProfile.getNickname());
            vo.setAvatar(userProfile.getAvatar());
            return vo;
        }).toList();
        Long total = result.getTotal();
        return new PageResult<>(total, dto.getPage(), dto.getSize(), list);
    }

    @Override
    public Boolean isBlocked(IsBlockedDTO dto) {
        return userBlockMapper.exists(
                new LambdaQueryWrapper<UserBlock>()
                        .eq(UserBlock::getUserId, UserContext.getUserId())
                        .eq(UserBlock::getBlockedUserId, dto.getUserId()));
    }
}
