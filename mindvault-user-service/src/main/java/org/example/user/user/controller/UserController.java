package org.example.user.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.common.annotation.RateLimit;
import org.example.user.user.dto.*;
import org.example.user.user.service.UserService;
import org.example.user.user.vo.UserVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;

    @RateLimit(key = "register", limit = 3, window = 300)
    @PostMapping("/user/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return Result.success(userService.register(registerDTO));
    }

    @RateLimit(key = "login")
    @PostMapping("/user/login")
    public Result<UserVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }

    @RateLimit(key = "changePassword", limit = 3, window = 300)
    @PutMapping("/user/changePassword")
    public Result<?> changePassword(@Valid ChangePasswordDTO changePasswordDTO) {
        userService.changePassword(changePasswordDTO);
        return Result.success();
    }

    @RateLimit(key = "changeUsername", limit = 3, window = 300)
    @PutMapping("/user/changeUsername")
    public Result<?> changeUsername(@Valid ChangeUsernameDTO changeUsernameDTO) {
        userService.changeUsername(changeUsernameDTO);
        return Result.success();
    }

    @PostMapping("/user/logout")
    public Result<?> logout(@Valid @RequestBody LogoutDTO logoutDTO) {
        userService.logout(logoutDTO);
        return Result.success();
    }
}