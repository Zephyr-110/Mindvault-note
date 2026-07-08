package org.example.note.document.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckOwner;
import org.example.common.annotation.CheckResource;
import org.example.common.logincheck.UserContext;
import org.example.note.document.dto.*;
import org.example.note.category.entity.Category;
import org.example.note.document.entity.Document;
import org.example.common.result.PageResult;
import org.example.note.tag.entity.Tag;
import org.example.note.category.mapper.CategoryMapper;
import org.example.note.document.DocumentService;
import org.example.note.document.mapper.DocumentMapper;
import org.example.note.document.vo.DocumentDetailVO;
import org.example.note.document.vo.DocumentVO;
import org.example.note.tag.vo.TagVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;

    private final CategoryMapper categoryMapper;

    @Transactional
    @Override
    @CacheEvict(value = "categoryTree",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    @CheckOwner(value = {@CheckResource(type = "category", idParam = "categoryId")})
    public DocumentVO createDocument(CreateDocumentDTO createDocumentDTO) {
        //查找文档归属目录
        Category category = categoryMapper.selectById(createDocumentDTO.getCategoryId());
        //记录日志
        log.info("在目录 {} 下创建了文档 {}", category.getName(), createDocumentDTO.getTitle());
        Document document = new Document();
        document.setTitle(createDocumentDTO.getTitle());
        document.setContent(createDocumentDTO.getContent());
        document.setCategoryId(createDocumentDTO.getCategoryId());
        document.setUserId(UserContext.getUserId());
        document.setCreateTime(LocalDateTime.now());
        //插入文档
        documentMapper.insert(document);
        //判断前端是否传入标签，如果有，则插入
        if(createDocumentDTO.getTagIds() != null && !createDocumentDTO.getTagIds().isEmpty()){
            documentMapper.insertDocumentTags(document.getId(), createDocumentDTO.getTagIds());
        }
        //因为是创建文档，所以这次的更改时间也应该是创建时间
        return new DocumentVO(document.getId(), document.getTitle(), document.getContent(), document.getCategoryId(), document.getUserId(), document.getCreateTime(), document.getCreateTime(), null);
    }

    @Override
    @CheckOwner(value = {@CheckResource(type = "document", idParam = "documentId")})
    public DocumentDetailVO getDocumentDetail(DocumentDetailDTO documentDetailDTO) {
        Document document = documentMapper.selectById(documentDetailDTO.getDocumentId());
        //获取文档关联的标签列表
        List<Tag> tags = documentMapper.selectTagsByDocumentId(documentDetailDTO.getDocumentId());
        if(tags == null) tags = List.of();//避免空指针
        List<TagVO> tagVOS = tags.stream().map(tag -> new TagVO(tag.getId(), tag.getName())).toList();
        //记录查询日志
        log.info("查询文档 {} 详情成功，关联标签 {} 个", documentDetailDTO.getDocumentId(), tagVOS.size());
        return new DocumentDetailVO(document.getId(), document.getTitle(), document.getContent(),
                document.getCategoryId(), document.getUserId(), document.getCreateTime(),
                document.getUpdateTime(), tagVOS);
    }

    @Override
    @CheckOwner(value = {@CheckResource(type = "category", idParam = "categoryId")})
    public PageResult<DocumentVO> listDocuments(ListDocumentDTO listDocumentDTO) {
        Long targetUserId = listDocumentDTO.getUserId() != null
                ? listDocumentDTO.getUserId() : UserContext.getUserId();
        //创建新的VO对象
        PageResult<DocumentVO> pageResult = new PageResult<>();
        //通过转化，获取总条数
        int selectTotal = documentMapper.countByCondition(targetUserId, listDocumentDTO.getCategoryId(), listDocumentDTO.getKeyword());
        Long total = (long) selectTotal;
        //给pageResult赋值
        pageResult.setTotal(total);
        pageResult.setPage(listDocumentDTO.getPage());
        pageResult.setSize(listDocumentDTO.getSize());
        //用泛型为Document的列表接收到查询数据库得到的数据
        List<Document> list = documentMapper.selectByCondition(targetUserId, listDocumentDTO.getCategoryId(), listDocumentDTO.getKeyword(),
                (listDocumentDTO.getPage() - 1) * listDocumentDTO.getSize(), listDocumentDTO.getSize());
        //转化为VO对象，放入pageResult中
        List<DocumentVO> documentVOS = list.stream().map(document -> new DocumentVO(document.getId(), document.getTitle(), document.getContent(),
                document.getCategoryId(), document.getUserId(), document.getCreateTime(), document.getUpdateTime(),  null)).toList();
        setPreview(documentVOS, listDocumentDTO.getKeyword());
        pageResult.setList(documentVOS);
        return pageResult;
     }
    //遍历文档列表，给每个文档设置预览片段，不是重写方法，只服务于上边的listDocuments方法
    private void setPreview(List<DocumentVO> documentVOS, String keyword) {
        if(keyword == null) return;
        for (DocumentVO documentVO : documentVOS) {
            //该文档没有相关内容则跳过继续下一层循环
            if(documentVO.getContent() == null) continue;
            //提取内容
            String content = documentVO.getContent();
            //先把内容和关键字都转为小写以便于找到准确索引
            String lowerContent = documentVO.getContent().toLowerCase();
            String lowerKeyword = keyword.toLowerCase();
            //搜索到关键字的头尾位置
            int startIndex = lowerContent.indexOf(lowerKeyword);
            int endIndex = startIndex + keyword.length();
            if (startIndex == -1) {
                documentVO.setPreview(content.substring(0, Math.min(60, content.length())) + "...");
                continue;
            }
            //定义要截取片段的头位置
            int start = Math.max(startIndex - 30, 0);
            //定义要截取片段的尾位置
            int end = Math.min(endIndex + 30, content.length());
            //定义预览片段
            String preview = "";
            //截取片段
            if(start > 0) preview = preview + ". . .";
            preview = preview + content.substring(start, end);
            if(end < content.length()) preview = preview + ". . .";
            //赋给VO对象
            documentVO.setPreview(preview);
        }
    }

     @Transactional
     @Override
     @CacheEvict(value = "categoryTree",
             key = "T(org.example.common.logincheck.UserContext).getUserId()")
     @CheckOwner(value = {
             @CheckResource(type = "document", idParam = "documentId"),
             @CheckResource(type = "category", idParam = "categoryId")})
    public DocumentVO updateDocument(UpdateDocumentDTO updateDocumentDTO) {
        Document document = documentMapper.selectById(updateDocumentDTO.getDocumentId());
        //文档更新前记录日志
        log.info("文档 {} 更新前 - 标题: {}, 目录ID: {}", document.getId(), document.getTitle(), document.getCategoryId());
        if (updateDocumentDTO.getCategoryId() != null) {
            //更新文档
             document.setCategoryId(updateDocumentDTO.getCategoryId());
        }
        document.setTitle(updateDocumentDTO.getTitle());
        document.setContent(updateDocumentDTO.getContent());
        document.setUpdateTime(LocalDateTime.now());
        //判断前端是否传入标签，如果有，则先删除旧关联，然后插入新关联，如果没有则无需理会
        //更新编辑后文档
        documentMapper.updateById(document);
        //先删后插的原因：
        //为了统一操作，我们后端只负责前端选择了哪些标签，后端只要管删除了旧的关联，然后插入新的关联就可以保证运行
        if (updateDocumentDTO.getTagIds() != null) {
            documentMapper.deleteDocumentTags(updateDocumentDTO.getDocumentId());  // 先清掉旧关联
             if (!updateDocumentDTO.getTagIds().isEmpty()) {
                 documentMapper.insertDocumentTags(updateDocumentDTO.getDocumentId(), updateDocumentDTO.getTagIds());  // 再插新的
             }
        }
         //文档更新后记录日志
        log.info("文档 {} 更新成功 - 标题: {}, 目录ID: {}", document.getId(), document.getTitle(), document.getCategoryId());
        //用前边处理过的文档对象包装成VO返回
        return new DocumentVO(document.getId(), document.getTitle(), document.getContent(),
                document.getCategoryId(), document.getUserId(), document.getCreateTime(),
                document.getUpdateTime(), null);
    }

     @Transactional
     @Override
     @CacheEvict(value = "categoryTree",
             key = "T(org.example.common.logincheck.UserContext).getUserId()")
     @CheckOwner(value = {@CheckResource(type = "document", idParam = "documentId")})
     public void deleteDocument(DeleteDocumentDTO deleteDocumentDTO) {
        //拿到文档对象
        Document document = documentMapper.selectByIdIncludeDeleted(deleteDocumentDTO.getDocumentId());
        //判断需要软删除还是清理
         if (document.getIsDeleted() == 0) {
             int deleteCount = documentMapper.deleteById(deleteDocumentDTO.getDocumentId());
             log.info("软删除{}条文档", deleteCount);
         }
         else{
             documentMapper.deleteDocumentTags(deleteDocumentDTO.getDocumentId());
             int deleteCount = documentMapper.hardDeleteById(deleteDocumentDTO.getDocumentId());
             log.info("清理{}条文档", deleteCount);
         }
     }

    @Override
    @CacheEvict(value = "categoryTree",
            key = "T(org.example.common.logincheck.UserContext).getUserId()")
    @CheckOwner(value = {@CheckResource(type = "document", idParam = "documentId")})
    public void restoreDocument(RestoreDocumentDTO restoreDocumentDTO) {
        Document document = documentMapper.selectByIdIncludeDeleted(restoreDocumentDTO.getDocumentId());
        if (document.getIsDeleted() == 1) {
            documentMapper.restoreById(restoreDocumentDTO.getDocumentId());
            log.info("恢复文档{}", document.getTitle());
        }
    }

    //查询被软删除的文档
    @Override
    public List<DocumentVO> listTrash() {
        List<Document> list = documentMapper.selectDeletedByUserId(UserContext.getUserId());
        return list.stream()
                .map(d -> new DocumentVO(d.getId(), d.getTitle(), null, d.getCategoryId(), d.getUserId(), d.getCreateTime(), d.getUpdateTime(), null))
                .toList();
    }

    public Long getUserId() {
        return UserContext.getUserId();
    }


}