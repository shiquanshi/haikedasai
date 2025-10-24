package com.knowledge.questioncard.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * WebSocket消息DTO
 */
@Data
public class BattleMessage {
    private MessageType type; // 消息类型
    private String roomId; // 房间ID
    private Object data; // 消息数据
    private LocalDateTime timestamp; // 时间戳
    
    public BattleMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public BattleMessage(MessageType type, String roomId, Object data) {
        this();
        this.type = type;
        this.roomId = roomId;
        this.data = data;
    }
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        // 房间相关
        ROOM_CREATED,       // 房间已创建
        ROOM_LIST_UPDATED,  // 房间列表更新（创建或销毁）
        PLAYER_JOINED,      // 玩家加入
        PLAYER_LEFT,        // 玩家离开
        PLAYER_READY,       // 玩家准备
        
        // 游戏流程
        GAME_START,         // 游戏开始
        QUESTION_GENERATED, // 题目生成完成
        ANSWER_SUBMITTED,   // 答案已提交
        ROUND_FINISHED,     // 轮次结束
        SCORES_UPDATED,     // 得分更新
        GAME_FINISHED,      // 游戏结束
        
        // 倒计时
        COUNTDOWN,          // 倒计时更新
        
        // 错误
        ERROR               // 错误消息
    }
}