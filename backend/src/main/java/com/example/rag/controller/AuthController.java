package com.example.rag.controller;

import com.example.rag.common.Result;
import com.example.rag.controller.dto.AuthResponse;
import com.example.rag.controller.dto.LoginRequest;
import com.example.rag.controller.dto.RegisterRequest;
import com.example.rag.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }
}
