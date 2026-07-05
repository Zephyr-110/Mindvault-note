package org.example.note.tag;

import org.example.note.tag.dto.CreateTagDTO;
import org.example.note.tag.dto.DeleteTagDTO;
import org.example.note.tag.vo.TagVO;

import java.util.List;

public interface TagService {
    TagVO createTag(CreateTagDTO createTagDTO);
    List<TagVO> listTags();
    void deleteTag(DeleteTagDTO deleteTagDTO);
}
