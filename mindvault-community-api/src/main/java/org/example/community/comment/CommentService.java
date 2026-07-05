package org.example.community.comment;

import org.example.community.comment.dto.CreateCommentDTO;
import org.example.community.comment.dto.DeleteCommentDTO;
import org.example.community.comment.dto.ListCommentDTO;
import org.example.community.comment.vo.CommentVO;
import org.example.common.result.PageResult;

public interface CommentService {

    CommentVO createComment(CreateCommentDTO createCommentDTO);

    void deleteComment(DeleteCommentDTO deleteCommentDTO);

    PageResult<CommentVO> listComments(ListCommentDTO listCommentDTO);
}
