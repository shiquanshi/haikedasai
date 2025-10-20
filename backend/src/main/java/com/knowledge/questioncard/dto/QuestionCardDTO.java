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
    private java.util.Date createdAt;
    private java.util.Date updatedAt;
}