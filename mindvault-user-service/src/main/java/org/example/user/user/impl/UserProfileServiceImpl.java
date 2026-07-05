package org.example.user.user.impl;

import lombok.RequiredArgsConstructor;
import org.example.common.logincheck.UserContext;
import org.example.community.follow.FollowService;
import org.example.user.setting.service.UserSettingService;
import org.example.user.user.dto.UpdateUserProfileDTO;
import org.example.user.user.entity.UserProfile;
import org.example.user.user.mapper.UserMapper;
import org.example.user.user.mapper.UserProfileMapper;
import org.example.user.user.service.UserProfileService;
import org.example.user.user.vo.UserProfileVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final UserSettingService userSettingService;
    private final FollowService followService;

    @Override
    public UserProfileVO getUserProfile() {
        UserProfile userProfile = userProfileMapper.selectByUserId(UserContext.getUserId());
        if (userProfile == null) {
            return null;
        }
        UserProfileVO userProfileVO = new UserProfileVO();
        userProfileVO.setNickname(userProfile.getNickname());
        userProfileVO.setAvatar(userProfile.getAvatar());
        userProfileVO.setPhone(userProfile.getPhone());
        userProfileVO.setEmail(userProfile.getEmail());
        userProfileVO.setGender(userProfile.getGender());
        userProfileVO.setAge(userProfile.getAge());
        userProfileVO.setRegion(userProfile.getRegion());
        userProfileVO.setBio(userProfile.getBio());
        return userProfileVO;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "nickname", key = "T(org.example.common.logincheck.UserContext).getUserId()"),
            @CacheEvict(value = "avatar", key = "T(org.example.common.logincheck.UserContext).getUserId()")
    })
    public UserProfileVO getUserProfileById(Long userId) {
        UserProfile userProfile = userProfileMapper.selectByUserId(userId);
        if (userProfile == null) {
            return null;
        }
        // 判断用户是否开启隐私设置
        String settingKeyProfile = "privacy.profileVisibility";
        if (userSettingService.getSetting(settingKeyProfile, userId).getSettingValue().equals("private")) {
            return null;
        } else if (userSettingService.getSetting(settingKeyProfile, userId).getSettingValue().equals("friendsOnly")) {
            if(!UserContext.getUserId().equals(userId)
            && !followService.isMutualFollow(UserContext.getUserId(), userId)){
                return null;
            }
        }
        UserProfileVO vo = new UserProfileVO();
        String settingKeyEmail = "privacy.showEmail";
        // 判断用户是否开启显示邮箱
        if (userSettingService.getSetting(settingKeyEmail, userId).getSettingValue().equals("false")) {
            vo.setEmail(null);
        }
        vo.setNickname(userProfile.getNickname());
        vo.setAvatar(userProfile.getAvatar());
        vo.setGender(userProfile.getGender());
        vo.setAge(userProfile.getAge());
        vo.setRegion(userProfile.getRegion());
        vo.setBio(userProfile.getBio());
        return vo;
    }

    @Override
    public void updateUserProfile(UpdateUserProfileDTO dto) {
        userProfileMapper.updateUserProfile(UserContext.getUserId(), dto);
    }
}