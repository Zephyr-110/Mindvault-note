package org.example.community.likerecord.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.community.likerecord.LikeRecordService;
import org.example.community.likerecord.dto.LikeCountDTO;
import org.example.community.likerecord.dto.ToggleLikeDTO;
import org.example.community.likerecord.vo.LikeCountVO;
import org.example.community.likerecord.vo.LikeVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/community/like-record")
public class LikeRecordController {

    private final LikeRecordService likeRecordService;

    @PostMapping("/toggle")
    public Result<LikeVO> toggleLike(@RequestBody ToggleLikeDTO toggleLikeDTO) {
        return Result.success(likeRecordService.toggleLike(toggleLikeDTO));
    }

    @GetMapping("/count")
    public Result<LikeCountVO> getLikeCount(LikeCountDTO likeCountDTO) {
        return Result.success(likeRecordService.getLikeCount(likeCountDTO));
    }
}