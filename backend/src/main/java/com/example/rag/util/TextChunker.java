package com.example.rag.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定窗口 + 重叠分块（RAG 入库管线的第一步加工）。
 *
 * <p><b>为什么要 overlap（重叠）？</b>
 * 如果块与块之间严丝合缝地切，当一句话或一个知识点正好落在边界上时，
 * 两边拿到的语义都不完整，会导致该知识点以后召不回来。
 * 保留 64 字符重叠可以显著降低这种“边界损失”。
 *
 * <p><b>权衡：</b>固定窗口实现简单、零依赖；缺点是不认识段落/标题结构。
 * 生产环境可升级为按 Markdown 标题或语义相似度切分。
 */
public class TextChunker {

    /**
     * 按固定窗口滑动切分文本。
     *
     * @param size    单块最大长度（默认 512）
     * @param overlap 相邻块重叠长度（默认 64），因此实际步长为 {@code size - overlap}
     * @return 切分后的文本块列表
     */
    public static List<String> chunk(String text, int size, int overlap) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        // 步长 = 窗口 - 重叠；用 Math.max 兜底，防止 overlap >= size 时步长 ≤0 导致死循环
        int step = Math.max(1, size - overlap);
        int start = 0;
        int len = normalized.length();
        while (start < len) {
            int end = Math.min(len, start + size);
            result.add(normalized.substring(start, end));
            start += step;
        }
        if (result.isEmpty()) {
            result.add(normalized);
        }
        return result;
    }
}
