# 项目经历 · 可直接写入简历

*智枢 · 企业级 RAG 知识库问答系统*

## 一、项目概述

面向企业私有化场景的 RAG 知识库问答平台：支持上传 PDF / Word / Markdown / TXT 等企业文档，经解析、分块、向量化入库后，以对话方式提供“带引用来源”的流式问答。模型完全本地运行（Ollama qwen3:4b / qwen3-embedding:4b），数据不出域、零 API 成本。本人从 0 到 1 完成后端架构设计与前端全栈交付。

## 二、技术栈

| 分层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 3.4、Spring AI（Ollama ChatModel / EmbeddingModel / PgVectorStore 抽象层） |
| 模型 | 本地 Ollama：qwen3:4b（对话）、qwen3-embedding:4b（嵌入，2560 维） |
| 向量库 | PostgreSQL 16 + pgvector（HNSW 索引，单库部署） |
| 解析 | Apache Tika 2.x |
| 鉴权 | Spring Security + JWT（多知识库租户隔离） |
| 前端 | Vue 3 + Vite + TypeScript + Element Plus + Pinia + fetch-SSE |

## 三、个人职责

- 负责整体技术方案设计：基于 Spring AI 抽象层统一接入本地 Ollama 与向量库，预留云端 DashScope 切换能力。
- 设计异步文档入库管线：Tika 解析 → 固定窗口 + 重叠分块 → 批量向量化，Controller 仅返回 taskId，前端轮询进度，避免大文件阻塞请求。
- 实现 RAG 检索增强：向量相似度 + 关键词融合重排，检索强制按 kbId 租户隔离，杜绝跨库泄露。
- 实现 SSE 流式问答，答案附带引用来源标签，可追溯至具体文档与片段。
- 完成 Vue3 + Element Plus 四页前端（登录 / 知识库 / 文档 / 问答），企业级科技蓝风格。
- 编写 Docker Compose 一键编排（ollama / postgres / backend / frontend）与部署文档。

## 四、架构权衡与亮点（面试可讲）

- 私有化交付：本地推理 + 单库向量，数据不出域，契合企业交付诉求（对比云端方案的合规优势）。
- pgvector 而非 Milvus / Qdrant：单库运维成本最低，HNSW 支撑十万级向量，足够演示体量，体现“够用就好”的工程权衡。
- 轻量融合重排预留升级位：未引入 cross-encoder，用向量 + 关键词线性融合，无额外推理开销，预留 Rerank 接口。
- 能力沉淀：以 Spring AI 抽象层屏蔽模型差异，业务侧代码与具体模型解耦，便于后续多模型 / 多云接入。

## 五、成果与价值

提供可本地一键部署、可演示、可写进简历的全栈 RAG 样例，覆盖“解析—入库—检索—生成”完整链路，体现从业务抽象到架构落地的端到端交付能力。