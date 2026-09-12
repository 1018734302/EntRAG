package com.example.rag.controller.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String username;
    private Long userId;
}
