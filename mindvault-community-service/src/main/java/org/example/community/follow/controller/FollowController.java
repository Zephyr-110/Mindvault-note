package org.example.community.follow.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.community.follow.dto.ListFolloweeDTO;
import org.example.community.follow.dto.ListFollowerDTO;
import org.example.community.follow.FollowService;
import org.example.community.follow.dto.ToggleFollowDTO;
import org.example.community.follow.vo.FollowUserVO;
import org.example.community.follow.vo.FollowVO;
import org.example.common.result.PageResult;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/follow")
public class FollowController {

    private final FollowService followService;

    @PostMapping("/toggle")
    public Result<FollowVO> toggleFollow(@Valid @RequestBody ToggleFollowDTO toggleFollowDTO) {
        return Result.success(followService.toggleFollow(toggleFollowDTO));
    }

    @GetMapping("/listFollowee")
    public Result<PageResult<FollowUserVO>> listFollowee(@Valid ListFolloweeDTO listFolloweeDTO) {
        return Result.success(followService.listFollowee(listFolloweeDTO));
    }

    @GetMapping("/listFollower")
    public Result<PageResult<FollowUserVO>> listFollower(@Valid ListFollowerDTO listFollowerDTO) {
        return Result.success(followService.listFollower(listFollowerDTO));
    }

}