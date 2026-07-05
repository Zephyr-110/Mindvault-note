package org.example.community.common.resolver;

import lombok.RequiredArgsConstructor;
import org.example.common.ValidatedEntityHolder;
import org.example.community.comment.entity.Comment;
import org.example.community.comment.mapper.CommentMapper;
import org.example.community.post.entity.Post;
import org.example.community.post.mapper.PostMapper;
import org.example.common.exception.BusinessException;
import org.example.common.resolver.ResourceOwnerResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityOwnerResolver implements ResourceOwnerResolver {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;


    //判断是否支持传递进来的资源类型
    @Override
    public boolean supports(String resourceType) {
        //直接拿字符串和参数字段类型去比较，返回布尔值
        return "post".equals(resourceType)
                || "comment".equals(resourceType);
    }

    @Override
    public Long getOwnerId(Long resourceId, String resourceType) {
        // 先从 ThreadLocal 拿（CheckPost/CheckComment 已经查过了，避免重复查库）
        Post post = ValidatedEntityHolder.get(Post.class);
        if (post != null) {
            return post.getAuthorId();
        }
        Comment comment = ValidatedEntityHolder.get(Comment.class);
        if (comment != null) {
            return comment.getAuthorId();
        }
        // 没拿到才按类型查库
        return switch (resourceType) {
            case "post" -> {
                Post p = postMapper.selectById(resourceId);
                if (p == null) throw new BusinessException(404, "帖子不存在");
                yield p.getAuthorId();
            }
            case "comment" -> {
                Comment c = commentMapper.selectById(resourceId);
                if (c == null) throw new BusinessException(404, "评论不存在");
                yield c.getAuthorId();
            }
            default -> throw new BusinessException(500, "不支持该资源类型: " + resourceType);
        };
    }
}