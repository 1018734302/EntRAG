package com.example.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 绑定 application.yml 中 rag.* 前缀的业务配置，便于演示时统一调参。
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
@Data
public class RagProperties {

    private Chat chat = new Chat();
    private Embedding embedding = new Embedding();
    private Chunk chunk = new Chunk();
    private Jwt jwt = new Jwt();
    private Rerank rerank = new Rerank();
    private String corsAllowedOrigins;

    @Data
    public static class Chat {
        private String model = "qwen3:4b";
        private int topK = 5;
        private double temperature = 0.2;
    }

    @Data
    public static class Embedding {
        private int dim = 1024;
    }

    @Data
    public static class Chunk {
        private int size = 512;
        private int overlap = 64;
    }

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000L;
    }

    /**
     * 重排（Rerank / Cross-Encoder）配置。
     */
    @Data
    public static class Rerank {
        /**
         * 是否启用。关闭时自动回退到「0.7 向量 + 0.3 关键词」线性融合，
         * 保证 Rerank 服务不可用时问答链路仍然可用。
         */
        private boolean enabled = false;
        /** Xinference 服务地址（Xinference 默认端口 9997） */
        private String baseUrl = "http://localhost:9997";
        /** Xinference 中注册的模型名，如 Qwen3-Reranker-0.6B / bge-reranker-v2-m3 */
        private String model = "Qwen3-Reranker-0.6B";
        /** 相关性低于该阈值的候选直接丢弃（0 表示不丢弃） */
        private double threshold = 0.0;
        /** 调用超时（毫秒），超时后降级为线性融合 */
        private int timeoutMs = 8000;
    }
}
