package org.example.community.notification.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.community.notification.dto.ListNotificationDTO;
import org.example.community.notification.NotificationService;
import org.example.community.notification.dto.ReadNotificationDTO;
import org.example.community.notification.vo.NotificationVO;
import org.example.community.notification.vo.UnreadCountVO;
import org.example.common.result.PageResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<PageResult<NotificationVO>> list(@Valid ListNotificationDTO listNotificationDTO) {
        return Result.success(notificationService.list(listNotificationDTO));
    }

    @GetMapping("/read")
    public Result<?> read(@Valid ReadNotificationDTO readNotificationDTO) {
        notificationService.read(readNotificationDTO);
        return Result.success();
    }

    @GetMapping("/unreadCount")
    public Result<UnreadCountVO> unreadCount() {
        return Result.success(notificationService.unreadCount());
    }
}