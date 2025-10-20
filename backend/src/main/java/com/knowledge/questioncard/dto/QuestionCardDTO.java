package com.knowledge.questioncard.dto;

import lombok.Data;
import java.util.Date;

@Data
public class QuestionCardDTO {
    private Long id;
    private Long bankId;
    private String question;
    private String answer;
    private String questionImage;
    private String answerImage;
    private Date createdAt;
    private Date updatedAt;
}