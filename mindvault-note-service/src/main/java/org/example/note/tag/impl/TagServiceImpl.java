package org.example.note.tag.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckOwner;
import org.example.common.annotation.CheckResource;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.note.tag.dto.CreateTagDTO;
import org.example.note.tag.dto.DeleteTagDTO;
import org.example.note.tag.entity.Tag;
import org.example.note.tag.TagService;
import org.example.note.tag.mapper.TagMapper;
import org.example.note.tag.vo.TagVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    @CacheEvict(value = "tagList",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    public TagVO createTag(CreateTagDTO createTagDTO) {
        if (tagMapper.selectByNameAndUserId(createTagDTO.getName(), UserContext.getUserId()) != null)
            throw new BusinessException(400, "标签已存在");
        Tag tag = new Tag();
        tag.setName(createTagDTO.getName());
        tag.setUserId(UserContext.getUserId());
        tagMapper.insert(tag);
        log.info("用户 {} 创建标签成功: {}", UserContext.getUserId(), tag.getName());
        return new TagVO(tag.getId(), tag.getName());
    }

    @Override
    @Cacheable(value = "tagList",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    public List<TagVO> listTags() {
        List<Tag> tags = tagMapper.selectByUserId(UserContext.getUserId());
        log.info("用户 {} 查询标签列表，共 {} 个", UserContext.getUserId(), tags.size());
        return tags.stream().map(tag -> new TagVO(tag.getId(), tag.getName())).toList();
    }

    @Transactional
    @Override
    @CacheEvict(value = "tagList",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    @CheckOwner(value = {@CheckResource(type = "tag", idParam = "tagId")})
    public void deleteTag(DeleteTagDTO deleteTagDTO) {
        int countTagAndDocument = tagMapper.deleteDocumentTagsByTagId(deleteTagDTO.getTagId());
        int countTag = tagMapper.deleteById(deleteTagDTO.getTagId());
        log.info("用户 {} 删除标签 {}，清理关联 {} 条，删除标签 {} 条",
                UserContext.getUserId(), deleteTagDTO.getTagId(), countTagAndDocument, countTag);
    }

}