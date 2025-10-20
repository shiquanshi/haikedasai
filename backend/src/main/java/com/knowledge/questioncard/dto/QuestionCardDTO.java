package com.knowledge.questioncard.dto;

import lombok.Data;

@Data
public class QuestionCardDTO {
    private Long id;
    private Long bankId;
    private String question;
    private String answer;
    private String questionImage;
    private String answerImage;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}