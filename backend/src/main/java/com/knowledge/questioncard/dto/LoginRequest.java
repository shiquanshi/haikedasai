package com.knowledge.questioncard.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username; // 用户名或邮箱或手机号
    private String password; // 密码
}