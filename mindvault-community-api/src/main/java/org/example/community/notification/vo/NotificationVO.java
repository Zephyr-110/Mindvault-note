package org.example.community.notification.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationVO {

    private Long id;//主键
    private String type;//通知类型
    private Long triggerUserId;//触发用户id
    private String triggerNickname;//触发用户昵称
    private String triggerAvatar;//触发用户头像
    private Long targetId;//关联的帖子或者评论 id，为了跳转
    private String content;//通知内容
    private Boolean isRead;//是否已读
    private String createTime;//创建时间
}
