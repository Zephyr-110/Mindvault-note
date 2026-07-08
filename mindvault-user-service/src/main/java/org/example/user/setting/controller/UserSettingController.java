package org.example.user.setting.controller;


import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.user.setting.dto.ListUserSettingDTO;
import org.example.user.setting.dto.UpdateUserSettingDTO;
import org.example.user.setting.service.UserSettingService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user/setting")
@RequiredArgsConstructor
@Validated
public class UserSettingController {

    private final UserSettingService userSettingService;


    @PutMapping("/update")
    public Result<?> updateSetting(UpdateUserSettingDTO dto) {
        userSettingService.updateSetting(dto);
        return Result.success();
    }

    @GetMapping("/get")
    public Result<Map<String, String>> getSetting(ListUserSettingDTO dto) {
        return Result.success(userSettingService.getSettingsByCategory(dto));
    }
}