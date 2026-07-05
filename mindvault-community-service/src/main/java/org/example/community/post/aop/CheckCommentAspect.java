package org.example.community.post.aop;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.common.ValidatedEntityHolder;
import org.example.common.annotation.CheckComment;
import org.example.common.exception.BusinessException;
import org.example.common.util.AspectUtil;
import org.example.community.comment.entity.Comment;
import org.example.community.comment.mapper.CommentMapper;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Aspect
@RequiredArgsConstructor
public class CheckCommentAspect {

    private final CommentMapper commentMapper;

    @Around("@annotation(checkComment)")
    public Object checkComment(ProceedingJoinPoint joinPoint, CheckComment checkComment) throws Throwable {
        //拿到切点参数
        Object dto = AspectUtil.extractDTO(joinPoint);
        //拿到评论ID字段名
        String idFieldName = checkComment.value();
        //获取评论ID
        Long commentId = AspectUtil.getResourceId(dto, idFieldName);
        //拿到评论实体
        Comment comment = commentMapper.selectById(commentId);
        //校验
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        //校验通过，将帖子实体设置到ThreadLocal中
        try {
            ValidatedEntityHolder.set(comment);
            return joinPoint.proceed();
        } finally {
            ValidatedEntityHolder.clear();
        }
    }
}
