package com.example.rag.security;

import com.example.rag.config.RagProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析。密钥来自配置，使用 HS256。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(RagProperties ragProperties) {
        this.key = Keys.hmacShaKeyFor(ragProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = ragProperties.getJwt().getExpirationMs();
    }

    public String generateToken(String username, Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
