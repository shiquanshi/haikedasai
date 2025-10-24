package com.knowledge.questioncard.controller;

import com.knowledge.questioncard.dto.*;
import com.knowledge.questioncard.entity.*;
import com.knowledge.questioncard.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 对战WebSocket控制器
 */
@Slf4j
@Controller
@RequestMapping("/battle")
public class BattleController {
    
    @Autowired
    private BattleRoomService roomService;
    
    @Autowired
    private BattleQuestionService questionService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 获取房间信息 (REST API)
     */
    @GetMapping("/room/{roomId}")
    @ResponseBody
    public Map<String, Object> getRoomInfo(@PathVariable String roomId) {
        try {
            log.info("REST API: 获取房间信息: roomId={}", roomId);
            BattleRoom room = roomService.getRoomById(roomId);
            if (room == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "房间不存在");
                return error;
            }
            BattleRoomDTO dto = roomService.convertToDTO(room);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("room", dto);
            return result;
        } catch (Exception e) {
            log.error("获取房间信息失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return error;
        }
    }
    
    /**
     * 创建房间
     */
    @MessageMapping("/battle/createRoom")
    public void createRoom(@Payload CreateRoomRequest request, @Header("userId") Long userId, @Header("username") String username) {
        try {
            log.info("收到创建房间请求: userId={}, username={}, request={}", userId, username, request);
            
            BattleRoom room = roomService.createRoom(request, userId, username);
            BattleRoomDTO dto = roomService.convertToDTO(room);
            
            // 发送房间创建成功消息到房间主题
            BattleMessage message = new BattleMessage(BattleMessage.MessageType.ROOM_CREATED, room.getRoomId(), dto);
            messagingTemplate.convertAndSend("/topic/battle/" + room.getRoomId(), message);
            
            // 广播房间列表更新消息到全局主题
            BattleMessage listUpdateMessage = new BattleMessage(BattleMessage.MessageType.ROOM_LIST_UPDATED, null, null);
            messagingTemplate.convertAndSend("/topic/battle/roomlist", listUpdateMessage);
            log.info("已广播房间列表更新消息: 房间创建");
            
            // 发送创建成功响应给创建者
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("room", dto);
            String destination = "/queue/create";
            log.info("发送创建房间响应: userId={}, username={}, destination={}, result={}", userId, username, destination, result);
            messagingTemplate.convertAndSendToUser(username, destination, result);
            log.info("创建房间响应已发送");
            
        } catch (Exception e) {
            log.error("创建房间失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            String destination = "/queue/create";
            log.info("发送创建房间失败响应: userId={}, username={}, destination={}, result={}", userId, username, destination, result);
            messagingTemplate.convertAndSendToUser(username, destination, result);
            log.info("创建房间失败响应已发送");
        }
    }
    
    /**
     * 获取房间列表
     */
    @MessageMapping("/battle/rooms")
    public void getRooms(Principal principal) {
        try {
            String userId = principal != null ? principal.getName() : "0";
            log.info("收到获取房间列表请求: userId={}", userId);
            
            List<BattleRoom> allRooms = roomService.getAllRooms();
            List<BattleRoomDTO> roomDTOs = allRooms.stream()
                .filter(room -> room.getStatus() == BattleRoom.RoomStatus.WAITING)
                .map(roomService::convertToDTO)
                .collect(Collectors.toList());
            
            messagingTemplate.convertAndSendToUser(userId, "/queue/rooms", roomDTOs);
            log.info("返回房间列表: userId={}, count={}", userId, roomDTOs.size());
        } catch (Exception e) {
            log.error("获取房间列表失败", e);
            String userId = principal != null ? principal.getName() : "0";
            messagingTemplate.convertAndSendToUser(userId, "/queue/rooms", new ArrayList<>());
        }
    }
    
    /**
     * 加入房间
     */
    @MessageMapping("/battle/joinRoom")
    public void joinRoom(@Payload Map<String, String> payload, @Header("userId") Long userId, @Header("username") String username) {
        String roomId = payload.get("roomId");
        try {
            log.info("收到加入房间请求: roomId={}, userId={}, username={}", roomId, userId, username);
            
            BattleRoom room = roomService.joinRoom(roomId, userId, username);
            BattleRoomDTO dto = roomService.convertToDTO(room);
            
            // 广播玩家加入消息
            BattleMessage message = new BattleMessage(BattleMessage.MessageType.PLAYER_JOINED, roomId, dto);
            messagingTemplate.convertAndSend("/topic/battle/" + roomId, message);
            
            // 发送加入成功响应给加入者
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("roomId", roomId);
            result.put("room", dto);
            messagingTemplate.convertAndSendToUser(username, "/queue/join", result);
            
        } catch (Exception e) {
            log.error("加入房间失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            messagingTemplate.convertAndSendToUser(username, "/queue/join", result);
        }
    }
    
    /**
     * 离开房间
     */
    @MessageMapping("/battle/leaveRoom")
    public void leaveRoom(@Payload Map<String, String> payload) {
        String roomId = payload.get("roomId");
        Long userId = Long.parseLong(payload.get("userId"));
        try {
            log.info("收到离开房间请求: roomId={}, userId={}", roomId, userId);
            
            roomService.leaveRoom(roomId, userId);
            
            // 广播玩家离开消息，包含更新后的完整房间信息
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            
            // 获取更新后的房间信息
            boolean roomDeleted = false;
            try {
                BattleRoom room = roomService.getRoomById(roomId);
                if (room != null) {
                    BattleRoomDTO roomDTO = roomService.convertToDTO(room);
                    data.put("room", roomDTO);
                } else {
                    roomDeleted = true;
                }
            } catch (Exception e) {
                // 如果房间已被删除（房主离开或房间为空），只发送userId
                log.warn("获取房间信息失败，房间可能已被删除: roomId={}", roomId);
                roomDeleted = true;
            }
            
            BattleMessage message = new BattleMessage(BattleMessage.MessageType.PLAYER_LEFT, roomId, data);
            messagingTemplate.convertAndSend("/topic/battle/" + roomId, message);
            
            // 如果房间被删除，广播房间列表更新消息
            if (roomDeleted) {
                BattleMessage listUpdateMessage = new BattleMessage(BattleMessage.MessageType.ROOM_LIST_UPDATED, null, null);
                messagingTemplate.convertAndSend("/topic/battle/roomlist", listUpdateMessage);
                log.info("已广播房间列表更新消息: 房间销毁");
            }
            
        } catch (Exception e) {
            log.error("离开房间失败", e);
        }
    }
    
    /**
     * 切换准备状态
     */
    @MessageMapping("/battle/toggleReady")
    public void toggleReady(@Payload Map<String, String> payload) {
        String roomId = payload.get("roomId");
        Long userId = Long.parseLong(payload.get("userId"));
        try {
            log.info("收到切换准备请求: roomId={}, userId={}", roomId, userId);
            
            BattleRoom room = roomService.toggleReady(roomId, userId);
            BattleRoomDTO dto = roomService.convertToDTO(room);
            
            // 广播玩家准备状态
            BattleMessage message = new BattleMessage(BattleMessage.MessageType.PLAYER_READY, roomId, dto);
            messagingTemplate.convertAndSend("/topic/battle/" + roomId, message);
            
        } catch (Exception e) {
            log.error("切换准备状态失败", e);
            sendError(roomId, e.getMessage());
        }
    }
    
    /**
     * 开始游戏
     */
    @MessageMapping("/battle/startGame")
    public void startGame(@Payload Map<String, String> payload) {
        String roomId = payload.get("roomId");
        Long userId = Long.parseLong(payload.get("userId"));
        try {
            log.info("收到开始游戏请求: roomId={}, userId={}", roomId, userId);
            
            BattleRoom room = roomService.startGame(roomId, userId);
            
            // 广播游戏开始消息
            BattleMessage startMessage = new BattleMessage(BattleMessage.MessageType.GAME_START, roomId, null);
            messagingTemplate.convertAndSend("/topic/battle/" + roomId, startMessage);
            
            // 异步生成第一题
            new Thread(() -> generateAndSendQuestion(room)).start();
            
        } catch (Exception e) {
            log.error("开始游戏失败", e);
            sendError(roomId, e.getMessage());
        }
    }
    
    /**
     * 提交答案
     */
    @MessageMapping("/battle/submitAnswer")
    public void submitAnswer(@Payload SubmitAnswerRequest request, @Header("userId") Long userId, @Header("username") String username) {
        try {
            log.info("收到提交答案请求: roomId={}, userId={}", request.getRoomId(), userId);
            
            BattleRoom room = roomService.submitAnswer(request.getRoomId(), userId, request.getAnswer());
            
            // 广播答案已提交
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("username", username);
            BattleMessage message = new BattleMessage(BattleMessage.MessageType.ANSWER_SUBMITTED, request.getRoomId(), data);
            messagingTemplate.convertAndSend("/topic/battle/" + request.getRoomId(), message);
            
            // 检查是否所有玩家都已提交
            boolean allSubmitted = room.getPlayers().values().stream()
                .allMatch(p -> p.getCurrentRoundAnswer() != null);
            
            if (allSubmitted) {
                log.info("所有玩家已提交答案，开始评分: roomId={}", request.getRoomId());
                new Thread(() -> scoreAndFinishRound(room)).start();
            }
            
        } catch (Exception e) {
            log.error("提交答案失败", e);
            sendError(request.getRoomId(), e.getMessage());
        }
    }
    
    /**
     * 生成并发送题目
     */
    private void generateAndSendQuestion(BattleRoom room) {
        try {
            log.info("开始生成题目: roomId={}, round={}", room.getRoomId(), room.getCurrentRound());
            
            BattleQuestion question = questionService.generateQuestion(
                room.getTopic(), 
                room.getDifficulty(), 
                room.getCurrentRound()
            );
            
            roomService.setCurrentQuestion(room.getRoomId(), question);
            
            // 广播题目
            Map<String, Object> data = new HashMap<>();
            data.put("round", question.getRound());
            data.put("content", question.getContent());
            data.put("timeLimit", question.getTimeLimit());
            
            BattleMessage message = new BattleMessage(BattleMessage.MessageType.QUESTION_GENERATED, room.getRoomId(), data);
            messagingTemplate.convertAndSend("/topic/battle/" + room.getRoomId() + "/question", message);
            
            // 启动倒计时
            startCountdown(room.getRoomId(), question.getTimeLimit());
            
        } catch (Exception e) {
            log.error("生成题目失败", e);
            sendError(room.getRoomId(), "生成题目失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动倒计时
     */
    private void startCountdown(String roomId, int timeLimit) {
        new Thread(() -> {
            try {
                for (int remaining = timeLimit; remaining > 0; remaining--) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("remaining", remaining);
                    BattleMessage message = new BattleMessage(BattleMessage.MessageType.COUNTDOWN, roomId, data);
                    messagingTemplate.convertAndSend("/topic/battle/" + roomId, message);
                    Thread.sleep(1000);
                }
                
                // 倒计时结束，评分
                BattleRoom room = roomService.getRoomById(roomId);
                scoreAndFinishRound(room);
                
            } catch (Exception e) {
                log.error("倒计时异常", e);
            }
        }).start();
    }
    
    /**
     * 评分并完成轮次
     */
    private void scoreAndFinishRound(BattleRoom room) {
        try {
            log.info("开始评分: roomId={}, round={}", room.getRoomId(), room.getCurrentRound());
            
            BattleQuestion question = room.getCurrentQuestion();
            
            // 收集所有答案
            Map<Long, String> answers = new HashMap<>();
            room.getPlayers().forEach((userId, player) -> {
                String answer = player.getCurrentRoundAnswer();
                if (answer != null && !answer.trim().isEmpty()) {
                    answers.put(userId, answer);
                } else {
                    answers.put(userId, ""); // 空答案
                }
            });
            
            // 批量评分
            final Map<Long, BattleQuestionService.ScoreResult> scores = questionService.scoreAnswers(question, answers);
            
            // 更新玩家得分
            BattleRoom finalRoom = room;
            scores.forEach((userId, result) -> {
                roomService.setPlayerScore(finalRoom.getRoomId(), userId, result.getScore());
            });
            
            // 准备评分结果DTO
            ScoreResultDTO resultDTO = new ScoreResultDTO();
            resultDTO.setRound(room.getCurrentRound());
            
            // 玩家得分列表
            List<ScoreResultDTO.PlayerScore> playerScores = new ArrayList<>();
            room.getPlayers().forEach((userId, player) -> {
                ScoreResultDTO.PlayerScore ps = new ScoreResultDTO.PlayerScore();
                ps.setUserId(userId);
                ps.setUsername(player.getUsername());
                ps.setAnswer(player.getCurrentRoundAnswer());
                ps.setScore(player.getCurrentRoundScore());
                
                BattleQuestionService.ScoreResult scoreResult = scores.get(userId);
                if (scoreResult != null) {
                    ps.setFeedback(scoreResult.getFeedback());
                }
                
                playerScores.add(ps);
            });
            resultDTO.setScores(playerScores);
            
            // 排名信息
            ScoreResultDTO.RankingDTO ranking = new ScoreResultDTO.RankingDTO();
            
            // 当前轮次排名
            List<ScoreResultDTO.RankItem> currentRoundRank = playerScores.stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .map(ps -> {
                    ScoreResultDTO.RankItem item = new ScoreResultDTO.RankItem();
                    item.setUserId(ps.getUserId());
                    item.setUsername(ps.getUsername());
                    item.setScore(ps.getScore());
                    return item;
                })
                .collect(Collectors.toList());
            
            for (int i = 0; i < currentRoundRank.size(); i++) {
                currentRoundRank.get(i).setRank(i + 1);
            }
            ranking.setCurrentRound(currentRoundRank);
            
            // 总排名
            List<ScoreResultDTO.RankItem> totalRank = room.getPlayers().values().stream()
                .sorted((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()))
                .map(player -> {
                    ScoreResultDTO.RankItem item = new ScoreResultDTO.RankItem();
                    item.setUserId(player.getUserId());
                    item.setUsername(player.getUsername());
                    item.setScore(player.getTotalScore() + player.getCurrentRoundScore());
                    return item;
                })
                .collect(Collectors.toList());
            
            for (int i = 0; i < totalRank.size(); i++) {
                totalRank.get(i).setRank(i + 1);
            }
            ranking.setTotal(totalRank);
            
            resultDTO.setRanking(ranking);
            
            // 广播得分结果
            BattleMessage scoreMessage = new BattleMessage(BattleMessage.MessageType.SCORES_UPDATED, room.getRoomId(), resultDTO);
            messagingTemplate.convertAndSend("/topic/battle/" + room.getRoomId() + "/score", scoreMessage);
            
            // 等待3秒后继续
            Thread.sleep(3000);
            
            // 完成当前轮次
            room = roomService.finishRound(room.getRoomId());
            
            // 广播轮次结束
            BattleMessage roundFinishMessage = new BattleMessage(BattleMessage.MessageType.ROUND_FINISHED, room.getRoomId(), null);
            messagingTemplate.convertAndSend("/topic/battle/" + room.getRoomId(), roundFinishMessage);
            
            // 判断是否继续下一轮或结束游戏
            if (room.getStatus() == BattleRoom.RoomStatus.FINISHED) {
                // 游戏结束
                BattleMessage gameFinishMessage = new BattleMessage(BattleMessage.MessageType.GAME_FINISHED, room.getRoomId(), resultDTO);
                messagingTemplate.convertAndSend("/topic/battle/" + room.getRoomId(), gameFinishMessage);
            } else {
                // 等待2秒后生成下一题
                Thread.sleep(2000);
                generateAndSendQuestion(room);
            }
            
        } catch (Exception e) {
            log.error("评分失败", e);
            sendError(room.getRoomId(), "评分失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送错误消息
     */
    private void sendError(String roomId, String errorMessage) {
        Map<String, String> data = new HashMap<>();
        data.put("message", errorMessage);
        BattleMessage message = new BattleMessage(BattleMessage.MessageType.ERROR, roomId, data);
        
        if (roomId != null) {
            messagingTemplate.convertAndSend("/topic/battle/" + roomId, message);
        }
    }
}