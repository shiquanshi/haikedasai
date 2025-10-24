package com.knowledge.questioncard.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房间信息DTO
 */
@Data
public class BattleRoomDTO {
    private String roomId;
    private String roomName;
    private Long hostUserId;
    private String hostUsername;
    private String status;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private Integer currentRound;
    private Integer totalRounds;
    private String topic;
    private String difficulty;
    private LocalDateTime createTime;
    private List<PlayerDTO> players;
    
    @Data
    public static class PlayerDTO {
        private Long userId;
        private String username;
        private boolean ready;
        private Integer totalScore;
        private LocalDateTime joinTime;
    }
}