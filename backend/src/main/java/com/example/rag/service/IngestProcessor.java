package com.example.rag.service;

import com.example.rag.config.RagProperties;
import com.example.rag.model.entity.DocMeta;
import com.example.rag.repository.DocMetaRepository;
import com.example.rag.service.dto.IngestStatus;
import com.example.rag.util.TextChunker;
import com.example.rag.util.TikaParser;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 异步入库处理（RAG 离线链路的核心）：解析 → 分块 → 批量向量化 → 写入 pgvector。
 *
 * <p><b>为什么必须异步？</b>
 * 解析与向量化可能耗时几十秒。若放在请求线程里同步执行，大文件会把 Tomcat 线程池打满，
 * 前端请求也会超时。因此 Controller 只返回 taskId，真正的处理交给本类的 {@code @Async} 方法，
 * 前端通过轮询 {@code /kb/ingest/status/{taskId}} 观察进度。
 *
 * <p>状态流转：{@code PENDING → PARSING → EMBEDDING → DONE / FAILED}
 */
@Component
public class IngestProcessor {

    private final RagProperties ragProperties;
    private final DocMetaRepository docMetaRepository;
    private final VectorStore vectorStore;
    private final IngestTaskRegistry registry;

    public IngestProcessor(RagProperties ragProperties, DocMetaRepository docMetaRepository,
                           VectorStore vectorStore, IngestTaskRegistry registry) {
        this.ragProperties = ragProperties;
        this.docMetaRepository = docMetaRepository;
        this.vectorStore = vectorStore;
        this.registry = registry;
    }

    @Async
    public void processAsync(String taskId, Long kbId, Long docId, String fileName, byte[] content) {
        IngestStatus status = registry.get(taskId);
        try {
            // ① PARSING：Tika 把 PDF/Word/Markdown/TXT 统一解析为纯文本
            registry.update(taskId, s -> { s.setStatus("PARSING"); s.setFileName(fileName); });
            docMetaRepository.findById(docId).ifPresent(d -> { d.setStatus("PARSING"); docMetaRepository.save(d); });

            String text = TikaParser.parseBytes(content, fileName);

            // ② 分块：固定窗口 + 重叠，避免知识点被切在边界上导致以后召回不到
            List<String> chunks = TextChunker.chunk(text, ragProperties.getChunk().getSize(),
                    ragProperties.getChunk().getOverlap());
            registry.update(taskId, s -> { s.setStatus("EMBEDDING"); s.setTotalChunks(chunks.size()); s.setEmbeddedChunks(0); });

            // ③ 构造 Document：元数据带 kbId（租户隔离）、fileName/chunkIndex（答案溯源）
            List<Document> documents = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                Map<String, Object> metadata = Map.of(
                        "kbId", String.valueOf(kbId),
                        "docId", String.valueOf(docId),
                        "fileName", fileName == null ? "" : fileName,
                        "chunkIndex", String.valueOf(i),
                        "text", chunk
                );
                documents.add(new Document(buildChunkId(kbId, docId, i), chunk, metadata));
            }
            // ④ 向量化并写入：PgVectorStore 内部按 batch_size 批量调用 EmbeddingModel
            //    （本例为本地 qwen3-embedding:4b，输出 2560 维）
            vectorStore.add(documents);

            // ⑤ 收尾：记录分块数并置为 DONE
            docMetaRepository.findById(docId).ifPresent(d -> {
                d.setStatus("DONE");
                d.setChunkCount(chunks.size());
                d.setUpdatedAt(java.time.LocalDateTime.now());
                docMetaRepository.save(d);
            });
            registry.update(taskId, s -> { s.setStatus("DONE"); s.setEmbeddedChunks(chunks.size()); });
        } catch (Exception e) {
            // 排障提示：这里只保留了顶层的 getMessage()（Spring 的异常通常只有 SQL 文本）。
            // 若要定位下层真实原因（例如字段类型不匹配），请查数据库服务端日志：
            //   docker logs entrag-postgres-1 --tail 50
            String msg = e.getMessage();
            registry.update(taskId, s -> { s.setStatus("FAILED"); s.setMessage(msg); });
            docMetaRepository.findById(docId).ifPresent(d -> {
                d.setStatus("FAILED");
                d.setError(msg);
                d.setUpdatedAt(java.time.LocalDateTime.now());
                docMetaRepository.save(d);
            });
        }
    }

    /**
     * 生成分块的向量 ID。
     *
     * <p><b>为什么必须是 UUID 形式？</b>
     * PgVectorStore 默认 idType 是 UUID，写入时会执行 {@code UUID.fromString(id)}。
     * 早期这里用的是 {@code "kb1-doc1-c0"} 这类字符串，会直接抛
     * {@code Invalid UUID string} 导致入库失败。
     *
     * <p><b>为什么用 nameUUIDFromBytes 而不是随机 UUID？</b>
     * 为了"确定性"：删除文档时要用同样的入参重建出同样的 ID，才能准确删掉对应向量。
     */
    private String buildChunkId(Long kbId, Long docId, int index) {
        String raw = "kb" + kbId + "-doc" + docId + "-c" + index;
        return UUID.nameUUIDFromBytes(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }
}
