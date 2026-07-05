package org.example.community.comment.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.community.comment.CommentService;
import org.example.community.comment.dto.CreateCommentDTO;
import org.example.community.comment.dto.DeleteCommentDTO;
import org.example.community.comment.dto.ListCommentDTO;
import org.example.community.comment.vo.CommentVO;
import org.example.common.result.Result;
import org.example.common.result.PageResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/comment")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/create")
    public Result<CommentVO> createComment(@Valid @RequestBody CreateCommentDTO createCommentDTO) {
        return Result.success(commentService.createComment(createCommentDTO));
    }

    @DeleteMapping("/delete")
    public Result<?> deleteComment(@Valid @RequestBody DeleteCommentDTO deleteCommentDTO) {
        commentService.deleteComment(deleteCommentDTO);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<PageResult<CommentVO>> listComments(@Valid ListCommentDTO listCommentDTO) {
        return Result.success(commentService.listComments(listCommentDTO));
    }

}