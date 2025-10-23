package com.knowledge.questioncard.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

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
    private Date createdAt; // 创建时间
    private Date updatedAt; // 更新时间
    private Integer viewCount; // 浏览次数
    private Integer favoriteCount; // 收藏次数
    private Long userId; // 创建者ID
    private String shareCode; // 分享码
    private Boolean favorited; // 是否已收藏
    private List<QuestionCardDTO> cards; // 卡片列表
}