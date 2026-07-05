package org.example.community.follow.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToggleFollowDTO {

    @NotNull(message = "关注对象不能为空")
    private Long followeeId;//要关注的人的用户id
}
