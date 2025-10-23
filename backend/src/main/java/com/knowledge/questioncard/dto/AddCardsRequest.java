package com.knowledge.questioncard.dto;

import lombok.Data;
import java.util.List;

@Data
public class AddCardsRequest {
    private Long targetBankId; // 目标题库ID
    private List<Long> cardIds; // 要添加的卡片ID列表
    private Long sourceBankId; // 来源题库ID（用于统计导入次数）
}