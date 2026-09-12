package com.example.rag.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 重排（Rerank / Cross-Encoder）抽象。
 *
 * <p><b>它解决什么问题？</b>
 * 召回阶段（向量相似度）只分别看了「问题」和「片段」各自的向量，属于<b>双塔/表示型</b>匹配，
 * 无法捕捉二者之间的细粒度语义交互。Rerank 用 <b>Cross-Encoder</b> 把
 * 「问题 + 候选片段」<b>拼接后一起</b>送进模型，直接输出相关性分数，精度显著更高。
 *
 * <p><b>代价</b>：每个候选都要跑一次推理，延迟随候选数线性增长。
 * 所以业界标准用法是——只对召回后的<b>小候选集</b>（本项目为 topK*2，约 10 条）做重排，
 * 绝不对全库做。
 */
public interface RerankService {

    /**
     * 对召回候选做重排。
     *
     * @param query      用户问题
     * @param candidates 召回阶段得到的候选（已按 kbId 过滤，保证租户隔离）
     * @param topN       需要返回的条数
     * @return 按相关性从高到低排序的片段，最多 topN 条
     */
    List<Document> rerank(String query, List<Document> candidates, int topN);
}
