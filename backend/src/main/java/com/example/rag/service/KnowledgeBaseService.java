package com.example.rag.service;

import com.example.rag.controller.dto.KbCreateRequest;
import com.example.rag.model.entity.KnowledgeBase;
import com.example.rag.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    public List<KnowledgeBase> listMine(Long ownerId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return knowledgeBaseRepository.findByOwnerId(ownerId);
        }
        return knowledgeBaseRepository.findByOwnerIdAndNameContaining(ownerId, keyword.trim());
    }

    @Transactional
    public KnowledgeBase create(Long ownerId, KbCreateRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setEmbeddingModel(request.getEmbeddingModel() == null || request.getEmbeddingModel().isBlank()
                ? "qwen3-embedding:4b" : request.getEmbeddingModel());
        kb.setOwnerId(ownerId);
        return knowledgeBaseRepository.save(kb);
    }

    public KnowledgeBase get(Long ownerId, Long id) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在"));
        if (!kb.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("无权访问该知识库");
        }
        return kb;
    }

    @Transactional
    public void delete(Long ownerId, Long id) {
        KnowledgeBase kb = get(ownerId, id);
        knowledgeBaseRepository.delete(kb);
    }
}
