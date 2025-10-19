package com.knowledge.questioncard.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudyRecord {
    private Long id;
    private Long userId;
    private Long bankId;
    private Long tenantId;
    private Integer totalCards;
    private Integer completedCards;
    private Integer correctCount;
    private Integer wrongCount;
    private BigDecimal masteryLevel;
    private Integer studyDuration;
    private LocalDateTime lastStudyAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}