package org.example.community.favorite.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long id;//收藏id
    private Long postId;//帖子id
    private Long userId;//用户id
    private LocalDateTime createTime;//收藏时间
}
