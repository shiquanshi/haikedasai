package com.knowledge.questioncard.entity;

import lombok.Data;
import java.util.Date;

@Data
public class BankFavorite {
    private Long id;
    private Long userId; // 用户ID
    private Long bankId; // 题库ID
    private Date createdAt; // 收藏时间
}