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
        
        String token = null;
        
        // 1. 优先从Header获取token
        String authHeader = request.getHeader(jwtConfig.getHeaderName());
        if (authHeader != null && authHeader.startsWith(jwtConfig.getTokenPrefix())) {
            token = authHeader.substring(jwtConfig.getTokenPrefix().length()).trim();
        }
        
        // 2. 如果Header中没有,尝试从URL参数获取(用于SSE等无法设置Header的场景)
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
            if (token != null) {
                token = token.trim();
            }
        }
        
        // 3. 验证token
        if (token != null && !token.isEmpty() && jwtUtil.validateToken(token)) {
            // 将用户信息存入request attributes
            request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
            request.setAttribute("username", jwtUtil.getUsernameFromToken(token));
            request.setAttribute("role", jwtUtil.getRoleFromToken(token));
            return true;
        }
        
        // 未认证或token无效
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}