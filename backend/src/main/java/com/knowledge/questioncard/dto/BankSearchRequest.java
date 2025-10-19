package com.knowledge.questioncard.dto;

import lombok.Data;

@Data
public class BankSearchRequest extends PageRequest {
    private String topic; // 主题搜索
    private String type; // 类型筛选：ai, system, custom
    private String difficulty; // 难度筛选：easy, medium, hard
    private String tags; // 标签筛选，逗号分隔
    private Long userId; // 用户ID筛选
    private String tenantId; // 租户ID筛选（SaaS隔离）
    private Integer minCards; // 最小题目数
    private Integer maxCards; // 最大题目数
}