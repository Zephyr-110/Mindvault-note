package org.example.community.follow.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListFolloweeDTO {//关注列表

    private Long userId;//要查看的用户id，为空则取当前用户
    private Long page = 1L;//页码
    private Long size = 10L;//每页用户条数
}