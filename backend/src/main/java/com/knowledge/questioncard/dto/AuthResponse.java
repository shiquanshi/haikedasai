package com.knowledge.questioncard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token; // JWT Token
    private String tokenType = "Bearer"; // Token类型
    private UserDTO user; // 用户信息
    
    public AuthResponse(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }
}