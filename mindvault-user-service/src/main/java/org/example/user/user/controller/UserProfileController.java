package org.example.user.user.controller;


import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.user.user.dto.UpdateUserProfileDTO;
import org.example.user.user.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;
    @GetMapping("/get")
    public Result<?> getUserProfile() {
        return Result.success(userProfileService.getUserProfile());
    }

    @GetMapping("/getById")
    public Result<?> getUserProfileById(@RequestParam Long userId) {
        return Result.success(userProfileService.getUserProfileById(userId));
    }

    @PutMapping("/update")
    public Result<?> updateUserProfile(@RequestBody UpdateUserProfileDTO dto) {
        userProfileService.updateUserProfile(dto);
        return Result.success();
    }
}