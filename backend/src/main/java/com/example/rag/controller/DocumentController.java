package com.example.rag.controller;

import com.example.rag.common.Result;
import com.example.rag.model.entity.DocMeta;
import com.example.rag.security.SecurityUtil;
import com.example.rag.service.DocumentIngestService;
import com.example.rag.service.dto.IngestStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/kb")
public class DocumentController {

    private final DocumentIngestService documentIngestService;

    public DocumentController(DocumentIngestService documentIngestService) {
        this.documentIngestService = documentIngestService;
    }

    @PostMapping("/{kbId}/docs")
    public Result<String> upload(@PathVariable Long kbId, @RequestParam("file") MultipartFile file) {
        SecurityUtil.getCurrentUserId();
        return Result.ok(documentIngestService.ingest(kbId, file));
    }

    @GetMapping("/{kbId}/docs")
    public Result<List<DocMeta>> list(@PathVariable Long kbId) {
        SecurityUtil.getCurrentUserId();
        // 复用知识库服务校验，这里直接返回（service 内部已校验）
        return Result.ok(documentIngestService.listDocs(kbId));
    }

    @GetMapping("/{kbId}/docs/{docId}")
    public Result<DocMeta> get(@PathVariable Long kbId, @PathVariable Long docId) {
        SecurityUtil.getCurrentUserId();
        return Result.ok(documentIngestService.getDoc(kbId, docId));
    }

    @DeleteMapping("/{kbId}/docs/{docId}")
    public Result<Void> delete(@PathVariable Long kbId, @PathVariable Long docId) {
        SecurityUtil.getCurrentUserId();
        documentIngestService.deleteDocument(kbId, docId);
        return Result.ok();
    }

    @GetMapping("/{kbId}/docs/{docId}/chunks")
    public Result<List<String>> preview(@PathVariable Long kbId, @PathVariable Long docId,
                                        @RequestParam(defaultValue = "10") int limit) {
        SecurityUtil.getCurrentUserId();
        return Result.ok(documentIngestService.previewChunks(kbId, docId, limit));
    }

    @GetMapping("/ingest/status/{taskId}")
    public Result<IngestStatus> status(@PathVariable String taskId) {
        return Result.ok(documentIngestService.queryStatus(taskId));
    }
}
