package org.example.note.category.impl;

import lombok.RequiredArgsConstructor;
import org.example.common.annotation.CheckOwner;
import org.example.common.annotation.CheckResource;
import org.example.note.category.dto.*;
import org.example.note.category.entity.Category;
import org.example.note.category.CategoryService;
import org.example.note.category.mapper.CategoryMapper;
import org.example.note.category.vo.CategoryTreeNodeVO;
import org.example.note.category.vo.CategoryVO;
import org.example.common.logincheck.UserContext;
import org.example.note.document.entity.Document;
import org.example.note.document.mapper.DocumentMapper;
import org.example.note.document.vo.DocumentNodeVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;

    private final DocumentMapper documentMapper;

    @Override
    @CacheEvict(value = "categoryTree",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    @CheckOwner(value = {@CheckResource(type = "category", idParam = "parentId")})
    public CategoryVO createCategory(CreateCategoryDTO createCategoryDTO) {
        //判断目录名是否顶级目录
        if(createCategoryDTO.getParentId() == null)
            log.info("用户 {} 创建了顶级目录: {}", UserContext.getUserId(), createCategoryDTO.getName());
        else{
        //如果不是顶级目录进行再判断
            log.info("用户 {} 创建了子目录: {}, 父目录ID: {}", UserContext.getUserId(), createCategoryDTO.getName(), createCategoryDTO.getParentId());
        }
        //没问题之后把传入的属性插入到数据库中，生成id
        Category category = new Category();
        category.setName(createCategoryDTO.getName());
        category.setParentId(createCategoryDTO.getParentId());
        category.setUserId(UserContext.getUserId());
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return new CategoryVO(category.getId(), category.getName(), category.getParentId(), category.getUserId(), category.getCreateTime());
    }

    @Override
    public List<CategoryVO> listCategory(ListCategoryDTO listCategoryDTO) {
        //记录查询日志
        log.info("用户{}查询了目录列表", UserContext.getUserId());
        //如果父目录id为null，则查询该用户下的所有顶级目录，否则查询该父目录下的子目录
        return categoryMapper.selectByParentId(listCategoryDTO.getParentId(), UserContext.getUserId()).stream().
                map(category -> new CategoryVO(category.getId(), category.getName(),
                    category.getParentId(), category.getUserId(), category.getCreateTime())).toList();
    }

    @Override
    @CacheEvict(value = "categoryTree",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    @CheckOwner(value = {@CheckResource(type = "category", idParam = "id")})
    public CategoryVO updateCategory(UpdateCategoryDTO updateCategoryDTO) {
        //通过传入的id，拿到了该目录对象
        Category category = categoryMapper.selectById(updateCategoryDTO.getId());
        //重命名目录
        category.setName(updateCategoryDTO.getName());
        //把拿到的目录对象传入Mapper拿来重命名
        int size = categoryMapper.updateById(category);
        log.info("目录重命名: {} -> {}, 影响 {} 条", category.getName(), updateCategoryDTO.getName(), size);
        return new CategoryVO(category.getId(), updateCategoryDTO.getName(),
            category.getParentId(), category.getUserId(), category.getCreateTime());
    }

    @Override
    @CacheEvict(value = "categoryTree",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    @CheckOwner(value = {@CheckResource(type = "category", idParam = "id")})
    public Map<String, Object> deleteCategory(DeleteCategoryDTO deleteCategoryDTO) {
        //通过传入的id，拿到了该目录对象
        Category category = categoryMapper.selectByIdIncludeDeleted(deleteCategoryDTO.getId());
        //创建Map用来存储提醒信息
        Map<String, Object> info = new HashMap<>();
        //判断是否强制删除
        if(deleteCategoryDTO.getForce() == 0){
            //通过传进来的id，查找它的子目录，返回到这个List当中
            List<Category> children = categoryMapper.selectByParentId(
                    deleteCategoryDTO.getId(),
                    UserContext.getUserId());
            //判断删除状态
            if (category.getIsDeleted() == 0) {
                //删除状态为0，判断是否有子目录，如果有，给前端提醒，若要再删除，则更改force为1，再传请求
                if (!children.isEmpty()) {
                    info = getMapResult(children, "删除");
                }
                else{
                    categoryMapper.deleteById(deleteCategoryDTO.getId());
                    info.put("message", "删除成功");
                }
            }
            else {
                //删除状态为1，判断是否有子目录，如果有，给前端提醒，若要再清理，则更改force为1，再传请求
                if (!children.isEmpty()) {
                    info = getMapResult(children, "清理");
                }
                else {
                    categoryMapper.hardDeleteById(deleteCategoryDTO.getId());
                    info.put("message", "清理成功");
                }
            }
        }
        else {
            //判断删除状态，若为0，则软删，若为1，则硬删
            if (category.getIsDeleted() == 0) {
                List<Category> list = categoryMapper.selectAllByUserId(UserContext.getUserId());
                deleteCategory1(deleteCategoryDTO.getId(), list);
                info.put("message", "删除成功");
            }
            else{
                List<Category> list = categoryMapper.selectAllByUserId(UserContext.getUserId());
                deleteCategory2(deleteCategoryDTO.getId(), list);
                info.put("message", "清理成功");
            }
        }
        return info;
    }

    //抽取出来的方法，是给前端返回的提醒信息，只服务于删除方法
    private Map<String, Object> getMapResult(List<Category> children, String message) {
        Map<String, Object> info = new HashMap<>();
        info.put("hasChildren", true);
        info.put("count", children.size());
        info.put("message", "该目录下有 " + children.size() + " 个子目录，是否确认全部" + message + "？");
        return info;
    }
    //递归软删，服务于上边的删除目录方法
    private void deleteCategory1(Long id, List<Category> list) {
        log.info("递归软删目录 {} 及其所有子目录", id);
        List<Category> children = new ArrayList<>();
        //从该用户下所有目录拿出传进来的目录id的子目录
        for(Category category : list){
            if(category.getParentId().equals(id)){
                children.add(category);
            }
        }
        //遍历该目录序列递归
        for(Category category : children){
            deleteCategory1(category.getId(), list);
        }
        //先删除文档后删除目录
        int docSize = categoryMapper.softDeleteDocumentByCategoryId(id);
        log.info("软删目录 {} 下的所有文档，影响 {} 条", id, docSize);
        categoryMapper.deleteById(id);
    }
    //递归硬删，服务于上边的删除目录方法
    private void deleteCategory2(Long id, List<Category> list) {
        log.info("递归硬删目录 {} 及其所有子目录", id);
        List<Category> children = new ArrayList<>();
        for(Category category : list){
            if(category.getParentId().equals(id)){
                children.add(category);
            }
        }
        for(Category category : children){
            deleteCategory2(category.getId(), list);
        }
        List<Long> docIds = categoryMapper.selectIdsByCategoryId(id);
        for(Long docId : docIds){
            categoryMapper.deleteByDocumentId(docId);
        }
        int size = categoryMapper.deleteByCategoryId(id);
        log.info("硬删目录 {} 下的所有文档，影响 {} 条", id, size);
        categoryMapper.hardDeleteById(id);
    }

    @Override
    @Cacheable(value = "categoryTree",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    public List<CategoryTreeNodeVO> getTree() {
        //拿到该用户下的所有目录
        List<Category> categories = categoryMapper.selectByUserId(UserContext.getUserId());
        //遍历该用户下的所有目录,全部转化为VO节点，存入一个Map集合
        Map<Long, CategoryTreeNodeVO> map = new HashMap<>();
        for (Category category : categories) {
            //创建VO节点
            CategoryTreeNodeVO node = new CategoryTreeNodeVO(category.getId(), category.getName(),
                    category.getParentId(), new ArrayList<>(), new ArrayList<>());
            //存入Map
            map.put(category.getId(), node);
        }
        //提前创建根节点集合
        List<CategoryTreeNodeVO> roots = new ArrayList<>();
        //遍历，建立目录节点之间的关系
        for (Map.Entry<Long, CategoryTreeNodeVO> entry : map.entrySet()) {
            //提取父节点id
            Long parent = entry.getValue().getParentId();
            //如果父节点id为null，则该节点为根节点
            if (parent == null) {
                roots.add(entry.getValue());
            }
            //否则，该节点为子节点，则进行关系建立
            else {
                //提取父节点的VO形式
                CategoryTreeNodeVO parentNode = map.get( parent);
                //遍历前判断父节点的子节点列表是否为空，为空则初始化，多一层保险
                if (parentNode.getChildren() == null) {
                    parentNode.setChildren(new ArrayList<>());
                }
                //之后再将子节点加入到父节点的children属性中
                parentNode.getChildren().add(entry.getValue());
            }
        }
        //拿到所有文档
        List<Document> documents = documentMapper.selectDocumentIdsAndTitlesByUserId(UserContext.getUserId());
        //遍历所有文档，为了将文档放在所属目录的节点中的文档集合字段中
        for (Document document : documents) {
            //新建文档节点将其存入所属目录的文档集合字段中，所属目录是通过parentId在map中拿到的
            CategoryTreeNodeVO node = map.get(document.getCategoryId());
            if(node == null) continue;
            node.getDocuments().add(new DocumentNodeVO(document.getId(), document.getTitle()));
        }
        //记录构建目录树日志
        log.info("用户 {} 的目录树构建完成，共 {} 个根节点", UserContext.getUserId(), roots.size());
        return roots;
    }
    @Override
    @CacheEvict(value = "categoryTree",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    @CheckOwner(value = {@CheckResource(type = "category", idParam = "id")})
    public void restoreCategory(RestoreCategoryDTO restoreCategoryDTO) {
        //递归恢复该目录下的所有子目录，包括该目录本身
        restoreCategory1(restoreCategoryDTO.getId(), UserContext.getUserId());
    }

    private void restoreCategory1(Long categoryId, Long userId){
        log.info("递归恢复目录 {} 及其所有子目录", categoryId);
        List<Category> list = categoryMapper.selectByParentIdIncludeDeleted(categoryId, userId);
        for(Category category : list){
            restoreCategory1(category.getId(), userId);
        }
        int size = categoryMapper.restoreDocumentByCategoryId(categoryId);
        log.info("恢复目录 {} 下的所有文档，影响 {} 条", categoryId, size);
        categoryMapper.restoreById(categoryId);
    }
    //查询被软删除的目录
    @Override
    public List<CategoryVO> listTrash() {
        List<Category> list = categoryMapper.selectDeletedByUserId(UserContext.getUserId());
        return list.stream()
                .map(c -> new CategoryVO(c.getId(), c.getName(), c.getParentId(), c.getUserId(), c.getCreateTime()))
                .toList();
    }

}