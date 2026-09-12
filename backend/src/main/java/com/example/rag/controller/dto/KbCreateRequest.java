package com.example.rag.controller.dto;

import lombok.Data;

@Data
public class KbCreateRequest {
    private String name;
    private String description;
    private String embeddingModel;
}
