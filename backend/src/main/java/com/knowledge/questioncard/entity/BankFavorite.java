package com.knowledge.questioncard.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BankFavorite {
    private Long id;
    private Long userId; // 用户ID
    private Long bankId; // 题库ID
    private LocalDateTime createdAt; // 收藏时间
}