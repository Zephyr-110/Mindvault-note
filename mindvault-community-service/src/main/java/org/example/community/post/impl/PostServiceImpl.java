package org.example.community.post.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ValidatedEntityHolder;
import org.example.common.annotation.CheckOwner;
import org.example.common.annotation.CheckPost;
import org.example.common.annotation.CheckResource;
import org.example.community.comment.entity.Comment;
import org.example.community.comment.mapper.CommentMapper;
import org.example.common.logincheck.UserContext;
import org.example.community.favorite.FavoriteService;
import org.example.community.favorite.entity.Favorite;
import org.example.community.favorite.mapper.FavoriteMapper;
import org.example.community.follow.FollowService;
import org.example.community.follow.mapper.FollowMapper;
import org.example.community.likerecord.LikeRecordService;
import org.example.community.likerecord.entity.LikeRecord;
import org.example.community.likerecord.mapper.LikeRecordMapper;
import org.example.community.post.vo.ListDocumentAccessoryVO;
import org.example.user.user.service.UserService;
import org.example.community.post.dto.CreatePostDTO;
import org.example.community.post.dto.DeletePostDTO;
import org.example.community.post.dto.FeedDTO;
import org.example.community.post.dto.PostDetailDTO;
import org.example.community.post.entity.Post;
import org.example.community.post.PostService;
import org.example.community.post.mapper.PostMapper;
import org.example.common.result.PageResult;
import org.example.community.post.vo.PostVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final UserService userService;
    private final LikeRecordService likeRecordService;
    private final LikeRecordMapper likeRecordMapper;
    private final CommentMapper commentMapper;
    private final FavoriteService favoriteService;
    private final FavoriteMapper favoriteMapper;
    private final FollowMapper followMapper;
    private final FollowService followService;



    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "feed", allEntries = true)
    public PostVO createPost(CreatePostDTO createPostDTO) {
        Long authorId = UserContext.getUserId();
        //封装Post用于插入数据库
        Post post = new Post();
        post.setTitle(createPostDTO.getTitle());
        post.setContent(createPostDTO.getContent());
        post.setAuthorId(authorId);
        post.setVisibility(createPostDTO.getVisibility());
        post.setOriginalPostId(createPostDTO.getOriginalPostId());
        if (createPostDTO.getDocumentAccessories() != null && !createPostDTO.getDocumentAccessories().isEmpty()) {
            post.setNoteAccessory(createPostDTO.getDocumentAccessories().stream()
                    .map(d -> d.getDocumentId() + ":" + d.getTitle())
                    .collect(Collectors.joining(";")));
        }
        post.setCreateTime(LocalDateTime.now());
        postMapper.insert(post);
        //利用已经在内存封装好的Post对象，封装VO
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setAuthorId(authorId);
        //作者昵称
        vo.setAuthorNickname(userService.getNickname(authorId));
        //作者头像
        vo.setAuthorAvatar(userService.getAvatar(authorId));
        //可见性
        vo.setVisibility(post.getVisibility());
        //转发帖原帖id
        vo.setOriginalPostId(post.getOriginalPostId());
        //赞数
        vo.setLikeCount(0L);
        //评论数
        vo.setCommentCount(0L);
        //收藏数
        vo.setFavoriteCount(0L);
        //是否点赞
        vo.setIsLiked(false);
        //是否收藏
        vo.setIsFavorited(false);
        //是否关注
        vo.setIsFollowed(false);
        vo.setCreateTime(post.getCreateTime());
        //文档的笔记附件
        vo.setDocumentAccessories(createPostDTO.getDocumentAccessories());
        return vo;
    }

    @Override
    @CheckPost("postId")
    @Cacheable(value = "postDetail", key = "#dto.postId")
    public PostVO getPostDetail(PostDetailDTO dto) {
        //查询帖子
        Post post = ValidatedEntityHolder.get(Post.class);
        //封装返回
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setAuthorId(post.getAuthorId());
        vo.setAuthorNickname(userService.getNickname(post.getAuthorId()));
        vo.setAuthorAvatar(userService.getAvatar(post.getAuthorId()));
        vo.setVisibility(post.getVisibility());
        vo.setOriginalPostId(post.getOriginalPostId());
        //封装点赞数
        vo.setLikeCount(likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>().
                eq(LikeRecord::getTargetId, post.getId())
        .eq(LikeRecord::getTargetType, "post")));
        //封装评论数
        vo.setCommentCount(commentMapper.selectCount(new LambdaQueryWrapper<Comment>().
                eq(Comment::getPostId, post.getId())));
        //封装收藏数
        vo.setFavoriteCount(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getPostId, post.getId())));
        //封装是否点赞
        vo.setIsLiked(likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, UserContext.getUserId())
                .eq(LikeRecord::getTargetId, post.getId())
                .eq(LikeRecord::getTargetType, "post")) > 0);
        //封装是否收藏
        vo.setIsFavorited(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, UserContext.getUserId())
                .eq(Favorite::getPostId, post.getId())) > 0);
        //封装是否关注
        Long myId = UserContext.getUserId();
        vo.setIsFollowed(followService.batchFollowedUserIds(myId, List.of(post.getAuthorId()))
                .contains(post.getAuthorId()));
        if(post.getNoteAccessory() != null) {
            vo.setDocumentAccessories(Arrays.stream(post.getNoteAccessory().split(";"))
                    .map(s -> {
                        String[] p = s.split(":", 2);
                        return new ListDocumentAccessoryVO(Long.parseLong(p[0]), p[1]);
                    }).toList());
        }
        vo.setCreateTime(post.getCreateTime());
        return vo;
    }

    @Override
    @CheckPost("postId")
    @Transactional(rollbackFor = Exception.class)
    @CheckOwner(@CheckResource(type = "post", idParam = "postId"))
    @Caching(evict = {
            @CacheEvict(value = "feed", allEntries = true),
            @CacheEvict(value = "postDetail", key = "#dto.postId")
    })
    public void deletePost(DeletePostDTO dto) {
        Post post = ValidatedEntityHolder.get(Post.class);
        //删评论的点赞
        List<Long> commentIds = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, post.getId()))
                .stream().map(Comment::getId).toList();
        if (!commentIds.isEmpty()) {
            likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>()
                    .in(LikeRecord::getTargetId, commentIds)
                    .eq(LikeRecord::getTargetType, "comment"));
        }
        //删评论
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, dto.getPostId()));
        //删帖子的点赞
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetId, dto.getPostId())
                .eq(LikeRecord::getTargetType, "post"));
        //删收藏
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getPostId, dto.getPostId()));
        //删帖子
        postMapper.deleteById(dto.getPostId());
    }

    @Override
    @Cacheable(value = "feed", key = "#dto.page + ':' + #dto.size")
    public PageResult<PostVO> getFeed(FeedDTO dto) {
        //分页查询帖子，回显全部可见帖子
        Page<Post> page = new Page<>(dto.getPage(), dto.getSize());
        Page<Post> postPage = postMapper.selectFeedPage(page, 0L);
        //批量查帖子的id
        List<Long> postIds = postPage.getRecords().stream()
                .map(Post::getId).toList();
        Long userId = UserContext.getUserId();
        //批量查点赞数（走Redis）
        Map<Long, Long> likeCountMap = likeRecordService.batchLikeCounts(postIds);
        //批量查当前用户点赞了哪些帖子（走Redis）
        Set<Long> likedPostIds = likeRecordService.batchLikedPostIds(userId, postIds);
        //批量查评论数
        Map<Long, Long>commentCountMap = commentMapper.selectList(
                        new LambdaQueryWrapper<Comment>()
                                .in(Comment::getPostId, postIds))
                .stream()
                .collect(Collectors.groupingBy(Comment::getPostId, Collectors.counting()));
        //批量查当前用户收藏了哪些帖子（走 Redis）
        Set<Long> favoritedPostIds = favoriteService.batchFavPostIds(userId, postIds);
        //批量查收藏数（走 Redis）
        Map<Long, Long> favoriteCountMap = favoriteService.batchFavCounts(postIds);
        //批量查当前用户关注了哪些作者（走 Redis）
        List<Long> authorIds = postPage.getRecords().stream()
                .map(Post::getAuthorId).distinct().toList();
        Set<Long> followedAuthorIds = followService.batchFollowedUserIds(userId, authorIds);
        //组装VO时直接从Map里取
        List<PostVO> voList = postPage.getRecords().stream().map(post -> {
            PostVO vo = new PostVO();
            vo.setId(post.getId());
            vo.setTitle(post.getTitle());
            vo.setContent(post.getContent());
            vo.setAuthorId(post.getAuthorId());
            vo.setAuthorNickname(userService.getNickname(post.getAuthorId()));
            vo.setAuthorAvatar(userService.getAvatar(post.getAuthorId()));
            vo.setVisibility(post.getVisibility());
            vo.setOriginalPostId(post.getOriginalPostId());
            vo.setLikeCount(likeCountMap.getOrDefault(post.getId(), 0L));
            vo.setCommentCount(commentCountMap.getOrDefault(post.getId(), 0L));
            vo.setFavoriteCount(favoriteCountMap.getOrDefault(post.getId(), 0L));
            vo.setIsLiked(likedPostIds.contains(post.getId()));
            vo.setIsFavorited(favoritedPostIds.contains(post.getId()));
            vo.setIsFollowed(followedAuthorIds.contains(post.getAuthorId()));
            if(post.getNoteAccessory() != null) {
                vo.setDocumentAccessories(Arrays.stream(post.getNoteAccessory().split(";"))
                        .map(s -> {
                            String[] p = s.split(":", 2);
                            return new ListDocumentAccessoryVO(Long.parseLong(p[0]), p[1]);
                        }).toList());
            }
            vo.setCreateTime(post.getCreateTime());
            return vo;
        }).toList();
        Long total = postPage.getTotal();
        return new PageResult<>(total, dto.getPage(), dto.getSize(), voList);
    }

}