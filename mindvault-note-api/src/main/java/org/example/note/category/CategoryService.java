package org.example.note.category;

import org.example.note.category.dto.*;
import org.example.note.category.vo.CategoryTreeNodeVO;
import org.example.note.category.vo.CategoryVO;

import java.util.List;
import java.util.Map;

public interface CategoryService {

    CategoryVO createCategory(CreateCategoryDTO createCategoryDTO);
    List<CategoryVO> listCategory(ListCategoryDTO listCategoryDTO);
    CategoryVO updateCategory(UpdateCategoryDTO updateCategoryDTO);
    Map<String, Object> deleteCategory(DeleteCategoryDTO deleteCategoryDTO);
    List<CategoryTreeNodeVO> getTree();
    void restoreCategory(RestoreCategoryDTO restoreCategoryDTO);
    List<CategoryVO> listTrash();
}