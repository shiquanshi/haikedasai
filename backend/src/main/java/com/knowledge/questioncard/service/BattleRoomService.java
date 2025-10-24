package com.knowledge.questioncard.service;

import com.knowledge.questioncard.dto.*;
import com.knowledge.questioncard.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 对战房间管理服务
 */
@Slf4j
@Service
public class BattleRoomService {
    
    // 使用内存存储房间信息（生产环境建议使用Redis）
    private final Map<String, BattleRoom> rooms = new ConcurrentHashMap<>();
    
    /**
     * 创建房间
     */
    public BattleRoom createRoom(CreateRoomRequest request, Long userId, String username) {
        BattleRoom room = new BattleRoom();
        room.setRoomId(generateRoomId());
        room.setRoomName(request.getRoomName());
        room.setHostUserId(userId);
        room.setMaxPlayers(request.getMaxPlayers());
        room.setTotalRounds(request.getTotalRounds());
        room.setTopic(request.getTopic());
        room.setDifficulty(request.getDifficulty());
        room.setCreateTime(LocalDateTime.now());
        
        // 房主自动加入房间
        BattlePlayer host = new BattlePlayer(userId, username);
        room.addPlayer(host);
        
        rooms.put(room.getRoomId(), room);
        log.info("房间创建成功: roomId={}, roomName={}, host={}", room.getRoomId(), room.getRoomName(), username);
        
        return room;
    }
    
    /**
     * 加入房间
     */
    public BattleRoom joinRoom(String roomId, Long userId, String username) {
        BattleRoom room = rooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        
        if (room.getStatus() != BattleRoom.RoomStatus.WAITING) {
            throw new RuntimeException("房间已开始，无法加入");
        }
        
        if (room.getPlayers().containsKey(userId)) {
            throw new RuntimeException("已在房间中");
        }
        
        BattlePlayer player = new BattlePlayer(userId, username);
        if (!room.addPlayer(player)) {
            throw new RuntimeException("房间已满");
        }
        
        log.info("玩家加入房间: roomId={}, userId={}, username={}", roomId, userId, username);
        return room;
    }
    
    /**
     * 离开房间
     */
    public void leaveRoom(String roomId, Long userId) {
        BattleRoom room = rooms.get(roomId);
        if (room == null) {
            return;
        }
        
        room.removePlayer(userId);
        log.info("玩家离开房间: roomId={}, userId={}", roomId, userId);
        
        // 如果是房主离开或房间为空，删除房间
        if (userId.equals(room.getHostUserId()) || room.getPlayers().isEmpty()) {
            rooms.remove(roomId);
            log.info("房间已删除: roomId={}", roomId);
        }
    }
    
    /**
     * 玩家准备
     */
    public BattleRoom toggleReady(String roomId, Long userId) {
        BattleRoom room = getRoomById(roomId);
        BattlePlayer player = room.getPlayers().get(userId);
        
        if (player == null) {
            throw new RuntimeException("玩家不在房间中");
        }
        
        if (userId.equals(room.getHostUserId())) {
            throw new RuntimeException("房主无需准备");
        }
        
        player.setReady(!player.isReady());
        log.info("玩家准备状态切换: roomId={}, userId={}, ready={}", roomId, userId, player.isReady());
        
        return room;
    }
    
    /**
     * 开始游戏
     */
    public BattleRoom startGame(String roomId, Long userId) {
        BattleRoom room = getRoomById(roomId);
        
        if (!userId.equals(room.getHostUserId())) {
            throw new RuntimeException("只有房主可以开始游戏");
        }
        
        if (room.getPlayers().size() < 2) {
            throw new RuntimeException("至少需要2名玩家");
        }
        
        if (!room.isAllPlayersReady()) {
            throw new RuntimeException("还有玩家未准备");
        }
        
        room.setStatus(BattleRoom.RoomStatus.PLAYING);
        room.setCurrentRound(1);
        log.info("游戏开始: roomId={}", roomId);
        
        return room;
    }
    
    /**
     * 设置当前题目
     */
    public void setCurrentQuestion(String roomId, BattleQuestion question) {
        BattleRoom room = getRoomById(roomId);
        room.setCurrentQuestion(question);
        room.setQuestionStartTime(LocalDateTime.now());
        
        // 重置所有玩家的当前轮次状态
        room.getPlayers().values().forEach(BattlePlayer::resetCurrentRound);
        
        log.info("题目已设置: roomId={}, round={}", roomId, question.getRound());
    }
    
    /**
     * 提交答案
     */
    public BattleRoom submitAnswer(String roomId, Long userId, String answer) {
        BattleRoom room = getRoomById(roomId);
        
        if (room.getStatus() != BattleRoom.RoomStatus.PLAYING) {
            throw new RuntimeException("当前不在答题阶段");
        }
        
        BattlePlayer player = room.getPlayers().get(userId);
        if (player == null) {
            throw new RuntimeException("玩家不在房间中");
        }
        
        if (player.getCurrentRoundAnswer() != null) {
            throw new RuntimeException("已提交答案");
        }
        
        player.setCurrentRoundAnswer(answer);
        log.info("答案已提交: roomId={}, userId={}, answerLength={}", roomId, userId, answer.length());
        
        return room;
    }
    
    /**
     * 设置玩家得分
     */
    public void setPlayerScore(String roomId, Long userId, Integer score) {
        BattleRoom room = getRoomById(roomId);
        BattlePlayer player = room.getPlayers().get(userId);
        
        if (player != null) {
            player.setCurrentRoundScore(score);
        }
    }
    

    
    /**
     * 完成当前轮次
     */
    public BattleRoom finishRound(String roomId) {
        BattleRoom room = getRoomById(roomId);
        
        // 保存所有玩家的轮次得分
        room.getPlayers().values().forEach(player -> 
            player.saveRoundScore(room.getCurrentRound())
        );
        
        // 增加当前轮次
        room.setCurrentRound(room.getCurrentRound() + 1);
        
        // 判断是否还有下一轮
        if (room.getCurrentRound() <= room.getTotalRounds()) {
            room.setStatus(BattleRoom.RoomStatus.PLAYING);
            log.info("进入下一轮: roomId={}, round={}/{}", roomId, room.getCurrentRound(), room.getTotalRounds());
        } else {
            room.setStatus(BattleRoom.RoomStatus.FINISHED);
            log.info("游戏结束: roomId={}, 完成轮次: {}/{}", roomId, room.getTotalRounds(), room.getTotalRounds());
        }
        
        return room;
    }
    
    /**
     * 获取房间信息
     */
    public BattleRoom getRoomById(String roomId) {
        BattleRoom room = rooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        return room;
    }
    
    /**
     * 获取所有房间列表
     */
    public List<BattleRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }
    
    /**
     * 转换为DTO
     */
    public BattleRoomDTO convertToDTO(BattleRoom room) {
        BattleRoomDTO dto = new BattleRoomDTO();
        dto.setRoomId(room.getRoomId());
        dto.setRoomName(room.getRoomName());
        dto.setHostUserId(room.getHostUserId());
        dto.setStatus(room.getStatus().name());
        dto.setMaxPlayers(room.getMaxPlayers());
        dto.setCurrentPlayers(room.getPlayers().size());
        dto.setCurrentRound(room.getCurrentRound());
        dto.setTotalRounds(room.getTotalRounds());
        dto.setTopic(room.getTopic());
        dto.setDifficulty(room.getDifficulty());
        dto.setCreateTime(room.getCreateTime());
        
        // 设置房主用户名
        BattlePlayer host = room.getPlayers().get(room.getHostUserId());
        if (host != null) {
            dto.setHostUsername(host.getUsername());
        }
        
        // 转换玩家列表
        List<BattleRoomDTO.PlayerDTO> players = room.getPlayers().values().stream()
            .map(this::convertPlayerToDTO)
            .collect(Collectors.toList());
        dto.setPlayers(players);
        
        return dto;
    }
    
    private BattleRoomDTO.PlayerDTO convertPlayerToDTO(BattlePlayer player) {
        BattleRoomDTO.PlayerDTO dto = new BattleRoomDTO.PlayerDTO();
        dto.setUserId(player.getUserId());
        dto.setUsername(player.getUsername());
        dto.setReady(player.isReady());
        dto.setTotalScore(player.getTotalScore());
        dto.setJoinTime(player.getJoinTime());
        return dto;
    }
    
    /**
     * 生成房间ID
     */
    private String generateRoomId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}