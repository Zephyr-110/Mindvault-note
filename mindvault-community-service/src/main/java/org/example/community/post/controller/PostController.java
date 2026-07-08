package org.example.community.post.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.community.post.PostService;
import org.example.community.post.dto.CreatePostDTO;
import org.example.community.post.dto.DeletePostDTO;
import org.example.community.post.dto.FeedDTO;
import org.example.community.post.dto.PostDetailDTO;
import org.example.common.result.PageResult;
import org.example.community.post.vo.PostVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/post")
@Validated
public class PostController {

    private final PostService postService;

    @PostMapping("/create")
    public Result<PostVO> createPost(@Valid @RequestBody CreatePostDTO createPostDTO) {
        return Result.success(postService.createPost(createPostDTO));
    }

    @GetMapping("/detail")
    public Result<PostVO> detail(@Valid PostDetailDTO dto) {
        return Result.success(postService.getPostDetail(dto));
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@Valid DeletePostDTO dto) {
        postService.deletePost(dto);
        return Result.success("删除成功");
    }

    @GetMapping("/feed")
    public Result<PageResult<PostVO>> feed(@Valid FeedDTO dto) {
        return Result.success(postService.getFeed(dto));
    }
}