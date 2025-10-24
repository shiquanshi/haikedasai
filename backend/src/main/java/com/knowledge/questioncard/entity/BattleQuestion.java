package com.knowledge.questioncard.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对战题目实体
 */
@Data
public class BattleQuestion {
    private String questionId; // 题目ID
    private String content; // 题目内容
    private String topic; // 主题
    private String difficulty; // 难度
    private Integer round; // 轮次
    private LocalDateTime generateTime; // 生成时间
    private Integer timeLimit; // 答题时限（秒）
    
    // 评分标准（由AI生成）
    private String scoringCriteria;
    
    // 参考答案（由AI生成，用于辅助评分）
    private String referenceAnswer;
    
    public BattleQuestion() {
        this.questionId = java.util.UUID.randomUUID().toString();
        this.generateTime = LocalDateTime.now();
        this.timeLimit = 60; // 默认60秒
    }
}