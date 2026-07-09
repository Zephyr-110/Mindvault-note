package org.example.user.user.service;

import org.example.common.result.PageResult;
import org.example.user.user.dto.SearchUserDTO;
import org.example.user.user.dto.UpdateUserProfileDTO;
import org.example.user.user.vo.UserProfileVO;
import org.example.user.user.vo.UserSearchVO;

import java.util.List;

public interface UserProfileService {

    UserProfileVO getUserProfile();

    UserProfileVO getUserProfileById(Long userId);

    void updateUserProfile(UpdateUserProfileDTO dto);

    PageResult<UserSearchVO> searchUserProfile(SearchUserDTO searchUserDTO);
}