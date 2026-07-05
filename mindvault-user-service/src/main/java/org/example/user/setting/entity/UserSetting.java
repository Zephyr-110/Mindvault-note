package org.example.user.setting.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSetting {
    @TableId(type = IdType.AUTO)
    private Long id;//主键
    private Long userId;//用户id
    private String settingKey;
    private String settingValue;
    private LocalDateTime updateTime;//更新时间
}
