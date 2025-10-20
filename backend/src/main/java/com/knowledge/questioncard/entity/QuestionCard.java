package com.knowledge.questioncard.entity;

import lombok.Data;
import java.util.Date;

@Data
public class QuestionCard {
    private Long id;
    private String question;
    private String answer;
    private String questionImage;
    private String answerImage;
    private Long bankId;
    private Date createdAt;
    private Date updatedAt;
}