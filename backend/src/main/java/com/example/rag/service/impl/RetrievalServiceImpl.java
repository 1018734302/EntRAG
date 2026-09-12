package com.example.rag.service.impl;

import com.example.rag.config.RagProperties;
import com.example.rag.service.RerankService;
import com.example.rag.service.RetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 检索增强（RAG 中的 Retrieval）：向量相似度召回 + 精排。
 *
 * <p><b>三个设计要点：</b>
 * <ol>
 *   <li><b>先多召回再精简</b>：按 topK 的 2 倍取候选，再做精排，
 *       避免"先截断再排序"漏掉真正更优的片段。</li>
 *   <li><b>强制 kbId 过滤</b>：过滤条件与向量检索在同一条 SQL 内下发给 pgvector，
 *       天然实现多知识库租户隔离，物理上不可能查到其它库的数据。</li>
 *   <li><b>精排可切换、可降级</b>：默认走「{@code 0.7*向量 + 0.3*关键词}」线性融合（零额外开销）；
 *       开启 {@code rag.rerank.enabled} 后改用 Cross-Encoder（Xinference + Qwen3-Reranker）重排，
 *       一旦调用失败会自动降级回线性融合，保证问答链路不中断。</li>
 * </ol>
 */
@Service
public class RetrievalServiceImpl implements RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalServiceImpl.class);

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;
    private final RerankService rerankService;

    public RetrievalServiceImpl(VectorStore vectorStore, RagProperties ragProperties, RerankService rerankService) {
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
        this.rerankService = rerankService;
    }

    @Override
    public List<Document> retrieve(Long kbId, String question, int topK) {
        // ① 召回阶段：故意取 topK 的 2 倍（至少 6 条）作为候选，给后续精排留足选择空间
        // ② kbId 过滤是租户隔离的落地点：过滤条件与向量检索合并为一条 SQL 下发到 pgvector
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        SearchRequest req = SearchRequest.builder()
                .query(question)
                .topK(Math.max(topK * 2, 6))
                .filterExpression(b.eq("kbId", String.valueOf(kbId)).build())
                .build();

        List<Document> candidates = vectorStore.similaritySearch(req);
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // ③ 精排：开启时用 Cross-Encoder 重排；未开启或调用失败则回退到线性融合
        if (ragProperties.getRerank().isEnabled()) {
            try {
                return rerankService.rerank(question, candidates, topK);
            } catch (Exception e) {
                log.warn("Rerank 调用失败，回退到线性融合: {}", e.getMessage());
            }
        }
        return linearFuse(question, candidates, topK);
    }

    /**
     * 回退方案：0.7*向量相似度 + 0.3*关键词命中率 的线性融合。
     *
     * <p>补关键词的原因：纯向量对专有名词、型号、缩写不敏感，字面命中可以纠偏。
     * 相比 Cross-Encoder 它零推理开销，是 Rerank 不可用时的兜底。
     */
    private List<Document> linearFuse(String question, List<Document> candidates, int topK) {
        List<Scored> scored = new ArrayList<>();
        for (Document doc : candidates) {
            // doc.getScore() 由 PgVectorStore 回填（余弦相似度）
            double vectorScore = doc.getScore() == null ? 0d : doc.getScore();
            double keywordScore = keywordScore(question, doc.getText());
            double fused = 0.7 * vectorScore + 0.3 * keywordScore;
            scored.add(new Scored(doc, fused));
        }
        scored.sort(Comparator.comparingDouble((Scored s) -> s.fused).reversed());

        int limit = Math.min(topK, scored.size());
        List<Document> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            result.add(scored.get(i).doc);
        }
        return result;
    }

    /**
     * 关键词命中率：问题按非单词字符切词（长度 ≥2 且去重），统计其在片段文本中的命中比例。
     *
     * @return 0.0 ~ 1.0，即「命中词数 / 去重后的总词数」
     */
    private double keywordScore(String question, String text) {
        if (text == null || text.isBlank()) {
            return 0d;
        }
        String[] terms = question.toLowerCase().split("\\W+");
        java.util.Set<String> queryTerms = new java.util.HashSet<>();
        for (String t : terms) {
            if (t.length() >= 2) {
                queryTerms.add(t);
            }
        }
        if (queryTerms.isEmpty()) {
            return 0d;
        }
        String lower = text.toLowerCase();
        long hit = queryTerms.stream().filter(lower::contains).count();
        return (double) hit / queryTerms.size();
    }

    private static class Scored {
        final Document doc;
        final double fused;

        Scored(Document doc, double fused) {
            this.doc = doc;
            this.fused = fused;
        }
    }
}
