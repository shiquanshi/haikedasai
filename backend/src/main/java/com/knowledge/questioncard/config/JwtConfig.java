package com.knowledge.questioncard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * JWT密钥
     */
    private String secret = "your-secret-key-change-this-in-production-at-least-256-bits";
    
    /**
     * JWT过期时间（毫秒），默认7天
     */
    private Long expiration = 7 * 24 * 60 * 60 * 1000L;
    
    /**
     * JWT刷新时间（毫秒），默认3天
     */
    private Long refreshExpiration = 3 * 24 * 60 * 60 * 1000L;
    
    /**
     * Token前缀
     */
    private String tokenPrefix = "Bearer ";
    
    /**
     * Token请求头名称
     */
    private String headerName = "Authorization";
}