package com.knowledge.questioncard.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String phone;
    private String avatar;
    private String role;
    private Integer status;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}