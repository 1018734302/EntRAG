package com.example.rag.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档元数据：记录上传文件、解析/入库状态、分块数。向量内容由 PgVectorStore 管理。
 */
@Entity
@Table(name = "doc_meta")
@Data
public class DocMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    /** PENDING / PARSING / EMBEDDING / DONE / FAILED */
    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(length = 1024)
    private String error;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
