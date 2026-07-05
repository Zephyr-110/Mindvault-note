package org.example.community.likerecord.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;//主键ID
    private String targetType;//目标类型
    private Long targetId;
    private Long userId;
    private LocalDateTime createTime;//创建时间
}
