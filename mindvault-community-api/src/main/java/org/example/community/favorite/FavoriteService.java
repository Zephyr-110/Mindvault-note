package org.example.community.favorite;

import org.example.community.favorite.dto.FavoriteCountDTO;
import org.example.community.favorite.dto.ListFavoriteDTO;
import org.example.community.favorite.dto.ToggleFavoriteDTO;
import org.example.community.favorite.vo.FavoriteCountVO;
import org.example.community.favorite.vo.FavoriteVO;
import org.example.common.result.PageResult;
import org.example.community.post.vo.PostVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FavoriteService {
    FavoriteVO toggleFavorite(ToggleFavoriteDTO toggleFavorite);

    FavoriteCountVO countFavorites(FavoriteCountDTO favoriteCountDTO);

    PageResult<PostVO> listFavorites(ListFavoriteDTO listFavoriteDTO);

    /**
     * 批量查询：当前用户收藏了哪些帖子（走 Redis）
     */
    Set<Long> batchFavPostIds(Long userId, List<Long> postIds);

    /**
     * 批量查询：所有帖子的收藏数（走 Redis）
     */
    Map<Long, Long> batchFavCounts(List<Long> postIds);
}