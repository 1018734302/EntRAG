# 07 · Rerank 接入与选型（Xinference + Qwen3-Reranker）

## 一、改造前后对比

| | **改造前：线性融合** | **改造后：Cross-Encoder 重排** |
|---|---|---|
| 做法 | `0.7 * 向量相似度 + 0.3 * 关键词命中率` | 把「问题 + 候选片段」拼在一起送进模型，直接输出相关性分数 |
| 匹配类型 | **双塔 / 表示型**：问题和片段各自独立编码 | **交互型**：模型同时看到两者，捕捉细粒度语义 |
| 能否理解语义交互 | ❌ 两个分数算完再加权 | ✅ 能捕捉同义改写、上下位、否定等 |
| 额外开销 | 几乎为零 | 每个候选一次推理（**延迟随候选数线性增长**） |
| 召回失败时的表现 | 专有名词、缩写容易排错 | 明显改善 |

### 为什么线性融合不够？

向量检索（双塔）在编码问题时**看不到候选片段**，编码片段时也看不到问题——两个向量是在"互不交流"的情况下各自算出来的，只能比"整体像不像"。
而 Cross-Encoder 把二者拼成一个输入，让模型在**注意力机制里直接对齐**每个词，精度高一个档次。

关键词分（0.3 权重）只能补一部分：它对字面命中有效，但解决不了"同义不同词"（如"年假"vs"带薪休假"）。

---

## 二、为什么选 Qwen3-Reranker？

| 维度 | 理由 |
|---|---|
| **中文能力** | 阿里 Qwen 系列，中文语料充足，中文语义匹配强 |
| **效果** | 2025 年发布的重排模型，多语言重排榜单第一梯队 |
| **尺寸可选** | 0.6B / 4B / 8B。**本项目选 0.6B**（约 1.2GB），CPU 可跑，延迟可控 |
| **技术栈一致** | 项目对话模型已经是 `qwen3:4b`、嵌入是 `qwen3-embedding:4b`，同系列便于统一维护与讲解 |
| **支持指令** | 可通过 instruction 定制"相关性"判定标准，便于业务微调 |

> 若机器没有大显存，不要上 4B/8B——0.6B 在重排这种判别式任务上性价比最高。

---

## 三、为什么用 Xinference 承载，而不是别的？

### 关键前提：Ollama 没有 rerank 端点

Ollama 只提供 `/api/generate`、`/api/chat`、`/api/embeddings`、`/api/embed`，**没有 rerank**。
所以本地私有化跑 Cross-Encoder，必须额外一个推理服务。

| 方案 | 优势 | 劣势 | 结论 |
|---|---|---|---|
| **Xinference** ✅ | 内置模型目录（含 Qwen3-Reranker 全系列）、一行启动、自带 Web 控制台、中文生态好、支持 ModelScope 加速 | 镜像较大（数 GB） | **选用** |
| TEI（HuggingFace） | 轻量、性能好 | 对 Qwen3-Reranker 支持不如 Xinference 直接；中文生态弱 | 备选 |
| vLLM | 吞吐高 | 重，主要为 LLM 设计；rerank 支持较新 | 备选 |
| Java 内嵌 ONNX | 零额外服务 | 模型管理麻烦、占 JVM 内存、依赖变重 | 备选 |
| 云端 API（DashScope/Cohere） | 接入最快 | **违背本项目"数据不出域"的定位** | 仅做效果对比 |

> **选型与项目定位一致性很重要**：这个项目卖点是"私有化交付、数据不出域"，
> 用云端 Rerank 会自相矛盾。所以即便云端接入更简单，也不作为最终方案。

---

## 四、架构设计：可切换 + 可降级

```
召回（向量，topK*2）
        │
        ├─ rerank.enabled = true  →  Xinference /v1/rerank（Cross-Encoder）
        │                              └─ 失败/超时 → 自动降级 ↓
        └─ rerank.enabled = false ──────────────→ 线性融合（0.7/0.3）
```

**为什么要降级？** Rerank 是外部服务，可能没启动、超时或返回异常。
若直接抛错，整个问答就不可用了。降级后仍能返回"次优但可用"的结果，保证链路可用性——
这是生产级设计与玩具 demo 的区别。

关键代码：`RetrievalServiceImpl#retrieve`

```java
if (ragProperties.getRerank().isEnabled()) {
    try {
        return rerankService.rerank(question, candidates, topK);
    } catch (Exception e) {
        log.warn("Rerank 调用失败，回退到线性融合: {}", e.getMessage());
    }
}
return linearFuse(question, candidates, topK);
```

`RerankService` 是接口，后续换 TEI / vLLM / 云端，只需换实现类，**检索逻辑不用动**。

---

## 五、代价与正确使用姿势（重要）

> **Cross-Encoder 对每个 (问题, 候选) 都要跑一次推理。**

本项目召回 2 倍（约 10 条）= **10 次推理**，CPU 上通常增加 **300ms ~ 1s**。

因此必须遵守：

1. **只对召回后的小候选集重排**（本项目 10 条），**绝不对全库做**
2. 保留开关，效果/延迟不达预期时一键关闭
3. 设置合理超时（`timeout-ms`），避免拖垮问答接口

> 面试话术：知道 Rerank 好，也知道它贵，并且知道"只在小候选集上用"这个业界标准做法。

---

## 六、如何启用

### 1）启动服务

```bash
docker compose up -d xinference
```

### 2）拉起模型（二选一）

**Web 控制台**：http://localhost:9997 → `Launch Model` → **切到 RERANK 分类页** → 选 `Qwen3-Reranker-0.6B` → 启动

**命令行**：

```bash
docker exec -it <xinference容器名> xinference launch ^
  --model-name "Qwen3-Reranker-0.6B" ^
  --model-type rerank
```

> ⚠️ 常见坑：Web 控制台默认是 LLM 分类，需要**手动切到 RERANK** 才能看到重排模型。

### 3）验证接口

```bash
curl -X POST http://localhost:9997/v1/rerank ^
  -H "Content-Type: application/json" ^
  -d "{\"model\":\"Qwen3-Reranker-0.6B\",\"query\":\"年假有几天\",\"documents\":[\"年假15天\",\"报销流程\"]}"
```

返回 `results[].relevance_score` 即成功。

### 4）开启开关

`application.yml`：

```yaml
rag:
  rerank:
    enabled: true
```

或用环境变量（无需改文件）：

```bash
set RAG_RERANK_ENABLED=true
```

> 注：当前 `enabled` 显式写在 yml 里为 `false`。若要支持环境变量开关，可改为
> `enabled: "${RAG_RERANK_ENABLED:false}"`。

---

## 七、验证方式

1. `enabled: false` 问一个问题，记录召回片段顺序
2. `enabled: true` 问同样问题，对比片段顺序是否更合理
3. 观察日志：
   - 出现 `Rerank 调用失败，回退到线性融合` → 服务没通或模型未加载
   - 无异常且排序变化 → 生效
4. 故意停掉 Xinference 容器，确认问答**仍能返回结果**（验证降级）

---

## 八、总结：一句话选型逻辑

> 原来用「向量 + 关键词」线性融合，是因为它零成本、零依赖，适合先把链路跑通；
> 但它本质是两个独立分数的加权，模型从未真正"一起看过"问题和片段，
> 对同义改写、专有名词的排序不够准。
>
> 所以引入 Cross-Encoder 做重排：**用它换精度，用"只在小候选集上跑 + 开关降级"控制成本与风险**。
> 模型选 Qwen3-Reranker-0.6B（中文强、轻量、与现有 qwen3 技术栈一致），
> 承载选 Xinference（Ollama 无 rerank 端点，Xinference 内置模型目录、中文生态好、支持 ModelScope 加速），
> 全程本地部署，不破坏"数据不出域"的定位。
