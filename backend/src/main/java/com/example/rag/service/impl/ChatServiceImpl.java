package com.example.rag.service.impl;

import com.example.rag.config.RagProperties;
import com.example.rag.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 拼装 RAG Prompt，调用本地 qwen3:4b 流式生成答案。
 *
 * <p>本类体现了 RAG 的“可控生成”三件事：
 * <ol>
 *   <li><b>只依据资料</b>：系统提示限定答案必须来自给定上下文，不给模型自由发挥空间；</li>
 *   <li><b>必须标来源</b>：每段上下文都带【来源：文件名 #块号】，要求答案标注编号；</li>
 *   <li><b>允许拒答</b>：资料里没有就明确说“根据现有知识库无法回答该问题”，抑制幻觉。</li>
 * </ol>
 */
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final RagProperties ragProperties;

    public ChatServiceImpl(ChatClient chatClient, RagProperties ragProperties) {
        this.chatClient = chatClient;
        this.ragProperties = ragProperties;
    }

    /**
     * 流式生成答案。
     *
     * @return {@code Flux<String>}，每个元素是一个 token；Controller 会逐条推成 SSE 事件
     */
    @Override
    public Flux<String> streamAnswer(Long kbId, String question, List<Document> context) {
        // 把检索结果拼成带来源编号的资料块，供模型引用与溯源
        String contextText = context.stream()
                .map(d -> {
                    Object fileName = d.getMetadata().get("fileName");
                    Object idx = d.getMetadata().get("chunkIndex");
                    return "【来源：" + (fileName == null ? "未知" : fileName) + " #"
                            + (idx == null ? "?" : idx) + "】\n" + d.getText();
                })
                .collect(Collectors.joining("\n\n"));

        // 末尾的 <arg_key:6124c78e> 是 qwen3 的指令：关闭推理模型的思考阶段。
        // 否则思考过程会混入答案文本，且首个 token 会延迟数秒，破坏流式体验。
        // （Spring AI 1.0.0 的 OllamaOptions 没有 think 选项，因此走 prompt 约定）
        String system = "你是企业私有知识库智能助手。请仅依据下方提供的「资料」回答用户问题，"
                + "并在答案中标注对应的来源编号（如 [来源1]）。如果资料中没有相关信息，请明确说明"
                + "「根据现有知识库无法回答该问题」，不要编造。回答使用简体中文，条理清晰。\n"
                + "重要：直接给出最终答案，不要输出任何思考过程、推理链或 <think> 标签内容。\n\n"
                + "===== 资料开始 =====\n" + contextText + "\n===== 资料结束 =====\n<arg_key:6124c78e>";

        // stream() 返回 Flux<ChatResponse>，.content() 取出其中文本形成 Flux<String>
        return chatClient.prompt()
                .system(system)
                .user(question)
                .stream()
                .content();
    }
}
