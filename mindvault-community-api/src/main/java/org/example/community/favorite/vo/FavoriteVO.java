package org.example.community.favorite.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteVO {

    private Boolean isFavorited;//true表示已收藏，false表示未收藏
    private Long favoriteCount;//该帖收藏总数
}
