package org.example.user.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.PasswordUtil;
import org.example.user.setting.entity.UserSetting;
import org.example.user.setting.mapper.UserSettingMapper;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.JwtUtil;
import org.example.common.logincheck.UserContext;
import org.example.user.user.dto.*;
import org.example.user.user.entity.User;
import org.example.user.user.entity.UserProfile;
import org.example.user.user.mapper.UserMapper;
import org.example.user.user.mapper.UserProfileMapper;
import org.example.user.user.service.UserService;
import org.example.user.user.vo.UserVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final UserProfileMapper userProfileMapper;
    private final UserSettingMapper userSettingMapper;
    private final StringRedisTemplate redis;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO registerDTO) {
        if (userMapper.selectByUsername(registerDTO.getUsername()) != null)
            throw new BusinessException(400, "用户名已存在");
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        String encodedPassword = PasswordUtil.encode(registerDTO.getPassword());
        user.setPassword(encodedPassword);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        //创建用户个人信息
        UserProfile userProfile = new UserProfile();
        userProfile.setNickname(registerDTO.getUsername());
        userProfile.setUserId(user.getId());
        userProfile.setCreateTime(LocalDateTime.now());
        userProfile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.insert(userProfile);
        // 初始化默认设置
        initDefaultSettings(user.getId());
        log.info("用户 {} 注册成功", user.getUsername());
        return new UserVO(user.getId(), user.getUsername(), user.getCreateTime(), jwtUtil.generateToken(user.getId()), userProfile.getNickname(), userProfile.getAvatar());
    }

    @Override
    public UserVO login(LoginDTO loginDTO) {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null || !PasswordUtil.matches(loginDTO.getPassword(), user.getPassword()))
            throw new BusinessException(400, "用户名或密码输入错误！");
        UserProfile userProfile = userProfileMapper.selectByUserId(user.getId());
        //如果用户个人信息为空，补充用户个人信息
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUserId(user.getId());
            userProfile.setNickname(user.getUsername());
            userProfile.setCreateTime(LocalDateTime.now());
            userProfile.setUpdateTime(LocalDateTime.now());
            userProfileMapper.insert(userProfile);
            log.info("为用户 {} 补建了个人信息", user.getUsername());
        }
        log.info("用户 {} 登录成功", user.getUsername());
        return new UserVO(user.getId(), user.getUsername(), user.getCreateTime(), jwtUtil.generateToken(user.getId()), userProfile.getNickname(), userProfile.getAvatar());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null || !PasswordUtil.matches(dto.getOldPassword(), user.getPassword()))
            throw new BusinessException(400, "旧密码输入错误！");
        String encodedNewPassword = PasswordUtil.encode(dto.getNewPassword());
        user.setPassword(encodedNewPassword);
        userMapper.updateById(user);
        log.info("用户 {} 密码更新成功", user.getUsername());
        //把token加入黑名单中，并且设置好过期时间
        LogoutDTO logoutDTO = new LogoutDTO(dto.getToken());
        logout(logoutDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeUsername(ChangeUsernameDTO dto) {
        User user = userMapper.selectById(UserContext.getUserId());
        if (userMapper.selectByUsername(dto.getNewUsername()) != null)
            throw new BusinessException(400, "用户名已存在");
        user.setUsername(dto.getNewUsername());
        userMapper.updateById(user);
        log.info("用户 {} 更新用户名成功", user.getUsername());
    }

    @Override
    public void logout(LogoutDTO dto) {
        //取出token
        String token = dto.getToken();
        //把token加入黑名单中，并且设置好过期时间
        redis.opsForValue().set("blacklist:" + token, "1",
                Duration.ofMillis(jwtUtil.getRemainingExpiration(token)));
    }

    /**
     * 注册时初始化用户默认设置
     */
    private void initDefaultSettings(Long userId) {
        Map<String, String> defaults = Map.ofEntries(
                Map.entry("appearance.theme", "default"),
                Map.entry("appearance.fontSize", "medium"),
                Map.entry("editor.autoSave", "true"),
                Map.entry("editor.defaultView", "normal"),
                Map.entry("notification.like", "true"),
                Map.entry("notification.comment", "true"),
                Map.entry("notification.reply", "true"),
                Map.entry("notification.follow", "true"),
                Map.entry("notification.favorite", "true"),
                Map.entry("privacy.profileVisibility", "public"),
                Map.entry("privacy.showEmail", "true"),
                Map.entry("privacy.allowStrangerChat", "true")
        );
        LocalDateTime now = LocalDateTime.now();
        for (var entry : defaults.entrySet()) {
            UserSetting setting = new UserSetting();
            setting.setUserId(userId);
            setting.setSettingKey(entry.getKey());
            setting.setSettingValue(entry.getValue());
            setting.setUpdateTime(now);
            userSettingMapper.insert(setting);
        }
        log.info("用户 {} 默认设置初始化完成", userId);
    }

    @Override
    @Cacheable(value = "nickname", key = "#p0")
    public String getNickname(Long userId) {
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            return "未知用户";
        }
        return profile.getNickname();
    }

    @Override
    @Cacheable(value = "avatar", key = "#p0")
    public String getAvatar(Long userId) {
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            return "";
        }
        return profile.getAvatar();
    }

    @Override
    public boolean existsById(Long userId) {
        return userMapper.selectById(userId) != null;
    }
}