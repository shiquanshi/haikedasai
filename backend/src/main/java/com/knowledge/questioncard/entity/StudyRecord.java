package com.knowledge.questioncard.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class StudyRecord {
    private Long id;
    private Long userId;
    private Long bankId;
    private Integer totalCards;
    private Integer completedCards;
    private Integer correctCount;
    private Integer wrongCount;
    private BigDecimal masteryLevel;
    private Integer studyDuration;
    private Date lastStudyAt;
    private Date createdAt;
    private Date updatedAt;
}