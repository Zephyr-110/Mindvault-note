package org.example.community.follow.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowVO {

    private Boolean followed;//是否关注
    private Long followerCount;//粉丝数量
    private Long followingCount;//关注数量
}
