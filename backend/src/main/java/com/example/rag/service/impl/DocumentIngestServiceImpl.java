package com.example.rag.service.impl;

import com.example.rag.model.entity.DocMeta;
import com.example.rag.repository.DocMetaRepository;
import com.example.rag.security.SecurityUtil;
import com.example.rag.service.DocumentIngestService;
import com.example.rag.service.IngestProcessor;
import com.example.rag.service.IngestTaskRegistry;
import com.example.rag.service.KnowledgeBaseService;
import com.example.rag.service.dto.IngestStatus;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentIngestServiceImpl implements DocumentIngestService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocMetaRepository docMetaRepository;
    private final IngestProcessor ingestProcessor;
    private final IngestTaskRegistry registry;
    private final VectorStore vectorStore;

    public DocumentIngestServiceImpl(KnowledgeBaseService knowledgeBaseService,
                                     DocMetaRepository docMetaRepository,
                                     IngestProcessor ingestProcessor,
                                     IngestTaskRegistry registry,
                                     VectorStore vectorStore) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.docMetaRepository = docMetaRepository;
        this.ingestProcessor = ingestProcessor;
        this.registry = registry;
        this.vectorStore = vectorStore;
    }

    /**
     * 提交入库任务。
     *
     * <p><b>为什么立即返回 taskId，而不是等处理完？</b>
     * 解析加向量化可能耗时几十秒，同步执行会占满请求线程并让前端超时。
     * 因此这里只做「落 doc_meta + 注册任务 + 投递异步任务」三件事后立刻返回，
     * 前端再轮询 {@link #queryStatus(String)} 查看进度。
     *
     * @return taskId，用于轮询入库进度
     */
    @Override
    public String ingest(Long kbId, MultipartFile file) {
        Long uid = SecurityUtil.getCurrentUserId();
        knowledgeBaseService.get(uid, kbId); // 校验归属，防止越权上传到别人的知识库

        DocMeta docMeta = new DocMeta();
        docMeta.setKbId(kbId);
        docMeta.setFileName(file.getOriginalFilename());
        docMeta.setFileSize(file.getSize());
        docMeta.setContentType(file.getContentType());
        docMeta.setStatus("PENDING");
        DocMeta saved = docMetaRepository.save(docMeta);

        String taskId = UUID.randomUUID().toString();
        IngestStatus status = new IngestStatus();
        status.setTaskId(taskId);
        status.setKbId(kbId);
        status.setDocId(saved.getId());
        status.setFileName(saved.getFileName());
        status.setStatus("PENDING");
        status.setTotalChunks(0);
        status.setEmbeddedChunks(0);
        registry.put(status);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("读取上传文件失败：" + e.getMessage());
        }
        ingestProcessor.processAsync(taskId, kbId, saved.getId(), saved.getFileName(), bytes);
        return taskId;
    }

    @Override
    public IngestStatus queryStatus(String taskId) {
        return registry.get(taskId);
    }

    @Override
    public List<DocMeta> listDocs(Long kbId) {
        Long uid = SecurityUtil.getCurrentUserId();
        knowledgeBaseService.get(uid, kbId);
        return docMetaRepository.findByKbId(kbId);
    }

    @Override
    public DocMeta getDoc(Long kbId, Long docId) {
        Long uid = SecurityUtil.getCurrentUserId();
        knowledgeBaseService.get(uid, kbId);
        DocMeta docMeta = docMetaRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!docMeta.getKbId().equals(kbId)) {
            throw new IllegalArgumentException("文档不属于该知识库");
        }
        return docMeta;
    }

    @Override
    @Transactional
    public void deleteDocument(Long kbId, Long docId) {
        Long uid = SecurityUtil.getCurrentUserId();
        knowledgeBaseService.get(uid, kbId);
        DocMeta docMeta = docMetaRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!docMeta.getKbId().equals(kbId)) {
            throw new IllegalArgumentException("文档不属于该知识库");
        }
        // 按入库时同样的规则重建分块 ID，才能准确删掉对应向量。
        // 这里必须与 IngestProcessor#buildChunkId 保持完全一致（同样的确定性 UUID 算法）。
        int count = docMeta.getChunkCount() == null ? 0 : docMeta.getChunkCount();
        List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(UUID.nameUUIDFromBytes(("kb" + kbId + "-doc" + docId + "-c" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
        }
        if (!ids.isEmpty()) {
            // 先删向量，再删元数据记录
            vectorStore.delete(ids);
        }
        docMetaRepository.delete(docMeta);
    }

    @Override
    public List<String> previewChunks(Long kbId, Long docId, int limit) {
        Long uid = SecurityUtil.getCurrentUserId();
        knowledgeBaseService.get(uid, kbId);
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        SearchRequest req = SearchRequest.builder()
                .query("文档内容")
                .topK(Math.max(limit, 1))
                .filterExpression(b.eq("docId", String.valueOf(docId)).build())
                .build();
        return vectorStore.similaritySearch(req).stream()
                .sorted((a, b2) -> {
                    Integer i1 = parseIndex(a.getMetadata().get("chunkIndex"));
                    Integer i2 = parseIndex(b2.getMetadata().get("chunkIndex"));
                    return Integer.compare(i1, i2);
                })
                .map(d -> (String) d.getMetadata().get("text"))
                .collect(Collectors.toList());
    }

    private Integer parseIndex(Object v) {
        if (v == null) return 0;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
