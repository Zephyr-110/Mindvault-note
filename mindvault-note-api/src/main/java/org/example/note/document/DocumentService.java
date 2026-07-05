package org.example.note.document;

import org.example.note.document.dto.*;
import org.example.note.document.vo.DocumentDetailVO;
import org.example.note.document.vo.DocumentVO;
import org.example.common.result.PageResult;

import java.util.List;


public interface DocumentService {

    DocumentVO createDocument(CreateDocumentDTO createDocumentDTO);

    DocumentDetailVO getDocumentDetail(DocumentDetailDTO documentDetailDTO);

    PageResult<DocumentVO> listDocuments(ListDocumentDTO listDocumentDTO);

    DocumentVO updateDocument(UpdateDocumentDTO dto);

    void deleteDocument(DeleteDocumentDTO deleteDocumentDTO);

    void restoreDocument(RestoreDocumentDTO restoreDocumentDTO);
    List<DocumentVO> listTrash();
}