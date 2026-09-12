package com.example.rag.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_base")
@Data
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 嵌入模型标识，默认 qwen3-embedding:4b */
    @Column(name = "embedding_model", length = 64)
    private String embeddingModel = "qwen3-embedding:4b";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
