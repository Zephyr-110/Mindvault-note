package org.example.note.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.note.category.entity.Category;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    //通过父目录id查目录
    List<Category> selectByParentId(Long parentId, Long userId);
    //删目录下的所有文档
    int deleteByCategoryId(Long categoryId);
    //查目录下的所有文档，用于删除标签关联
    List<Long> selectIdsByCategoryId(Long categoryId);
    //删文档的所有标签关联
    int deleteByDocumentId(Long documentId);
    //查改用户下所有目录
    List<Category> selectByUserId(Long userId);
    //硬删目录（不走软删除）
    int hardDeleteById(Long id);
    //通过目录id修改该目录下所有文档的删除状态为1，删除时间当前时间
    int softDeleteDocumentByCategoryId(Long categoryId);
    //恢复被软删除的目录,删除时间清空
    int restoreById(Long id);
    //通过目录id修改该目录下所有文档的删除状态为0，删除时间清空
    int restoreDocumentByCategoryId(Long categoryId);
    //通过目录id查目录（包含已删除的）
    Category selectByIdIncludeDeleted(Long id);
    //通过父目录id查目录（包含已删除的）
    List<Category> selectByParentIdIncludeDeleted(Long parentId, Long userId);
    //通过用户id查已删除的目录（回收站用）
    List<Category> selectDeletedByUserId(Long userId);
    //通过用户id查所有目录
    List<Category> selectAllByUserId(Long userId);

}