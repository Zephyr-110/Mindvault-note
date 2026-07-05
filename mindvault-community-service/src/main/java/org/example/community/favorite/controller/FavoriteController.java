package org.example.community.favorite.controller;


import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.community.favorite.dto.FavoriteCountDTO;
import org.example.community.favorite.dto.ListFavoriteDTO;
import org.example.community.favorite.FavoriteService;
import org.example.community.favorite.dto.ToggleFavoriteDTO;
import org.example.community.favorite.vo.FavoriteCountVO;
import org.example.community.favorite.vo.FavoriteVO;
import org.example.common.result.PageResult;
import org.example.community.post.vo.PostVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/toggle")
    public Result<FavoriteVO> toggleFavorite(@RequestBody ToggleFavoriteDTO toggleFavorite) {
        return Result.success(favoriteService.toggleFavorite(toggleFavorite));
    }

    @GetMapping("/count")
    public Result<FavoriteCountVO> countFavorites(FavoriteCountDTO favoriteCountDTO) {
        return Result.success(favoriteService.countFavorites(favoriteCountDTO));
    }

    @GetMapping("/list")
    public Result<PageResult<PostVO>> listFavorites(ListFavoriteDTO listFavoriteDTO) {
        return Result.success(favoriteService.listFavorites(listFavoriteDTO));
    }
}