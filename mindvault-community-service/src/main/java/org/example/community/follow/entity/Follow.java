package org.example.community.follow.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Follow {
    @TableId(type = IdType.AUTO)
    private Long id;//主键 ID
    private Long followerId;//粉丝 ID
    private Long followeeId;//被关注人 ID
    private LocalDateTime createTime;//创建时间
}
