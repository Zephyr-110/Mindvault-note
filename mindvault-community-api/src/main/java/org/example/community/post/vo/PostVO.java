package org.example.community.post.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostVO {

    private Long id;
    private String title;
    private String content;//列表时可截断，详情时完整
    private Long authorId;
    private String authorNickname;//从笔记项目拿
    private String authorAvatar;//从笔记项目拿
    private Long visibility;//可见性 0:全部可见 1:仅可见关注者
    private Long originalPostId;
    private String originalPostTitle;//转发时有值，从原帖查
    private Long likeCount;//点赞数
    private Long commentCount;//评论数
    private Long favoriteCount;//收藏数
    private Boolean isLiked;//是否点赞
    private Boolean isFavorited;//是否收藏
    private Boolean isFollowed;//是否关注
    private LocalDateTime createTime;
    private List<ListDocumentAccessoryVO> documentAccessories;//笔记附件列表
}
