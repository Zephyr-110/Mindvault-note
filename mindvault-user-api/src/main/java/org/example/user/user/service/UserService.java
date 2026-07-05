package org.example.user.user.service;

import org.example.user.user.dto.*;
import org.example.user.user.vo.UserVO;

public interface UserService {

    UserVO register(RegisterDTO registerDTO);
    UserVO login(LoginDTO loginDTO);
    void changePassword(ChangePasswordDTO dto);
    void changeUsername(ChangeUsernameDTO dto);
    void logout(LogoutDTO dto);
    String getNickname(Long userId);
    String getAvatar(Long userId);
    boolean existsById(Long userId);
}