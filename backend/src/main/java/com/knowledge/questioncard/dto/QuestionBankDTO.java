package com.knowledge.questioncard.dto;

import lombok.Data;

@Data
public class QuestionBankDTO {
    private Long id;
    private String name;
    private String description;
    private String topic;
    private String type; // 类型：ai, system, custom
    private String tags; // 标签，逗号分隔
    private Integer cardCount; // 题目数量
    private String difficulty; // 难度：easy, medium, hard
    private String language; // 语言
}