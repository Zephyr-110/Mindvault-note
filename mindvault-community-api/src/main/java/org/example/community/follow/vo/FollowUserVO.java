package org.example.community.follow.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowUserVO {

    private Long userId;//用户id
    private String nickname;//昵称
    private String avatar;//头像
    private String bio;//个人签名
    private Boolean followed;//对方是否关注我
    private LocalDateTime createTime;//关注时间
}
