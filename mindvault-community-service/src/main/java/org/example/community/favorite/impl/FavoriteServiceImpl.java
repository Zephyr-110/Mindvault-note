package org.example.community.favorite.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckPost;
import org.example.common.redis.BitmapCounter;
import org.example.common.util.StringUtil;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.community.favorite.dto.FavoriteCountDTO;
import org.example.community.favorite.dto.ListFavoriteDTO;
import org.example.community.favorite.dto.ToggleFavoriteDTO;
import org.example.community.favorite.entity.Favorite;
import org.example.community.favorite.FavoriteService;
import org.example.community.favorite.mapper.FavoriteMapper;
import org.example.community.favorite.vo.FavoriteCountVO;
import org.example.community.favorite.vo.FavoriteVO;
import org.example.community.post.entity.Post;
import org.example.community.post.mapper.PostMapper;
import org.example.common.result.PageResult;
import org.example.community.post.vo.PostVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private static final String REDIS_FAV_MEMBERS = "fav:members:post:%d";
    private static final String REDIS_FAV_COUNT   = "fav:count:post:%d";

    private final FavoriteMapper favoriteMapper;
    private final PostMapper postMapper;
    private final BitmapCounter bitmapCounter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckPost(value = "postId")
    public FavoriteVO toggleFavorite(ToggleFavoriteDTO dto) {
        FavoriteVO favoriteVO = new FavoriteVO();
        Long postId = dto.getPostId();
        Long userId = UserContext.getUserId();

        // 拼 Redis key，冷启动兜底
        String membersKey = favMembersKey(postId);
        String countKey = favCountKey(postId);
        bitmapCounter.ensureInit(membersKey, countKey, () ->
                favoriteMapper.selectList(
                        new LambdaQueryWrapper<Favorite>()
                                .eq(Favorite::getPostId, postId))
                        .stream()
                        .map(Favorite::getUserId)
                        .toList()
        );

        // 从 Redis 查当前状态
        boolean alreadyFavorited = bitmapCounter.contains(membersKey, userId);

        // 写 DB
        if (alreadyFavorited) {
            Favorite favorite = favoriteMapper.selectOne(
                    new LambdaQueryWrapper<Favorite>()
                            .eq(Favorite::getUserId, userId)
                            .eq(Favorite::getPostId, postId));
            if (favorite != null) {
                favoriteMapper.deleteById(favorite);
                log.info("用户 {} 取消收藏帖子 {}", userId, postId);
            }
            bitmapCounter.remove(membersKey, countKey, userId);
            favoriteVO.setIsFavorited(false);
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setPostId(postId);
            favorite.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(favorite);
            log.info("用户 {} 收藏了帖子 {}", userId, postId);
            bitmapCounter.add(membersKey, countKey, userId);
            favoriteVO.setIsFavorited(true);
        }

        // 从 Redis 拿计数
        favoriteVO.setFavoriteCount(bitmapCounter.getCount(countKey));
        return favoriteVO;
    }


    @Override
    @CheckPost(value = "postId")
    public FavoriteCountVO countFavorites(FavoriteCountDTO dto) {
        Long postId = dto.getPostId();
        String membersKey = favMembersKey(postId);
        String countKey = favCountKey(postId);
        bitmapCounter.ensureInit(membersKey, countKey, () ->
                favoriteMapper.selectList(
                        new LambdaQueryWrapper<Favorite>()
                                .eq(Favorite::getPostId, postId))
                        .stream()
                        .map(Favorite::getUserId)
                        .toList()
        );
        return new FavoriteCountVO(postId, bitmapCounter.getCount(countKey));
    }

    @Override
    public PageResult<PostVO> listFavorites(ListFavoriteDTO listFavoriteDTO) {
        //查询该用户的收藏记录
        Page<Favorite> page = new Page<>(listFavoriteDTO.getPage(), listFavoriteDTO.getSize());
        Page<Favorite> result = favoriteMapper.selectPage(page,
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, UserContext.getUserId())
        );
        //拿到收藏的帖子的id
        List<Long> postIds = result.getRecords().stream()
                .map(Favorite::getPostId)
                .toList();
        //批量查询帖子实体类列表
        List<Post> posts = postIds.isEmpty() ? List.of() : postMapper.selectByIds(postIds);
        //组装VO返回
        List<PostVO> voList = posts.stream().map(post -> {
            PostVO vo = new PostVO();
            vo.setId(post.getId());
            vo.setTitle(post.getTitle());
            vo.setContent(StringUtil.truncate(post.getContent(), 100));
            vo.setAuthorId(post.getAuthorId());
            vo.setVisibility(post.getVisibility());
            vo.setOriginalPostId(post.getOriginalPostId());
            vo.setCreateTime(post.getCreateTime());
            return vo;
        }).toList();
        //返回分页回显收藏帖子列表
        return new PageResult<>(result.getTotal(), listFavoriteDTO.getPage(), listFavoriteDTO.getSize(), voList);

    }

    // ==================== 批量查询（供 getFeed 调用） ====================

    @Override
    public Set<Long> batchFavPostIds(Long userId, List<Long> postIds) {
        return postIds.stream()
                .filter(pid -> bitmapCounter.contains(favMembersKey(pid), userId))
                .collect(Collectors.toSet());
    }

    @Override
    public Map<Long, Long> batchFavCounts(List<Long> postIds) {
        return postIds.stream()
                .collect(Collectors.toMap(
                        pid -> pid,
                        pid -> bitmapCounter.getCount(favCountKey(pid))));
    }

    private String favMembersKey(Long postId) {
        return String.format(REDIS_FAV_MEMBERS, postId);
    }

    private String favCountKey(Long postId) {
        return String.format(REDIS_FAV_COUNT, postId);
    }

}