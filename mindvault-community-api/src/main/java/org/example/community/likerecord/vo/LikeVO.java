package org.example.community.likerecord.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeVO {

    private Boolean isLiked;//true表示已点赞，false表示未点赞
    private Long likeCount;//点赞数量
}
