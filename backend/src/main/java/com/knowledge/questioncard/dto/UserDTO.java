package com.knowledge.questioncard.dto;

import lombok.Data;
import java.util.Date;

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
    private Date createdAt;
    private Date lastLoginAt;
}