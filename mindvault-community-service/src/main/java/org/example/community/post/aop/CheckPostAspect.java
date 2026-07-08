package org.example.community.post.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.common.ValidatedEntityHolder;
import org.example.common.annotation.CheckPost;
import org.example.common.exception.BusinessException;
import org.example.common.util.AspectUtil;
import org.example.community.post.entity.Post;
import org.example.community.post.mapper.PostMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class CheckPostAspect {

    private final PostMapper postMapper;

    @Around("@annotation(checkPost)")
    public Object checkPost(ProceedingJoinPoint joinPoint, CheckPost checkPost) throws Throwable {
        //拿到切点参数
        Object dto = AspectUtil.extractDTO(joinPoint);
        //拿到帖子ID字段名
        String idFieldName = checkPost.value();
        //获取帖子ID
        Long postId = AspectUtil.getResourceId(dto, idFieldName);
        //拿到帖子实体
        Post post = postMapper.selectById(postId);
        //校验
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        if (!post.getVisibility().equals(0L)) {
            throw new BusinessException(400, "帖子不可见");
        }
        //校验通过，将帖子实体设置到ThreadLocal中
        try {
            ValidatedEntityHolder.set(post);
            return joinPoint.proceed();
        } finally {
            ValidatedEntityHolder.clear();
        }
    }
}