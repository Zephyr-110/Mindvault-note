package org.example.community.notification.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListNotificationDTO {

    private Long page = 1L;
    private Long size = 10L;
}
