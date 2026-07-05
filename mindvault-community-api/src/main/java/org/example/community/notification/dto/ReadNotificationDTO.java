package org.example.community.notification.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadNotificationDTO {
    @NotNull(message = "通知id不能为空")
    private Long notificationId;
}
