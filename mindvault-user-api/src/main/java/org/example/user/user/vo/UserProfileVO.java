package org.example.user.user.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileVO {
    private String nickname;//昵称
    private String avatar;//头像
    private String phone;//手机号
    private String email;//邮箱
    private Long gender;//性别
    private Long age;//年龄
    private String region;//区域
    private String bio;//个性签名
}