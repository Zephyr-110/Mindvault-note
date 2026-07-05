package org.example.user.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {

    private Long id;//用户id
    private String username;//用户名
    private LocalDateTime createTime;//创建时间
    private String token;//JWT token
    //没有密码字段是因为用户密码是加密存储的，不能明文返回前端
    private String nickname;//昵称
    private String avatar;//头像
}