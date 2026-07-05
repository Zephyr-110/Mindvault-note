package org.example.community.likerecord.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeCountVO {

    private String targetType;//点赞目标类型
    private Long targetId;//点赞目标ID
    private Long likeCount;//点赞数
    private Boolean liked;//红心还是空心
}
