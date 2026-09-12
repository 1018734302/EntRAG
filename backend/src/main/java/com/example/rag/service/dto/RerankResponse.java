package com.example.rag.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Xinference（及其它 OpenAI 兼容服务）{@code POST /v1/rerank} 的响应结构。
 *
 * <pre>
 * {
 *   "results": [
 *     { "index": 0, "relevance_score": 0.93, "document": "..." }
 *   ]
 * }
 * </pre>
 *
 * <p>{@code index} 对应请求体 {@code documents} 数组的下标，
 * 实现里需要用它把分数映射回原始的 {@code Document} 对象（响应里只回下标，不回原文）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RerankResponse {

    private List<RerankResult> results;

    public List<RerankResult> getResults() {
        return results;
    }

    public void setResults(List<RerankResult> results) {
        this.results = results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RerankResult {

        /** 对应请求 documents 数组的下标 */
        private int index;

        /** 相关性分数，越高越相关 */
        @JsonProperty("relevance_score")
        private double relevanceScore;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public double getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }
    }
}
