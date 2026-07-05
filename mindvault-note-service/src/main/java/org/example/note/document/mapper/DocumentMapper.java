package org.example.note.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.note.document.entity.Document;
import org.example.note.document.vo.DocumentVO;
import org.example.note.tag.entity.Tag;

import java.util.List;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
    //查看传进来的文档的所有标签
    List<Tag> selectTagsByDocumentId(Long documentId);
    //删除文档关联的标签
    int deleteDocumentTags(Long documentId);
    //批量插入关联
    int insertDocumentTags(@Param("documentId") Long documentId, @Param("tagIds") List<Long> tagIds);
    //查符合条件的文档数量
    int countByCondition(@Param("userId") Long userId,
                         @Param("categoryId") Long categoryId,
                         @Param("keyword") String keyword);
    //查符合条件的文档，分页查询
    List<Document> selectByCondition(@Param("userId") Long userId,
                     @Param("categoryId") Long categoryId,
                     @Param("keyword") String keyword,
                     @Param("offset") Long offset,
                     @Param("size") Long size);

    //通过用户id查所有文档
    List<Document> selectByUserId(Long userId);
    //硬删文档（不走软删除）
    int hardDeleteById(Long id);
    //恢复文档删除状态为0，删除时间清空
    int restoreById(Long id);
    //通过id查文档（包含已删除的）
    Document selectByIdIncludeDeleted(Long id);
    //通过用户id查已删除的文档（回收站用）
    List<Document> selectDeletedByUserId(Long userId);
    //查所有文档，只查id和title，用于查看目录下文档列表
    List<Document> selectDocumentIdsAndTitlesByUserId(Long userId);
}