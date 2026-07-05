package org.example.user.block.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBlockVO {

    private Long userId;// 被屏蔽的用户id
    private String nickname;
    private String avatar;
}
