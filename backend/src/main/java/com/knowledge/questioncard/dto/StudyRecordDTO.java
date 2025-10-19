package com.knowledge.questioncard.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudyRecordDTO {
    private Long id;
    private Long userId;
    private Long bankId;
    private String bankName;
    private String bankTopic;
    private Integer totalCards;
    private Integer completedCards;
    private Integer correctCount;
    private Integer wrongCount;
    private BigDecimal masteryLevel;
    private Integer studyDuration;
    private LocalDateTime lastStudyAt;
    private LocalDateTime createdAt;
    
    // 计算进度百分比
    public Double getProgressPercentage() {
        if (totalCards == null || totalCards == 0) {
            return 0.0;
        }
        return (completedCards.doubleValue() / totalCards.doubleValue()) * 100;
    }
    
    // 计算正确率
    public Double getAccuracyRate() {
        int total = (correctCount != null ? correctCount : 0) + (wrongCount != null ? wrongCount : 0);
        if (total == 0) {
            return 0.0;
        }
        return (correctCount.doubleValue() / total) * 100;
    }
}