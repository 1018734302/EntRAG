-- 启用 pgvector 向量扩展（首次执行需数据库有创建扩展权限；docker 镜像默认满足）
CREATE EXTENSION IF NOT EXISTS vector;

-- 说明：User / KnowledgeBase / DocMeta 三张表由 JPA(ddl-auto=update) 自动维护；
-- 向量表 vector_store 由 Spring AI PgVectorStore 启动时自动建表（含 vector 列与 HNSW 索引）。
-- 此处仅确保 vector 扩展存在即可，避免 PgVectorStore 初始化失败。
