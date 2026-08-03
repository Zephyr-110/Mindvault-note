package org.example.community.comment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ValidatedEntityHolder;
import org.example.common.annotation.CheckComment;
import org.example.common.annotation.CheckOwner;
import org.example.common.annotation.CheckPost;
import org.example.common.annotation.CheckResource;
import org.example.community.comment.dto.CreateCommentDTO;
import org.example.community.comment.dto.DeleteCommentDTO;
import org.example.community.comment.dto.ListCommentDTO;
import org.example.community.comment.entity.Comment;
import org.example.community.comment.CommentService;
import org.example.community.comment.mapper.CommentMapper;
import org.example.community.notification.NotificationService;
import org.example.community.comment.vo.CommentVO;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.community.likerecord.entity.LikeRecord;
import org.example.user.user.service.UserProfileService;
import org.example.user.user.service.UserService;
import org.example.community.likerecord.mapper.LikeRecordMapper;
import org.example.community.post.entity.Post;
import org.example.community.post.mapper.PostMapper;
import org.example.common.result.PageResult;
import org.example.user.user.vo.UserProfileVO;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {


    private final CommentMapper commentMapper;
    private final UserService userService;
    private final LikeRecordMapper likeRecordMapper;
    private final NotificationService notificationService;
    private final CacheManager cacheManager;
    private final UserProfileService userProfileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckPost(value = "postId")
    @CacheEvict(value = "postDetail", key = "#createCommentDTO.postId")
    public CommentVO createComment(CreateCommentDTO createCommentDTO) {
        //从切面拿到实体类
        Post post = ValidatedEntityHolder.get(Post.class);
        //创建评论的对象，拿到字段并插入数据库
        Comment comment = new Comment();
        comment.setPostId(createCommentDTO.getPostId());
        comment.setAuthorId(UserContext.getUserId());
        comment.setContent(createCommentDTO.getContent());
        comment.setParentId(createCommentDTO.getParentId());
        comment.setCreateTime(LocalDateTime.now());
        int size = commentMapper.insert(comment);
        log.info("用户 {} 创建了 {} 条评论", UserContext.getUserId(), size);
        notificationService.createNotification(post.getAuthorId(), "comment",
                UserContext.getUserId(), comment.getId());
        //通过父评论id查询父评论作者昵称
        Comment parent = null;
        if (comment.getParentId() != null) {
            parent = commentMapper.selectById(comment.getParentId());
            if (parent == null) {
                throw new BusinessException(400, "父评论不存在");
            }
            if (!parent.getPostId().equals(comment.getPostId())) {
                throw new BusinessException(400, "父评论不属于该帖子");
            }
        }
        //先将VO除了父评论昵称其他字段赋值
        CommentVO commentVO = new CommentVO(comment.getId(), comment.getPostId(), comment.getAuthorId(),
                userService.getNickname(comment.getAuthorId()), userService.getAvatar(comment.getAuthorId()), comment.getParentId(),
                null, comment.getContent(), comment.getCreateTime());
        //判断是否根评论，如果是则直接返回
        if (parent == null) {
            return commentVO;
        }
        //回复评论时，通知父评论作者
        notificationService.createNotification(parent.getAuthorId(), "reply",
                UserContext.getUserId(), comment.getId());
        //否则赋值父评论作者昵称
        commentVO.setParentNickname(userService.getNickname(parent.getAuthorId()));
        return commentVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckComment(value = "id")
    @CheckOwner(@CheckResource(type = "comment", idParam = "id"))
    public void deleteComment(DeleteCommentDTO deleteCommentDTO) {
        // 从切面拿到实体类
        Comment comment = ValidatedEntityHolder.get(Comment.class);
        // 删除该评论的点赞记录
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetId, deleteCommentDTO.getId())
                .eq(LikeRecord::getTargetType, "comment"));
        // 删除评论本身
        commentMapper.deleteById(deleteCommentDTO.getId());
        log.info("用户 {} 删除了条评论", UserContext.getUserId());
        // 清除帖子缓存
        Cache cache = cacheManager.getCache("postDetail");
        if (cache != null) {
            cache.evict(comment.getPostId());
        }
    }

    @Override
    @CheckPost(value = "postId")
    public PageResult<CommentVO> listComments(ListCommentDTO listCommentDTO) {
        //获取页码和每页条数
        Page<Comment> page = new Page<>(listCommentDTO.getPage(), listCommentDTO.getSize());
        Page<Comment> result = commentMapper.selectByPostId(page, listCommentDTO.getPostId());
        // 获取查到的评论id列表
        List<Long> parentIds = result.getRecords()
                .stream()
                .map(Comment::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // 创建Map，Key为这些评论id，Value为评论实体，为了取父评论ID
        Map<Long, Comment> parentMap = parentIds.isEmpty()
                ? Map.of()
                : commentMapper.selectByIds(parentIds)
                  .stream()
                  .collect(Collectors.toMap(Comment::getId, c -> c));
        // 获取查到的评论的作者列表
        Set<Long> authorIds = new HashSet<>(result.getRecords()
                .stream()
                .map(Comment::getAuthorId)
                .toList());
        // 把父评论作者ID添加到作者列表中
        parentMap.values().forEach(p -> authorIds.add(p.getAuthorId()));
        // 创建Map，Key为这些作者id，Value为作者信息，为了取作者昵称和头像
        Map<Long, UserProfileVO> userMap = authorIds.isEmpty()
                ? Map.of()
                : userProfileService.getUserProfileByIds(new ArrayList<>(authorIds));
        //提前创建返回值列表
        List<CommentVO> commentVOs = new ArrayList<>();
        //组装返回值列表
        for (Comment comment : result.getRecords()) {
            // 下方条件不满足则为根评论，返回空
            String parentNickname = null;
            if (comment.getParentId() != null) {
                // 如果父评论id存在，则从Map中获取实体类（如果父评论删了，就会不存在）
                Comment parent = parentMap.get(comment.getParentId());
                if (parent != null) {
                    // 如果实体类存在，则从Map中获取作者昵称
                    UserProfileVO pu = userMap.get(parent.getAuthorId());
                    parentNickname = pu != null ? pu.getNickname() : null;
                }
            }
            CommentVO commentVO = new CommentVO(
                    comment.getId(),
                    comment.getPostId(),
                    comment.getAuthorId(),
                    userMap.get(comment.getAuthorId()).getNickname(),
                    userMap.get(comment.getAuthorId()).getAvatar(),
                    comment.getParentId(),
                    parentNickname,
                    comment.getContent(),
                    comment.getCreateTime());
            commentVOs.add(commentVO);
        }
        return new PageResult<>(result.getTotal(), listCommentDTO.getPage(), listCommentDTO.getSize(), commentVOs);
    }
}