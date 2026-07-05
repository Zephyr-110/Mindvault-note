package org.example.user.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_profile")
public class UserProfile {
    @TableId(type = IdType.AUTO)
    private Long id;//主键
    private Long userId;//用户ID
    private String nickname;//昵称
    private String avatar;//头像
    private String phone;//手机号
    private String email;//邮箱
    private Long gender;//性别
    private Long age;//年龄
    private String region;//区域
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//更新时间
    private String bio;//个性签名
}
