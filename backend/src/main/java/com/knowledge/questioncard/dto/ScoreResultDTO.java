package com.knowledge.questioncard.dto;

import lombok.Data;
import java.util.List;

/**
 * 评分结果DTO
 */
@Data
public class ScoreResultDTO {
    private Integer round; // 轮次
    private List<PlayerScore> scores; // 玩家得分列表
    private RankingDTO ranking; // 排名信息
    
    @Data
    public static class PlayerScore {
        private Long userId;
        private String username;
        private String answer;
        private Integer score;
        private String feedback; // AI评分反馈
    }
    
    @Data
    public static class RankingDTO {
        private List<RankItem> currentRound; // 当前轮次排名
        private List<RankItem> total; // 总排名
    }
    
    @Data
    public static class RankItem {
        private Integer rank;
        private Long userId;
        private String username;
        private Integer score;
    }
}