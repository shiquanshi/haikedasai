package com.knowledge.questioncard.config;

import com.knowledge.questioncard.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    
                    // 从token中提取用户信息
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);
                    
                    log.info("WebSocket认证成功: userId={}, username={}", userId, username);
                    
                    // 创建简单的Principal对象并设置到StompHeaderAccessor中
                    Principal principal = () -> username;
                    accessor.setUser(principal);
                    
                    // 将userId存储到session attributes中，供后续消息使用
                    accessor.getSessionAttributes().put("userId", userId);
                    accessor.getSessionAttributes().put("username", username);
                } catch (Exception e) {
                    log.error("WebSocket认证失败", e);
                }
            }
        } else if (accessor != null && accessor.getUser() != null) {
            // 对于非CONNECT命令，从session attributes中获取userId并设置到消息header
            Object userId = accessor.getSessionAttributes().get("userId");
            if (userId != null) {
                accessor.setHeader("userId", userId);
            }
        }
        
        return message;
    }
}