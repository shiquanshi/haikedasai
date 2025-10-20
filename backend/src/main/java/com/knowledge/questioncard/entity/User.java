package com.knowledge.questioncard.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Long id;
    private String username; // 用户名（唯一）
    private String password; // 密码（加密存储）
    private String email; // 邮箱
    private String nickname; // 昵称
    private String phone; // 手机号
    private String avatar; // 头像URL
    private String role; // 角色：user, admin
    private Integer status; // 状态：0-禁用，1-正常
    private Date createdAt; // 创建时间
    private Date updatedAt; // 更新时间
    private Date lastLoginAt; // 最后登录时间
}