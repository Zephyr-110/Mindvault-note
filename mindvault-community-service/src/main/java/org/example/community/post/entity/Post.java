package org.example.community.post.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;//主键ID
    private String title;//标题
    private String content;//内容
    private Long authorId;//作者ID
    private Long visibility;//可见性
    private Long originalPostId;//原始帖子ID
    private String noteAccessory;//笔记附件JSON
    private LocalDateTime createTime;//创建时间
}