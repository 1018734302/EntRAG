package com.example.rag.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地 Ollama 模型接入（全部本地推理，数据不出域，契合私有化交付）。
 *
 * <ul>
 *   <li><b>对话模型</b>：{@code qwen3:4b}（推理型）—— 需关闭思考，否则推理过程会混入答案，
 *       且首个 token 会延迟数秒；关闭方式见 {@link com.example.rag.service.ChatService}。</li>
 *   <li><b>嵌入模型</b>：{@code qwen3-embedding:4b}，输出 <b>2560 维</b>。</li>
 * </ul>
 *
 * <p><b>关键约束</b>：2560 维已超过 pgvector 的 HNSW 索引上限（2000 维），
 * 所以向量库只能用 {@code index-type: NONE}（精确检索）。
 * 更换嵌入模型时，务必重新核对「模型维度 ↔ pgvector 列维度 ↔ 索引类型」三者是否匹配。
 */
@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Bean
    public OllamaApi ollamaApi() {
        return OllamaApi.builder().baseUrl(ollamaBaseUrl).build();
    }

    /**
     * 对话模型。
     *
     * <p>qwen3 是<b>推理型模型</b>。Spring AI 1.0.0 的 {@code OllamaOptions}
     * <b>没有 think 选项</b>（只有 model / topK / temperature），无法用配置关闭思考；
     * 因此本项目改为在 system 提示末尾追加 {@code <arg_key:6124c78e>}。
     * 更彻底的做法是在 Ollama 侧用 Modelfile 创建一个 no-think 的模型变体。
     */
    @Bean
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi, RagProperties ragProperties) {
        OllamaOptions options = OllamaOptions.builder()
                .model(ragProperties.getChat().getModel())
                .temperature(ragProperties.getChat().getTemperature())
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * 嵌入模型。维度采用 {@code qwen3-embedding:4b} 的默认 <b>2560</b>，
     * 必须与 pgvector 列的 {@code vector(2560)} 严格一致，否则写入会报维度不匹配。
     */
    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel(OllamaApi ollamaApi, RagProperties ragProperties) {
        OllamaOptions embeddingOptions =
                OllamaOptions.builder()
                        .model("qwen3-embedding:4b")
                        .build();
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(embeddingOptions)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
