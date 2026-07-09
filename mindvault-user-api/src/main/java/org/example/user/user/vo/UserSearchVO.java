package org.example.user.user.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchVO {

    private Long userId;
    private String nickname;
    private String avatar;
    private String bio;
    private Boolean isFollow;
}
