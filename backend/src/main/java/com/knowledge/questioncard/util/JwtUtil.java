package com.knowledge.questioncard.util;

import com.knowledge.questioncard.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    
    private final JwtConfig jwtConfig;
    
    /**
     * 生成JWT Token
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration());
        
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
    
    /**
     * 从Token中获取Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT token", e);
            return null;
        }
    }
    
    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims != null) {
                Object userId = claims.get("userId");
                log.info("[JWT调试] 从token提取userId: value={}, type={}", userId, userId != null ? userId.getClass().getName() : "null");
                
                if (userId == null) {
                    log.warn("[JWT调试] token中不存在userId字段 - token前20位: {}", 
                            token.substring(0, Math.min(20, token.length())));
                    log.warn("[JWT调试] token中的所有claims: {}", claims);
                    return null;
                }
                
                if (userId instanceof Integer) {
                    Long result = ((Integer) userId).longValue();
                    log.info("[JWT调试] userId(Integer)转换成功: {}", result);
                    return result;
                } else if (userId instanceof Long) {
                    log.info("[JWT调试] userId(Long)获取成功: {}", userId);
                    return (Long) userId;
                } else if (userId instanceof String) {
                    try {
                        Long result = Long.parseLong((String) userId);
                        log.info("[JWT调试] userId(String)解析成功: {}", result);
                        return result;
                    } catch (NumberFormatException e) {
                        log.warn("[JWT调试] userId(String)解析失败: {}", userId);
                        return null;
                    }
                } else {
                    log.warn("[JWT调试] userId类型不支持: {}, 值: {}", userId.getClass().getName(), userId);
                    return null;
                }
            }
            log.warn("[JWT调试] 从token提取claims失败");
            return null;
        } catch (Exception e) {
            log.warn("[JWT调试] 提取userId时发生异常: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }
    

    
    /**
     * 从Token中获取角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("role") : null;
    }
    
    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return false;
            }
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }
    
    /**
     * 从请求头中提取Token
     */
    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(jwtConfig.getTokenPrefix())) {
            return authHeader.substring(jwtConfig.getTokenPrefix().length());
        }
        return null;
    }
}