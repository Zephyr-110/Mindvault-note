package org.example.note.common.resolver;

import lombok.RequiredArgsConstructor;
import org.example.common.exception.BusinessException;
import org.example.common.resolver.ResourceOwnerResolver;
import org.example.note.category.mapper.CategoryMapper;
import org.example.note.category.entity.Category;
import org.example.note.document.mapper.DocumentMapper;
import org.example.note.document.entity.Document;
import org.example.note.tag.mapper.TagMapper;
import org.example.note.tag.entity.Tag;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoteOwnerResolver implements ResourceOwnerResolver {

    private final DocumentMapper documentMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;


    //判断是否支持传递进来的资源类型
    @Override
    public boolean supports(String resourceType) {
        //直接拿字符串和参数字段类型去比较，返回布尔值
        return "document".equals(resourceType)
                || "category".equals(resourceType)
                || "tag".equals(resourceType);
    }

    @Override
    public Long getOwnerId(Long resourceId, String resourceType) {
        return switch (resourceType) {
            case "document" -> {
                Document doc = documentMapper.selectByIdIncludeDeleted(resourceId);
                if (doc == null) throw new BusinessException(404, "文档不存在");
                yield doc.getUserId();
            }
            case "category" -> {
                Category cat = categoryMapper.selectByIdIncludeDeleted(resourceId);
                if (cat == null) throw new BusinessException(404, "目录不存在");
                yield cat.getUserId();
            }
            case "tag" -> {
                Tag tag = tagMapper.selectById(resourceId);
                if (tag == null) throw new BusinessException(404, "标签不存在");
                yield tag.getUserId();
            }
            default -> throw new BusinessException(500, "不支持该资源类型: " + resourceType);
        };
    }
}