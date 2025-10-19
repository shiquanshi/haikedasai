package com.knowledge.questioncard.config;

import com.knowledge.questioncard.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        String token = request.getHeader(jwtConfig.getHeaderName());
        if (token != null && token.startsWith(jwtConfig.getTokenPrefix())) {
            token = token.substring(jwtConfig.getTokenPrefix().length()).trim();
            
            if (jwtUtil.validateToken(token)) {
                // 将用户信息存入request attributes
                request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
                request.setAttribute("username", jwtUtil.getUsernameFromToken(token));
                request.setAttribute("tenantId", jwtUtil.getTenantIdFromToken(token));
                request.setAttribute("role", jwtUtil.getRoleFromToken(token));
                return true;
            }
        }
        
        // 未认证或token无效
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}