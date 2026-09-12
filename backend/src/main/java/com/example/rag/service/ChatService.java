package com.example.rag.service;

import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    /**
     * 基于检索上下文流式生成答案（SSE 逐 token）。
     * @param kbId    知识库 ID
     * @param question 用户问题
     * @param context 已检索并重排的文档片段
     */
    Flux<String> streamAnswer(Long kbId, String question, List<Document> context);
}
