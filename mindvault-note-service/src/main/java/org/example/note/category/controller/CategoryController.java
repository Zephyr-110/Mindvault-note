package org.example.note.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.note.category.CategoryService;
import org.example.note.category.dto.*;
import org.example.common.result.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/category/create")
    public Result<?> createCategory(@Valid @RequestBody CreateCategoryDTO createCategoryDTO) {
        return Result.success(categoryService.createCategory(createCategoryDTO));
    }

    @GetMapping("/category/list")
    public Result<?> listCategory(@Valid ListCategoryDTO listCategoryDTO) {
        return Result.success(categoryService.listCategory(listCategoryDTO));
    }

    @PutMapping("/category/update")
    public Result<?> updateCategory(@Valid UpdateCategoryDTO updateCategoryDTO) {
        return Result.success(categoryService.updateCategory(updateCategoryDTO));
    }

    @DeleteMapping("/category/delete")
    public Result<?> deleteCategory(@Valid DeleteCategoryDTO deleteCategoryDTO) {
        return Result.success(categoryService.deleteCategory(deleteCategoryDTO));
    }

    @GetMapping("/category/tree")
    public Result<?> getTree() {
        return Result.success(categoryService.getTree());
    }

    @PutMapping("/category/restore")
    public Result<?> restoreCategory(@Valid RestoreCategoryDTO restoreCategoryDTO) {
        categoryService.restoreCategory(restoreCategoryDTO);
        return Result.success();
    }

    @GetMapping("/category/trash")
    public Result<?> listTrashCategories() {
        return Result.success(categoryService.listTrash());
    }
}