package com.example.rag.service;

import com.example.rag.controller.dto.AuthResponse;
import com.example.rag.controller.dto.LoginRequest;
import com.example.rag.controller.dto.RegisterRequest;
import com.example.rag.model.entity.User;
import com.example.rag.repository.UserRepository;
import com.example.rag.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        return buildToken(saved);
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> opt = userRepository.findByUsername(request.getUsername());
        if (opt.isEmpty() || !passwordEncoder.matches(request.getPassword(), opt.get().getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return buildToken(opt.get());
    }

    private AuthResponse buildToken(User user) {
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        AuthResponse resp = new AuthResponse();
        resp.setToken(token);
        resp.setUsername(user.getUsername());
        resp.setUserId(user.getId());
        return resp;
    }
}
