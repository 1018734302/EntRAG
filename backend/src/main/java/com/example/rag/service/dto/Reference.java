package com.example.rag.service.dto;

import lombok.Data;

/**
 * 答案引用来源：指向具体文档与片段，便于前端展示「引用标签」。
 */
@Data
public class Reference {
    private int index;
    private String fileName;
    private int chunkIndex;
    private String snippet;
}
