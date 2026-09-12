package com.example.rag.controller.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private String sessionId;
}
