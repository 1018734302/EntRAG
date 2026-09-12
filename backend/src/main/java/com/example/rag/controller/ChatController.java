package com.example.rag.controller;

import com.example.rag.config.RagProperties;
import com.example.rag.model.entity.DocMeta;
import com.example.rag.security.SecurityUtil;
import com.example.rag.service.KnowledgeBaseService;
import com.example.rag.service.RetrievalService;
import com.example.rag.service.ChatService;
import com.example.rag.service.dto.Reference;
import com.example.rag.controller.dto.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流式问答接口（RAG 在线链路的入口）。
 *
 * <p>一次问答推送的 SSE 事件序列：
 * <pre>
 * event:references → 本次引用了哪些文档的哪些片段（最先推）
 * data:"token"     → 每个 token 一条（默认事件名），逐字推送
 * event:done       → [DONE]，生成结束
 * </pre>
 *
 * <p><b>为什么先把 references 推给前端？</b>
 * 检索是在生成之前完成的，先把引用推出去，用户不用干等就能看到"这次参考了哪几篇文档"，
 * 既提升等待体验，也让答案的来源可追溯。
 */
@RestController
@RequestMapping("/kb")
public class ChatController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final RetrievalService retrievalService;
    private final ChatService chatService;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatController(KnowledgeBaseService knowledgeBaseService, RetrievalService retrievalService,
                          ChatService chatService, RagProperties ragProperties) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.retrievalService = retrievalService;
        this.chatService = chatService;
        this.ragProperties = ragProperties;
    }

    @GetMapping(value = "/{kbId}/chat", produces = "text/event-stream")
    public SseEmitter chat(@PathVariable Long kbId, @RequestParam String question) {
        // 0L = 不设置超时，避免长答案生成到一半被容器判定超时而断开
        SseEmitter emitter = new SseEmitter(0L);
        Long uid = SecurityUtil.getCurrentUserId();
        knowledgeBaseService.get(uid, kbId);   // 校验知识库归属，未授权会抛异常

        // ① 检索（同步执行）：内部已强制按 kbId 过滤，并做过融合重排
        List<Document> docs = retrievalService.retrieve(kbId, question, ragProperties.getChat().getTopK());
        List<Reference> references = toReferences(docs);

        // ② 先把引用推给前端（此时尚未开始生成）
        try {
            emitter.send(SseEmitter.event().name("references").data(objectMapper.writeValueAsString(references)));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // ③ 订阅生成流：Flux<String> 每发射一个 token 就推一条 SSE，实现真正的流式
        chatService.streamAnswer(kbId, question, docs)
                .subscribe(
                        token -> {
                            try {
                                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(token)));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        error -> emitter.completeWithError(error),
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            } catch (IOException ignored) {
                            }
                            emitter.complete();
                        }
                );
        return emitter;
    }

    /**
     * 把检索到的 Document 转成前端可展示的引用（文件名 + 块号 + 摘要）。
     * 摘要截断到 120 字符，避免引用块过大挤占答案区域。
     */
    private List<Reference> toReferences(List<Document> docs) {
        return docs.stream().map(d -> {
            Reference r = new Reference();
            r.setFileName(String.valueOf(d.getMetadata().getOrDefault("fileName", "未知")));
            Object idx = d.getMetadata().get("chunkIndex");
            r.setChunkIndex(idx == null ? 0 : Integer.parseInt(idx.toString()));
            String text = d.getText() == null ? "" : d.getText();
            r.setSnippet(text.length() > 120 ? text.substring(0, 120) + "…" : text);
            return r;
        }).collect(Collectors.toList());
    }
}
