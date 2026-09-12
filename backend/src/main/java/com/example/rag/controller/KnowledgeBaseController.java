package com.example.rag.controller;

import com.example.rag.common.Result;
import com.example.rag.controller.dto.KbCreateRequest;
import com.example.rag.model.entity.KnowledgeBase;
import com.example.rag.security.SecurityUtil;
import com.example.rag.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    public Result<List<KnowledgeBase>> list(@RequestParam(required = false) String keyword) {
        Long uid = SecurityUtil.getCurrentUserId();
        return Result.ok(knowledgeBaseService.listMine(uid, keyword));
    }

    @PostMapping
    public Result<KnowledgeBase> create(@RequestBody KbCreateRequest request) {
        Long uid = SecurityUtil.getCurrentUserId();
        return Result.ok(knowledgeBaseService.create(uid, request));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeBase> get(@PathVariable Long id) {
        Long uid = SecurityUtil.getCurrentUserId();
        return Result.ok(knowledgeBaseService.get(uid, id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long uid = SecurityUtil.getCurrentUserId();
        knowledgeBaseService.delete(uid, id);
        return Result.ok();
    }
}
