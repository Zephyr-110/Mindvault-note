package org.example.note.document.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.note.document.DocumentService;
import org.example.note.document.dto.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/document/create")
    public Result<?> createDocument(@Valid @RequestBody CreateDocumentDTO createDocumentDTO) {
        return Result.success(documentService.createDocument(createDocumentDTO));
    }

    @GetMapping("/document/detail")
    public Result<?> getDetail(@Valid DocumentDetailDTO documentDetailDTO) {
        return Result.success(documentService.getDocumentDetail(documentDetailDTO));
    }

    @GetMapping("/document/list")
    public Result<?> listDocuments(@Valid ListDocumentDTO listDocumentDTO) {
        return Result.success(documentService.listDocuments(listDocumentDTO));
    }

    @PutMapping("/document/update")
    public Result<?> updateDocument(@Valid UpdateDocumentDTO updateDocumentDTO) {
        return Result.success(documentService.updateDocument(updateDocumentDTO));
    }

    @DeleteMapping("/document/delete")
    public Result<?> deleteDocument(@Valid DeleteDocumentDTO deleteDocumentDTO) {
        documentService.deleteDocument(deleteDocumentDTO);
        return Result.success();
    }

    @PutMapping("/document/restore")
    public Result<?> restoreDocument(@Valid RestoreDocumentDTO restoreDocumentDTO) {
        documentService.restoreDocument(restoreDocumentDTO);
        return Result.success();
    }

    @GetMapping("/document/trash")
    public Result<?> listTrashDocuments() {
        return Result.success(documentService.listTrash());
    }
}