package com.example.rag.service.impl;

import com.example.rag.config.RagProperties;
import com.example.rag.service.RerankService;
import com.example.rag.service.dto.RerankResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 Xinference 的 Rerank 实现（Qwen3-Reranker / bge-reranker-v2-m3 等）。
 *
 * <p>Xinference 暴露 OpenAI 兼容的 {@code POST /v1/rerank} 端点：
 * <ul>
 *   <li>请求：{@code {model, query, documents, top_n}}</li>
 *   <li>响应：{@code {results: [{index, relevance_score}]}}</li>
 * </ul>
 *
 * <p><b>为什么用 Xinference 承载？</b>
 * Ollama 只提供 generate / chat / embeddings，<b>没有 rerank 端点</b>，
 * 因此本地私有化跑 Cross-Encoder 需要额外一个推理服务，Xinference 是中文生态里最省事的选择
 * （内置模型目录、一行启动、自带 Web 控制台）。
 */
@Service
public class XinferenceRerankServiceImpl implements RerankService {

    private static final Logger log = LoggerFactory.getLogger(XinferenceRerankServiceImpl.class);

    private final RagProperties ragProperties;
    private final RestClient restClient;

    public XinferenceRerankServiceImpl(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
        // 独立设置超时：Rerank 是同步阻塞调用，不能让它拖垮问答接口
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(ragProperties.getRerank().getTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(ragProperties.getRerank().getTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(ragProperties.getRerank().getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public List<Document> rerank(String query, List<Document> candidates, int topN) {
        if (candidates == null || candidates.isEmpty() || topN <= 0) {
            return List.of();
        }

        // ① 只把片段正文送给模型打分（不出域的前提下，正文仍需离开应用进程，私有化部署时需注意）
        List<String> documents = candidates.stream()
                .map(d -> d.getText() == null ? "" : d.getText())
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("model", ragProperties.getRerank().getModel());
        body.put("query", query);
        body.put("documents", documents);
        body.put("top_n", Math.min(topN, documents.size()));

        // ② 调用 Xinference 的 /v1/rerank
        RerankResponse resp = restClient.post()
                .uri("/v1/rerank")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RerankResponse.class);

        if (resp == null || resp.getResults() == null || resp.getResults().isEmpty()) {
            log.warn("Rerank 返回为空，回退到候选原顺序");
            return truncate(candidates, topN);
        }

        // ③ 过滤 + 按分数降序，并把 index 映射回原始 Document
        double threshold = ragProperties.getRerank().getThreshold();
        List<RerankResponse.RerankResult> ranked = resp.getResults().stream()
                .filter(r -> r.getIndex() >= 0 && r.getIndex() < candidates.size())
                .filter(r -> r.getRelevanceScore() >= threshold)
                .sorted(Comparator.comparingDouble(RerankResponse.RerankResult::getRelevanceScore).reversed())
                .collect(Collectors.toList());

        if (ranked.isEmpty()) {
            log.warn("Rerank 结果均低于阈值 {}，回退到候选原顺序", threshold);
            return truncate(candidates, topN);
        }

        List<Document> result = new ArrayList<>(ranked.size());
        for (RerankResponse.RerankResult r : ranked) {
            result.add(candidates.get(r.getIndex()));
            if (result.size() >= topN) {
                break;
            }
        }
        return result;
    }

    private List<Document> truncate(List<Document> docs, int topN) {
        return docs.subList(0, Math.min(topN, docs.size()));
    }
}
