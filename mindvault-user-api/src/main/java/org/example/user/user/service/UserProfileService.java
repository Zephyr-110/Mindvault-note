package org.example.user.user.service;

import org.example.user.user.dto.UpdateUserProfileDTO;
import org.example.user.user.vo.UserProfileVO;

public interface UserProfileService {

    UserProfileVO getUserProfile();

    UserProfileVO getUserProfileById(Long userId);

    void updateUserProfile(UpdateUserProfileDTO dto);
}