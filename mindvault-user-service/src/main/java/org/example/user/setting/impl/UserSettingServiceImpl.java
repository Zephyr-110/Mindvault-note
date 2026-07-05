package org.example.user.setting.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.user.setting.dto.ListUserSettingDTO;
import org.example.user.setting.dto.UpdateUserSettingDTO;
import org.example.user.setting.entity.UserSetting;
import org.example.user.setting.mapper.UserSettingMapper;
import org.example.user.setting.service.UserSettingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingServiceImpl implements UserSettingService {

    private final UserSettingMapper userSettingMapper;

    // ========== 白名单：每个 key 允许哪些值 ==========
    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry("appearance.theme",             Set.of("default", "warm", "dark")),
            Map.entry("appearance.fontSize",          Set.of("small", "medium", "large")),
            Map.entry("editor.autoSave",              Set.of("true", "false")),
            Map.entry("editor.defaultView",           Set.of("normal", "2d", "3d")),
            Map.entry("notification.like",            Set.of("true", "false")),
            Map.entry("notification.comment",         Set.of("true", "false")),
            Map.entry("notification.reply",           Set.of("true", "false")),
            Map.entry("notification.follow",          Set.of("true", "false")),
            Map.entry("notification.favorite",        Set.of("true", "false")),
            Map.entry("privacy.profileVisibility",    Set.of("public", "friendsOnly", "private")),
            Map.entry("privacy.showEmail",            Set.of("true", "false")),
            Map.entry("privacy.allowStrangerChat",    Set.of("true", "false"))
    );

    // ========== 默认值：key 不存在时返回什么 ==========
    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("appearance.theme",             "default"),
            Map.entry("appearance.fontSize",          "medium"),
            Map.entry("editor.autoSave",              "true"),
            Map.entry("editor.defaultView",           "normal"),
            Map.entry("notification.like",            "true"),
            Map.entry("notification.comment",         "true"),
            Map.entry("notification.follow",          "true"),
            Map.entry("notification.favorite",        "true"),
            Map.entry("privacy.profileVisibility",    "public"),
            Map.entry("privacy.showEmail",            "true"),
            Map.entry("privacy.allowStrangerChat",    "true")
    );

    @Override
    public void updateSetting(UpdateUserSettingDTO dto) {
        String key = dto.getSettingKey();
        String value = dto.getSettingValue();
        // 1. key 是否在白名单内
        Set<String> allowedValues = ALLOWED.get(key);
        if (allowedValues == null) {
            throw new BusinessException(400, "未知的设置项：" + key);
        }
        // 2. value 是否合法
        if (!allowedValues.contains(value)) {
            throw new BusinessException(400, "非法的设置值：" + value);
        }
        // 3. upsert 入库
        upsert(key, value);
    }

    @Override
    public Map<String, String> getSettingsByCategory(ListUserSettingDTO dto) {
        log.info("用户{}查询了设置项{}", UserContext.getUserId(), dto.getCategory());
        // 拿到当前登录用户id
        Long userId = UserContext.getUserId();
        // 拿到该用户所有该目录的设置
        List<UserSetting> list = userSettingMapper.selectList(
                new LambdaQueryWrapper<UserSetting>()
                        .eq(UserSetting::getUserId, userId)
                        .likeRight(UserSetting::getSettingKey, dto.getCategory() + ".")  // 匹配 category.*
        );
        // 已存的值
        Map<String, String> result = new HashMap<>();
        for (UserSetting s : list) {
            result.put(s.getSettingKey(), s.getSettingValue());
        }
        // 补上默认值（数据库中还没有的 key）
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (entry.getKey().startsWith(dto.getCategory() + ".") && !result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    //
    private void upsert(String key, String value) {
        log.info("用户{}更改了设置{}", UserContext.getUserId(), key);
        //拿到当前登录用户id
        Long userId = UserContext.getUserId();
        //拿到该项设置实体类
        UserSetting exist = userSettingMapper.selectOne(
                new LambdaQueryWrapper<UserSetting>()
                        .eq(UserSetting::getUserId, userId)
                        .eq(UserSetting::getSettingKey, key)
        );
        //如果不存在就新建
        if (exist == null) {
            UserSetting setting = new UserSetting();
            setting.setUserId(userId);
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setUpdateTime(LocalDateTime.now());
            userSettingMapper.insert(setting);
        } else {//如果存在就更新
            exist.setSettingValue(value);
            exist.setUpdateTime(LocalDateTime.now());
            userSettingMapper.updateById(exist);
        }
    }

    @Override
    public UpdateUserSettingDTO getSetting(String key, Long userId) {
        UserSetting setting = userSettingMapper.selectOne(
                new LambdaQueryWrapper<UserSetting>()
                        .eq(UserSetting::getUserId, userId)
                        .eq(UserSetting::getSettingKey, key)
        );
        if (setting == null) {
            return new UpdateUserSettingDTO(key, DEFAULTS.get(key));
        }
        return new UpdateUserSettingDTO(setting.getSettingKey(), setting.getSettingValue());
    }
}