package org.example.community.send.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;//发送者id
    private Long receiverId;//接收者id
    private String content;// 内容
    private Boolean isRead;
    private LocalDateTime createTime;
}
