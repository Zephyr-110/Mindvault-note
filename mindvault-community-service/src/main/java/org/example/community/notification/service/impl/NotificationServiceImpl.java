package org.example.community.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.user.setting.dto.ListUserSettingDTO;
import org.example.user.setting.service.UserSettingService;
import org.example.user.user.service.UserService;
import org.example.community.notification.dto.ListNotificationDTO;
import org.example.community.notification.dto.ReadNotificationDTO;
import org.example.community.notification.entity.Notification;
import org.example.community.notification.NotificationService;
import org.example.community.notification.mapper.NotificationMapper;
import org.example.community.notification.vo.NotificationVO;
import org.example.community.notification.vo.UnreadCountVO;
import org.example.common.result.PageResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserService userService;
    private final UserSettingService userSettingService;

    @Override
    public PageResult<NotificationVO> list(ListNotificationDTO listNotificationDTO) {
        //获取分页信息
        Page<Notification> page = new Page<>(listNotificationDTO.getPage(), listNotificationDTO.getSize());
        //拿到这一页数据
        Page<Notification> result = notificationMapper.selectPage(page, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, UserContext.getUserId())
                .orderByDesc(Notification::getCreateTime));
        List<NotificationVO> notificationVOList = new ArrayList<>();
        //遍历这一页信息组装VO
        for (Notification notification : result.getRecords()) {
            NotificationVO notificationVO = new NotificationVO();
            notificationVO.setId(notification.getId());
            notificationVO.setType(notification.getType());
            notificationVO.setTriggerUserId(notification.getTriggerUserId());
            notificationVO.setTriggerNickname(userService.getNickname(notification.getTriggerUserId()));
            notificationVO.setTriggerAvatar(userService.getAvatar(notification.getTriggerUserId()));
            notificationVO.setTargetId(notification.getTargetId());
            notificationVO.setContent(getContent(notification.getType()));
            notificationVO.setIsRead(notification.getIsRead());
            notificationVO.setCreateTime(notification.getCreateTime().toString());
            notificationVOList.add(notificationVO);
        }
        //返回分页数据
        return new PageResult<>(result.getTotal(), listNotificationDTO.getPage(), listNotificationDTO.getSize(), notificationVOList);
    }

    private String getContent(String type) {
        return switch (type) {
            case "follow" -> "关注了你";
            case "like" -> "点赞了你的内容";
            case "comment" -> "评论了你的帖子";
            case "reply" -> "回复了你的评论";
            case "forward" -> "转发了你的帖子";
            default -> throw new BusinessException(401, "未知的通知类型: " + type);
        };
    }

    @Override
    public void read(ReadNotificationDTO readNotificationDTO) {
        //只要id不合法那就是用户直接清空通知信息了。
        if(readNotificationDTO.getNotificationId() == null
        || readNotificationDTO.getNotificationId() <= 1){
            //指定类型，照这个改
            Notification updateEntity = new Notification();
            updateEntity.setIsRead(true);
            notificationMapper.update(updateEntity,
                    new LambdaQueryWrapper<Notification>()
                            .eq(Notification::getUserId, UserContext.getUserId())
                            .eq(Notification::getIsRead, false)
            );
            log.info("用户 {} 清空了所有未读通知", UserContext.getUserId());
        }
        //如果id合法，那就是用户点进去一条通知详情了，只更新一条为已读
        else {
            Notification updateEntity = notificationMapper.selectById(readNotificationDTO.getNotificationId());
            if(updateEntity == null){
                throw new BusinessException(401, "通知不存在");
            }
            updateEntity.setIsRead(true);
            notificationMapper.updateById(updateEntity);
            log.info("用户 {} 阅读了通知 {}", UserContext.getUserId(), updateEntity.getId());
        }
    }

    @Override
    public UnreadCountVO unreadCount() {
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, UserContext.getUserId())
                .eq(Notification::getIsRead, false));
        return new UnreadCountVO(count);
    }

    @Override
    public void createNotification(Long userId, String type, Long triggerUserId, Long targetId) {
        if (triggerUserId.equals(userId)) {
            return;
        }
        //判断用户是否关闭了该类型的通知
        String settingKey = "notification." + type;
        if (userSettingService.getSetting(settingKey, userId).getSettingValue().equals("false")) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTriggerUserId(triggerUserId);
        notification.setTargetId(targetId);
        notification.setIsRead(false);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        log.info("通知：用户 {} 触发了类型 {} 的通知给用户 {}", triggerUserId, type, userId);
    }
}