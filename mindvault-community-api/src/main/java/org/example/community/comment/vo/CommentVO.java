package org.example.community.comment.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentVO {
    private Long id;//评论id
    private Long postId;//帖子id
    private Long authorId;//评论作者id
    private String authorNickname;//评论作者昵称
    private String authorAvatar;//评论作者头像
    private Long parentId;//父评论id，如果为null则为根评论
    private String parentNickname;//父评论作者昵称
    private String content;//评论内容
    private LocalDateTime createTime;//评论时间
}
