package com.knowledge.questioncard.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username; // 用户名（必填）
    private String password; // 密码（必填）
    private String email; // 邮箱（必填）
    private String nickname; // 昵称（可选）
    private String phone; // 手机号（可选）
    private String inviteCode; // 邀请码（用于租户关联，可选）
}