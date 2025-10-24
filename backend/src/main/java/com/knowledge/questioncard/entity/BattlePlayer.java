package com.knowledge.questioncard.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对战玩家实体
 */
@Data
public class BattlePlayer {
    private Long userId; // 用户ID
    private String username; // 用户名
    private boolean ready; // 是否准备
    private Integer totalScore; // 总得分
    private LocalDateTime joinTime; // 加入时间
    
    // 当前轮次的答案
    private String currentRoundAnswer;
    
    // 当前轮次的得分
    private Integer currentRoundScore;
    
    // 历史轮次得分记录
    private List<RoundScore> roundScores;
    
    public BattlePlayer() {
        this.ready = false;
        this.totalScore = 0;
        this.roundScores = new ArrayList<>();
    }
    
    public BattlePlayer(Long userId, String username) {
        this();
        this.userId = userId;
        this.username = username;
        this.joinTime = LocalDateTime.now();
    }
    
    /**
     * 重置当前轮次状态
     */
    public void resetCurrentRound() {
        this.currentRoundAnswer = null;
        this.currentRoundScore = null;
    }
    
    /**
     * 保存当前轮次得分到历史记录
     */
    public void saveRoundScore(int round) {
        if (currentRoundScore != null) {
            RoundScore score = new RoundScore();
            score.setRound(round);
            score.setAnswer(currentRoundAnswer);
            score.setScore(currentRoundScore);
            roundScores.add(score);
            totalScore += currentRoundScore;
        }
    }
    
    /**
     * 单轮得分记录
     */
    @Data
    public static class RoundScore {
        private Integer round; // 轮次
        private String answer; // 答案
        private Integer score; // 得分
    }
}