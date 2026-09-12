package com.example.rag.config;

import com.example.rag.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 安全配置：JWT 无状态鉴权 + 放行登录/注册/健康检查，其余接口需携带令牌。
 *
 * <p><b>两个容易踩的坑（本项目都已修复）：</b>
 * <ol>
 *   <li><b>CORS 不能用 allowedOrigins("*") 配 allowCredentials(true)</b>：
 *       Spring 会直接抛 {@code IllegalArgumentException}，导致
 *       <b>带 Origin 头的请求全部 500</b>。浏览器的同源 POST 必带 Origin，
 *       所以典型表现是"命令行调用正常、浏览器一直失败"。
 *       此处改用 {@code setAllowedOriginPatterns}。</li>
 *   <li><b>SSE 的异步分发会被二次鉴权</b>：SseEmitter 属于 ASYNC 分发，
 *       若对 ASYNC 也鉴权，会因 SecurityContext 未传播而抛 Access Denied。
 *       此处用 {@code shouldFilterAllDispatcherTypes(false)}，只对 REQUEST 分发鉴权。</li>
 * </ol>
 */
@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final RagProperties ragProperties;

    public SecurityConfig(JwtFilter jwtFilter, RagProperties ragProperties) {
        this.jwtFilter = jwtFilter;
        this.ragProperties = ragProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // SSE(SseEmitter) 是异步分发，若对 ASYNC 分发二次鉴权会因 SecurityContext
                        // 未传播而抛 Access Denied，这里只对 REQUEST 分发鉴权。
                        .shouldFilterAllDispatcherTypes(false)
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS 配置。
     *
     * <p>必须用 {@code setAllowedOriginPatterns} 而非 {@code setAllowedOrigins}：
     * 后者与 {@code allowCredentials(true)} 同时出现时，Spring 认为 {@code "*"}
     * 不能作为 {@code Access-Control-Allow-Origin} 的值而抛异常，
     * 结果是所有带 Origin 的请求 500。
     *
     * <p>配置值来自环境变量 {@code RAG_CORS_ALLOWED_ORIGINS}，<b>不要设为 *</b>，
     * 应显式列出前端地址（如 http://localhost:5173）。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(ragProperties.getCorsAllowedOrigins().split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
