package org.example.community.send.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationVO {

    private Long userId;//对方用户id
    private String nickname;//对方昵称
    private String avatar;//对方头像
    private String lastMessage;//最新消息
    private LocalDateTime lastTime;//最新消息时间
    private Integer unreadCount;
    private Boolean isLastMessageFromMe;//最新消息是否来自自己
}
