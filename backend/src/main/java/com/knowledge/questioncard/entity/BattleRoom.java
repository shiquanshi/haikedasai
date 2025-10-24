package com.knowledge.questioncard.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对战房间实体
 */
@Data
public class BattleRoom {
    private String roomId; // 房间ID
    private String roomName; // 房间名称
    private Long hostUserId; // 房主用户ID
    private RoomStatus status; // 房间状态
    private Integer maxPlayers; // 最大玩家数
    private Integer currentRound; // 当前轮次
    private Integer totalRounds; // 总轮次数
    private String topic; // 答题主题
    private String difficulty; // 难度等级
    private LocalDateTime createTime; // 创建时间
    
    // 使用线程安全的Map存储玩家
    private Map<Long, BattlePlayer> players; // 玩家列表 <userId, BattlePlayer>
    
    // 当前题目
    private BattleQuestion currentQuestion;
    
    // 答题开始时间
    private LocalDateTime questionStartTime;
    
    public BattleRoom() {
        this.players = new ConcurrentHashMap<>();
        this.status = RoomStatus.WAITING;
        this.currentRound = 0;
    }
    
    /**
     * 房间状态枚举
     */
    public enum RoomStatus {
        WAITING,    // 等待中
        READY,      // 准备就绪
        PLAYING,    // 答题中
        SCORING,    // 评分中
        FINISHED    // 已结束
    }
    
    /**
     * 添加玩家
     */
    public boolean addPlayer(BattlePlayer player) {
        if (players.size() >= maxPlayers) {
            return false;
        }
        players.put(player.getUserId(), player);
        return true;
    }
    
    /**
     * 移除玩家
     */
    public void removePlayer(Long userId) {
        players.remove(userId);
    }
    
    /**
     * 检查是否所有玩家都已准备（房主除外）
     */
    public boolean isAllPlayersReady() {
        if (players.isEmpty()) {
            return false;
        }
        // 排除房主，只检查其他玩家的准备状态
        return players.values().stream()
                .filter(player -> !player.getUserId().equals(hostUserId))
                .allMatch(BattlePlayer::isReady);
    }
    
    /**
     * 检查是否所有玩家都已提交答案
     */
    public boolean isAllPlayersAnswered() {
        return players.values().stream()
                .allMatch(p -> p.getCurrentRoundAnswer() != null);
    }
}