package org.example.community.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;//主键
    private Long userId;//用户id
    private String type;//通知类型
    private Long triggerUserId;//触发用户id
    private Long targetId;//目标id
    private Boolean isRead;//是否已读
    private LocalDateTime createTime;//创建时间
}
