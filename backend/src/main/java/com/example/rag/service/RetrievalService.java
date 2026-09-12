package com.example.rag.service;

import org.springframework.ai.document.Document;

import java.util.List;

public interface RetrievalService {

    /**
     * 向量检索 + 关键词融合重排 + 租户隔离。
     * @param kbId     知识库 ID（强制过滤，防止跨库泄露）
     * @param question 用户问题
     * @param topK     返回片段数
     */
    List<Document> retrieve(Long kbId, String question, int topK);
}
