package org.example.user.setting.service;

import org.example.user.setting.dto.ListUserSettingDTO;
import org.example.user.setting.dto.UpdateUserSettingDTO;


import java.util.Map;

public interface UserSettingService {

    public void updateSetting(UpdateUserSettingDTO dto);
    Map<String, String> getSettingsByCategory(ListUserSettingDTO dto);
    UpdateUserSettingDTO getSetting(String key, Long userId);
}
