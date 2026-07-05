package org.example.community.favorite.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteCountVO {

    private Long postId;
    private Long favoriteCount;
}
