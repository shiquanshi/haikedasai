package com.knowledge.questioncard.entity;

import lombok.Data;
import java.util.Date;

@Data
public class QuestionBank {
    private Long id;
    private String name;
    private String description;
    private String topic;
    private String type;
    private Date createdAt;
    private Integer cardCount;
    private Integer viewCount;
    private Integer favoriteCount;
    private String tags;
    private String difficulty;
    private String language;
    private String generatedContent;
    private String userId;
    private Date updatedAt;
}