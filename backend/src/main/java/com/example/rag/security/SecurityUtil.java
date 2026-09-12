package com.example.rag.security;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 读取当前登录用户。JWT 过滤器将 uid 写入 principal。
 */
public class SecurityUtil {

    public static Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long uid) {
            return uid;
        }
        if (principal instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalStateException("未登录或令牌无效");
    }
}
