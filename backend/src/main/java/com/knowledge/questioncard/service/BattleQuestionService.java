package com.knowledge.questioncard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.questioncard.entity.BattleQuestion;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 对战题目生成和评分服务
 */
@Slf4j
@Service
public class BattleQuestionService {
    
    @Autowired
    private ArkService arkService;
    
    @Value("${volcengine.api.model}")
    private String modelName;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 生成对战题目
     */
    public BattleQuestion generateQuestion(String topic, String difficulty, int round) {
        log.info("开始生成题目: topic={}, difficulty={}, round={}", topic, difficulty, round);
        
        String prompt = buildQuestionPrompt(topic, difficulty, round);
        
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM)
                .content("你是一个专业的出题专家，擅长根据主题和难度生成高质量的开放性问题。")
                .build());
            messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(prompt)
                .build());
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(modelName)
                .messages(messages)
                .temperature(0.8)
                .build();
            
            Object contentObj = arkService.createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent();
            String response = contentObj != null ? contentObj.toString() : "";
            
            log.info("AI生成题目响应: {}", response);
            
            return parseQuestionResponse(response, topic, difficulty, round);
            
        } catch (Exception e) {
            log.error("生成题目失败", e);
            throw new RuntimeException("生成题目失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量评分
     */
    public Map<Long, ScoreResult> scoreAnswers(BattleQuestion question, Map<Long, String> answers) {
        log.info("开始评分: questionId={}, 答案数量={}", question.getQuestionId(), answers.size());
        
        Map<Long, ScoreResult> results = new HashMap<>();
        
        for (Map.Entry<Long, String> entry : answers.entrySet()) {
            Long userId = entry.getKey();
            String answer = entry.getValue();
            
            try {
                ScoreResult result = scoreAnswer(question, answer);
                results.put(userId, result);
            } catch (Exception e) {
                log.error("评分失败: userId={}", userId, e);
                // 如果评分失败，给予默认分数
                ScoreResult defaultResult = new ScoreResult();
                defaultResult.setScore(0);
                defaultResult.setFeedback("评分失败，系统错误");
                results.put(userId, defaultResult);
            }
        }
        
        return results;
    }
    
    /**
     * 单个答案评分
     */
    private ScoreResult scoreAnswer(BattleQuestion question, String answer) {
        String prompt = buildScoringPrompt(question, answer);
        
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM)
                .content("你是一个公正严格的评分专家，需要根据题目要求和评分标准，对答案进行客观评分。")
                .build());
            messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(prompt)
                .build());
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(modelName)
                .messages(messages)
                .temperature(0.3)
                .build();
            
            Object scoreContentObj = arkService.createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent();
            String response = scoreContentObj != null ? scoreContentObj.toString() : "";
            
            log.info("AI评分响应: {}", response);
            
            return parseScoreResponse(response);
            
        } catch (Exception e) {
            log.error("评分失败", e);
            throw new RuntimeException("评分失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建题目生成提示词
     */
    private String buildQuestionPrompt(String topic, String difficulty, int round) {
        String difficultyDesc = switch (difficulty.toLowerCase()) {
            case "easy" -> "简单";
            case "medium" -> "中等";
            case "hard" -> "困难";
            default -> "中等";
        };
        
        return String.format(
            "请根据以下要求生成一道开放性问题：\n\n" +
            "主题：%s\n" +
            "难度：%s\n" +
            "轮次：第%d轮\n\n" +
            "要求：\n" +
            "1. 问题应该是开放性的，需要思考和表达观点\n" +
            "2. 问题难度符合%s级别\n" +
            "3. 问题与主题密切相关\n" +
            "4. 适合在60秒内作答\n" +
            "5. 问题要有一定的深度，能够区分不同水平的答案\n\n" +
            "请以JSON格式返回，包含以下字段：\n" +
            "{\n" +
            "    \"question\": \"题目内容\",\n" +
            "    \"scoring_criteria\": \"评分标准（100分制）\",\n" +
            "    \"reference_answer\": \"参考答案要点\"\n" +
            "}",
            topic, difficultyDesc, round, difficultyDesc);
    }
    
    /**
     * 构建评分提示词
     */
    private String buildScoringPrompt(BattleQuestion question, String answer) {
        return String.format(
            "请对以下答案进行评分：\n\n" +
            "题目：%s\n\n" +
            "评分标准：\n%s\n\n" +
            "参考答案要点：\n%s\n\n" +
            "学生答案：\n%s\n\n" +
            "要求：\n" +
            "1. 满分100分\n" +
            "2. 根据答案的完整性、准确性、深度进行评分\n" +
            "3. 提供简短的评语（50字以内）\n" +
            "4. 如果答案完全不相关或为空，给0分\n\n" +
            "请以JSON格式返回：\n" +
            "{\n" +
            "    \"score\": 分数(0-100),\n" +
            "    \"feedback\": \"评语\"\n" +
            "}",
            question.getContent(),
            question.getScoringCriteria(),
            question.getReferenceAnswer(),
            answer);
    }
    
    /**
     * 解析题目生成响应
     */
    private BattleQuestion parseQuestionResponse(String response, String topic, String difficulty, int round) {
        try {
            // 尝试从markdown代码块中提取JSON
            String jsonStr = response;
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.lastIndexOf("```");
                jsonStr = response.substring(start, end).trim();
            } else if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.lastIndexOf("```");
                jsonStr = response.substring(start, end).trim();
            }
            
            JsonNode node = objectMapper.readTree(jsonStr);
            
            BattleQuestion question = new BattleQuestion();
            question.setContent(node.get("question").asText());
            question.setScoringCriteria(node.get("scoring_criteria").asText());
            question.setReferenceAnswer(node.get("reference_answer").asText());
            question.setTopic(topic);
            question.setDifficulty(difficulty);
            question.setRound(round);
            question.setTimeLimit(60);
            
            return question;
            
        } catch (Exception e) {
            log.error("解析题目响应失败: {}", response, e);
            throw new RuntimeException("解析题目失败");
        }
    }
    
    /**
     * 解析评分响应
     */
    private ScoreResult parseScoreResponse(String response) {
        try {
            // 尝试从markdown代码块中提取JSON
            String jsonStr = response;
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.lastIndexOf("```");
                jsonStr = response.substring(start, end).trim();
            } else if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.lastIndexOf("```");
                jsonStr = response.substring(start, end).trim();
            }
            
            JsonNode node = objectMapper.readTree(jsonStr);
            
            ScoreResult result = new ScoreResult();
            result.setScore(node.get("score").asInt());
            result.setFeedback(node.get("feedback").asText());
            
            return result;
            
        } catch (Exception e) {
            log.error("解析评分响应失败: {}", response, e);
            throw new RuntimeException("解析评分失败");
        }
    }
    
    /**
     * 评分结果内部类
     */
    public static class ScoreResult {
        private Integer score;
        private String feedback;
        
        public Integer getScore() {
            return score;
        }
        
        public void setScore(Integer score) {
            this.score = score;
        }
        
        public String getFeedback() {
            return feedback;
        }
        
        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
    }
}