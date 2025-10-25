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
        // ��¼����·�����ڵ���
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        log.info("JwtInterceptor���ص�����: {} {}, contextPath={}, servletPath={}", request.getMethod(), requestURI, contextPath, servletPath);
        
        // OPTIONS����ֱ�ӷ���
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        String token = null;
        
        // 1. ���ȴ�Header��ȡtoken
        String authHeader = request.getHeader(jwtConfig.getHeaderName());
        if (authHeader != null && authHeader.startsWith(jwtConfig.getTokenPrefix())) {
            token = authHeader.substring(jwtConfig.getTokenPrefix().length()).trim();
        }
        
        // 2. ���Header��û��,���Դ�URL������ȡ(����SSE���޷�����Header�ĳ���)
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
            if (token != null) {
                token = token.trim();
            }
        }
        
        // 3. ����Ƿ��ֶ�������userId����
        String manualUserId = request.getParameter("userId");
        if (manualUserId != null && !manualUserId.isEmpty()) {
            try {
                Long userId = Long.parseLong(manualUserId);
                log.info("�ֶ�����userId: {}", userId);
                request.setAttribute("userId", userId);
                // ��������Ĭ�ϵ�username��role
                request.setAttribute("username", "manual_user_");
                request.setAttribute("role", "USER");
                return true;
            } catch (NumberFormatException e) {
                log.warn("�ֶ����ݵ�userId��ʽ��Ч: {}", manualUserId);
            }
        }

        // 4. ��֤token
        if (token != null && !token.isEmpty() && jwtUtil.validateToken(token)) {
            // ���û���Ϣ����request attributes
            request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
            request.setAttribute("username", jwtUtil.getUsernameFromToken(token));
            request.setAttribute("role", jwtUtil.getRoleFromToken(token));
            return true;
        }
        
        // δ��֤��token��Ч
        log.warn("JWT��֤ʧ�� - ����·��: {}, Token: {}", requestURI, token != null ? "���ڵ���Ч" : "������");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}