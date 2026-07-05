package org.example.community.post;

import org.example.community.post.dto.CreatePostDTO;
import org.example.community.post.dto.DeletePostDTO;
import org.example.community.post.dto.FeedDTO;
import org.example.community.post.dto.PostDetailDTO;
import org.example.common.result.PageResult;
import org.example.community.post.vo.PostVO;

public interface PostService {

    PostVO createPost(CreatePostDTO createPostDTO);

    PostVO getPostDetail(PostDetailDTO dto);

    void deletePost(DeletePostDTO dto);

    PageResult<PostVO> getFeed(FeedDTO dto);
}