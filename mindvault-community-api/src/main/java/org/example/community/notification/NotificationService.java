package org.example.community.notification;

import jakarta.validation.Valid;
import org.example.community.notification.dto.ListNotificationDTO;
import org.example.community.notification.dto.ReadNotificationDTO;
import org.example.community.notification.vo.NotificationVO;
import org.example.community.notification.vo.UnreadCountVO;
import org.example.common.result.PageResult;

public interface NotificationService {
    PageResult<NotificationVO> list(@Valid ListNotificationDTO listNotificationDTO);

    void read(@Valid ReadNotificationDTO readNotificationDTO);

    UnreadCountVO unreadCount();

    void createNotification(Long userId, String type, Long triggerUserId, Long targetId);
}