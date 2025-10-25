package com.knowledge.questioncard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.questioncard.common.Result;
import com.knowledge.questioncard.dto.*;
import com.knowledge.questioncard.entity.SharedBank;
import com.knowledge.questioncard.service.BankFavoriteService;
import com.knowledge.questioncard.service.QuestionBankService;
import com.knowledge.questioncard.service.SharedBankService;
import com.knowledge.questioncard.service.VolcEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@RestController
@RequestMapping("/question-bank")
@RequiredArgsConstructor
@Slf4j
public class QuestionBankController {
    private final QuestionBankService questionBankService;
    private final BankFavoriteService bankFavoriteService;
    private final SharedBankService sharedBankService;
    private final VolcEngineService volcEngineService;
    private final Executor sseTaskExecutor;

    /**
     * AI生成题库
     */
    @PostMapping("/generate")
    public Result<List<QuestionCardDTO>> generateAIBank(@RequestBody Map<String, Object> request,
                                                         HttpServletRequest httpRequest) {
        String topic = (String) request.get("topic");
        if (topic == null || topic.trim().isEmpty()) {
            return Result.error("主题不能为空");
        }
        
        // 获取可选参数
        Integer cardCount = request.get("cardCount") != null ? (Integer) request.get("cardCount") : 5;
        String difficulty = request.get("difficulty") != null ? (String) request.get("difficulty") : "中等";
        String language = request.get("language") != null ? (String) request.get("language") : "zh";
        String scenario = request.get("scenario") != null ? (String) request.get("scenario") : "";
        
        // 从请求中获取用户ID(由JWT拦截器设置)
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        log.info("🔥 收到AI生成题库请求: topic={}, scenario={}, cardCount={}, difficulty={}, language={}", 
                topic, scenario, cardCount, difficulty, language);
                
        List<QuestionCardDTO> cards = questionBankService.generateAIBank(topic, cardCount, difficulty, language, userId, scenario);
        return Result.success(cards);
    }

    /**
     * AI流式生成题库
     */
    @GetMapping("/generate-stream")
    public SseEmitter generateAIBankStream(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") Integer cardCount,
            @RequestParam(defaultValue = "中等") String difficulty,
            @RequestParam(defaultValue = "zh") String language,
            @RequestParam(defaultValue = "false") Boolean withImages,
            @RequestParam(defaultValue = "") String scenario,
            HttpServletRequest request) {
        
        log.info("🔥 收到流式生成请求: topic={}, cardCount={}, difficulty={}, language={}, withImages={}, scenario={}", 
                topic, cardCount, difficulty, language, withImages, scenario);
        
        // 获取用户信息
        Long userId = (Long) request.getAttribute("userId");
        
        // 创建SSE发射器,超时时间设置为10分钟（与application.yml中的mvc.async.request-timeout保持一致）
        SseEmitter emitter = new SseEmitter(600000L);
        
        // 添加客户端断开连接的监听器
        emitter.onCompletion(() -> {
            log.info("SSE连接已正常完成");
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE连接已超时");
            emitter.complete();
        });
        
        emitter.onError((ex) -> {
            log.error("SSE连接发生错误", ex);
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE连接关闭失败", e);
            }
        });
        
        // 在异步任务执行前先获取userId,避免在异步线程中无法访问request attribute
        final Long finalUserId = userId;
        
        // 使用异步任务执行生成操作,避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 流式生成卡片内容(此方法内部会等待所有图片生成完成后才返回)
                String cardsJson = volcEngineService.generateCardsStream(topic, cardCount, difficulty, language, withImages, scenario, emitter);
                
                // 如果生成成功,保存到数据库
                if (cardsJson != null && !cardsJson.trim().isEmpty()) {
                    List<QuestionCardDTO> savedCards = questionBankService.saveStreamGeneratedCards(
                        cardsJson, topic, difficulty, language, finalUserId, scenario);
                    
                    // 发送保存成功的卡片数据（包含真实ID）
                    ObjectMapper objectMapper = new ObjectMapper();
                    String savedCardsJson = objectMapper.writeValueAsString(savedCards);
                    emitter.send(SseEmitter.event()
                        .name("saved")
                        .data(savedCardsJson));
                    
                    log.info("✅ 流式生成并保存成功: {} 张卡片", savedCards.size());
                }
                
                // 确保所有数据（包括图片）都已发送完成后，再发送done信号并关闭连接
                log.info("📡 所有数据已发送完成，准备关闭SSE连接");
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
                log.info("✅ SSE连接已正常关闭");
                
            } catch (Exception e) {
                log.error("❌ 流式生成或保存失败", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("生成或保存失败: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.error("发送错误消息失败", ex);
                }
            }
        }, sseTaskExecutor).exceptionally(ex -> {
            log.error("❌ 执行生成任务时发生异常", ex);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("生成失败: " + ex.getMessage()));
                emitter.completeWithError(ex);
            } catch (IOException ioEx) {
                log.error("发送错误消息失败", ioEx);
            }
            return null;
        });
        
        return emitter;
    }

    /**
     * AI批量生成题库(非流式,适用于外语内容)
     */
    @GetMapping("/generate-batch")
    public Result<String> generateAIBankBatch(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") Integer cardCount,
            @RequestParam(defaultValue = "中等") String difficulty,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "false") Boolean withImages,
            @RequestParam(defaultValue = "") String scenario) {
        
        log.info("🔥 收到批量生成请求: topic={}, cardCount={}, difficulty={}, language={}, withImages={}, scenario={}", 
                topic, cardCount, difficulty, language, withImages, scenario);
        
        try {
            // 调用非流式方法,直接返回完整JSON
            String cardsJson = volcEngineService.generateCards(topic, cardCount, difficulty, language, scenario);
            
            // 如果需要生成图片描述
            if (withImages) {
                cardsJson = questionBankService.addImageDescriptions(cardsJson);
            }
            
            log.info("✅ 批量生成成功,返回JSON长度: {}", cardsJson.length());
            return Result.success(cardsJson);
        } catch (Exception e) {
            log.error("❌ 批量生成失败", e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }

    /**
     * 创建自定义题库
     */
    @PostMapping("/create")
    public Result<QuestionBankDTO> createCustomBank(@RequestBody Map<String, Object> request,
                                                     HttpServletRequest httpRequest) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String topic = (String) request.get("topic");
        String difficulty = (String) request.get("difficulty");
        String language = (String) request.get("language");
        
        if (name == null || name.trim().isEmpty()) {
            return Result.error("题库名称不能为空");
        }
        if (topic == null || topic.trim().isEmpty()) {
            return Result.error("主题不能为空");
        }
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        QuestionBankDTO bank = questionBankService.createCustomBank(name, description, topic, difficulty, language, userId);
        return Result.success(bank);
    }

    /**
     * 获取系统推荐题库（支持分页）
     */
    @GetMapping("/system")
    public Result<PageResponse<QuestionBankDTO>> getSystemBanks(
            @RequestParam String topic,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResponse<QuestionBankDTO> response = questionBankService.getSystemBanks(topic, page, pageSize);
        return Result.success(response);
    }

    /**
     * 获取用户自定义题库（支持分页）
     */
    @GetMapping("/custom")
    public Result<PageResponse<QuestionBankDTO>> getUserCustomBanks(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PageResponse<QuestionBankDTO> response = questionBankService.getUserCustomBanks(userId, page, pageSize);
        return Result.success(response);
    }

    /**
     * 根据ID获取题库详情
     */
    @GetMapping("/{bankId}")
    public Result<QuestionBankDTO> getBankById(
            @PathVariable Long bankId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            QuestionBankDTO bank = questionBankService.getBankById(bankId, userId);
            if (bank == null) {
                return Result.error("题库不存在或已被删除");
            }
            return Result.success(bank);
        } catch (Exception e) {
            log.error("获取题库详情失败: bankId={}", bankId, e);
            return Result.error("获取题库详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定题库的卡片
     */
    @GetMapping("/{bankId}/cards")
    public Result<List<QuestionCardDTO>> getBankCards(
            @PathVariable Long bankId,
            HttpServletRequest httpRequest) {
        
        List<QuestionCardDTO> cards = questionBankService.getBankCards(bankId);
        return Result.success(cards);
    }

    /**
     * 上传自定义文档生成题库
     */
    @PostMapping("/upload")
    public Result<List<QuestionCardDTO>> uploadCustomBank(
            @RequestParam("file") MultipartFile file,
            @RequestParam("topic") String topic,
            HttpServletRequest httpRequest) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        if (topic == null || topic.trim().isEmpty()) {
            return Result.error("主题不能为空");
        }
        
        // 从请求中获取用户ID(由JWT拦截器设置)
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        try {
            List<QuestionCardDTO> cards = questionBankService.uploadCustomBank(file, topic, userId);
            return Result.success(cards);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 高级搜索题库（分页）
     */
    @PostMapping("/search")
    public Result<PageResponse<QuestionBankDTO>> searchBanks(
            @RequestBody BankSearchRequest request,
            HttpServletRequest httpRequest) {
        PageResponse<QuestionBankDTO> response = questionBankService.searchBanks(request);
        return Result.success(response);
    }
    
    /**
     * 增加题库浏览次数
     */
    @PostMapping("/{bankId}/view")
    public Result<Void> incrementViewCount(@PathVariable Long bankId) {
        questionBankService.incrementViewCount(bankId);
        return Result.success(null);
    }
    
    /**
     * 更新题库信息
     */
    @PutMapping("/{bankId}")
    public Result<Void> updateBank(
            @PathVariable Long bankId,
            @RequestBody QuestionBankDTO bankDTO,
            HttpServletRequest httpRequest) {
        try {
            // 从请求中获取租户ID和用户ID(由JWT拦截器设置)
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            questionBankService.updateBank(bankId, bankDTO, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除题库
     */
    @DeleteMapping("/{bankId}")
    public Result<Void> deleteBank(
            @PathVariable Long bankId,
            HttpServletRequest httpRequest) {
        try {
            // 从请求中获取租户ID和用户ID(由JWT拦截器设置)
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            questionBankService.deleteBank(bankId, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除单个题库卡片
     */
    @DeleteMapping("/cards/{cardId}")
    public Result<Void> deleteCard(
            @PathVariable Long cardId,
            HttpServletRequest httpRequest) {
        try {
            // 从请求中获取租户ID和用户ID(由JWT拦截器设置)
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            questionBankService.deleteCard(cardId, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 收藏题库
     */
    @PostMapping("/{bankId}/favorite")
    public Result<Void> addFavorite(
            @PathVariable Long bankId,
            HttpServletRequest httpRequest) {
        try {
            // 从请求中获取租户ID和用户ID(由JWT拦截器设置)
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            bankFavoriteService.addFavorite(userId, bankId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 取消收藏
     */
    @DeleteMapping("/{bankId}/favorite")
    public Result<Void> removeFavorite(
            @PathVariable Long bankId,
            HttpServletRequest httpRequest) {
        try {
            // 从请求中获取租户ID和用户ID(由JWT拦截器设置)
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            bankFavoriteService.removeFavorite(userId, bankId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 检查是否已收藏
     */
    @GetMapping("/{bankId}/favorite/check")
    public Result<Boolean> checkFavorite(
            @PathVariable Long bankId,
            HttpServletRequest httpRequest) {
        // 从请求中获取租户ID和用户ID(由JWT拦截器设置)
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        boolean isFavorited = bankFavoriteService.isFavorited(userId, bankId);
        return Result.success(isFavorited);
    }
    
    /**
     * 获取用户收藏的题库
     */
    @GetMapping("/favorites")
    public Result<List<Long>> getUserFavorites(HttpServletRequest httpRequest) {
        // 从请求中获取租户ID和用户ID(由JWT拦截器设置)
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        List<Long> bankIds = bankFavoriteService.getUserFavoriteBankIds(userId);
        return Result.success(bankIds);
    }
    
    /**
     * 批量添加卡片到题库
     */
    @PostMapping("/add-cards")
    public Result<List<QuestionCardDTO>> addCardsToBank(
            @RequestBody AddCardsRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                log.warn("未找到用户ID，请确保已正确登录并携带有效的JWT Token");
                return Result.error("未找到用户ID，请重新登录");
            }
            
            List<QuestionCardDTO> cards = questionBankService.addCardsToBank(
                request.getTargetBankId(), 
                request.getCardIds(), 
                userId,
                request.getSourceBankId()
            );
            return Result.success(cards);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 批量添加卡片内容到题库（用于AI生成的临时卡片）
     */
    @PostMapping("/add-card-contents")
    public Result<List<QuestionCardDTO>> addCardContentsToBank(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                log.warn("未找到用户ID，请确保已正确登录并携带有效的JWT Token");
                return Result.error("未找到用户ID，请重新登录");
            }
            Long targetBankId = Long.valueOf(request.get("targetBankId").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, String>> cardContents = (List<Map<String, String>>) request.get("cardContents");
            
            List<QuestionCardDTO> cards = questionBankService.addCardContentsToBank(
                targetBankId, 
                cardContents, 
                userId
            );
            return Result.success(cards);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导出题库卡片为Excel
     */
    @GetMapping("/{bankId}/export")
    public void exportBankToExcel(
            @PathVariable Long bankId,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                log.warn("未找到用户ID，请确保已正确登录并携带有效的JWT Token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("未找到用户ID，请重新登录");
                return;
            }
            questionBankService.exportBankToExcel(bankId, userId, response);
        } catch (Exception e) {
            log.error("导出题库失败: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 从Excel导入题库卡片
     */
    @PostMapping("/import")
    public Result<QuestionBankDTO> importBankFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetBankId", required = false) Long targetBankId,
            @RequestParam(value = "bankName", required = false) String bankName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "language", required = false) String language,
            HttpServletRequest request) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
                return Result.error("仅支持.xlsx格式的Excel文件");
            }

            // 获取用户信息
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                log.warn("未找到用户ID，请确保已正确登录并携带有效的JWT Token");
                return Result.error("未找到用户ID，请重新登录");
            }

            // 执行导入
            QuestionBankDTO bankDTO = questionBankService.importBankFromExcel(file, targetBankId, bankName, description, difficulty, language, userId);
            return Result.success(bankDTO);
        } catch (Exception e) {
            log.error("导入题库失败: {}", e.getMessage(), e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }
    
    /**
     * 新增单个卡片到题库
     */
    @PostMapping("/{bankId}/card")
    public Result<QuestionCardDTO> addCard(
            @PathVariable Long bankId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            // 首先检查请求体中是否手动传递了userId参数
            Long userId = null;
            String manualUserId = request.get("userId");
            if (manualUserId != null && !manualUserId.isEmpty()) {
                try {
                    userId = Long.parseLong(manualUserId);
                    log.info("手动传递userId: {}", userId);
                } catch (NumberFormatException e) {
                    log.warn("手动传递的userId格式无效: {}", manualUserId);
                }
            }
            
            // 如果没有手动传递userId，则从JWT拦截器设置的attribute中获取
            if (userId == null) {
                userId = (Long) httpRequest.getAttribute("userId");
                if (userId == null) {
                    log.warn("未找到用户ID，请确保已正确登录并携带有效的JWT Token");
                    return Result.error("未找到用户ID，请重新登录");
                }
            }
            
            String question = request.get("question");
            String answer = request.get("answer");
            String questionImage = request.get("questionImage");
            String answerImage = request.get("answerImage");
            
            if (question == null || question.trim().isEmpty()) {
                return Result.error("问题不能为空");
            }
            if (answer == null || answer.trim().isEmpty()) {
                return Result.error("答案不能为空");
            }
            
            QuestionCardDTO card = questionBankService.addCard(bankId, question, answer, questionImage, answerImage, userId);
            return Result.success(card);
        } catch (Exception e) {
            log.error("新增卡片失败: {}", e.getMessage(), e);
            return Result.error("新增失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新卡片
     */
    @PutMapping("/cards/{cardId}")
    public Result<QuestionCardDTO> updateCard(
            @PathVariable Long cardId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            String question = (String) request.get("question");
            String answer = (String) request.get("answer");
            String questionImage = (String) request.get("questionImage");
            String answerImage = (String) request.get("answerImage");
            
            QuestionCardDTO card = questionBankService.updateCard(cardId, userId, question, answer, questionImage, answerImage);
            return Result.success(card);
        } catch (Exception e) {
            log.error("更新卡片失败: {}", e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    

    
    /**
     * 获取用户的分享记录
     */
    @GetMapping("/shared-records")
    public Result<List<Map<String, Object>>> getSharedRecords(HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            List<Map<String, Object>> sharedBanks = sharedBankService.getUserShares(userId);
            return Result.success(sharedBanks);
        } catch (Exception e) {
            log.error("获取分享记录失败: {}", e.getMessage(), e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 通过分享码获取题库
     */
    @GetMapping("/shared/{shareCode}")
    public Result<QuestionBankDTO> getByShareCode(
            @PathVariable String shareCode,
            HttpServletRequest httpRequest) {
        try {
            log.info("通过分享码获取题库: shareCode={}", shareCode);
            
            // 先获取分享信息
            SharedBank sharedBank = sharedBankService.getByShareCode(shareCode);
            
            if (sharedBank == null) {
                return Result.error("分享码不存在或已失效");
            }
            
            // 记录浏览
            Long userId = (Long) httpRequest.getAttribute("userId");
            String clientIp = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            String deviceType = getDeviceType(userAgent);
            
            try {
                sharedBankService.recordView(sharedBank.getId(), userId, clientIp, userAgent, deviceType);
            } catch (Exception e) {
                // 记录浏览失败不影响返回结果
                log.warn("记录浏览失败: {}", e.getMessage());
            }
            
            // 获取完整题库信息
            QuestionBankDTO bankDTO = questionBankService.getBankById(sharedBank.getBankId(), userId);
            if (bankDTO == null) {
                return Result.error("题库不存在");
            }
            
            return Result.success(bankDTO);
        } catch (Exception e) {
            log.error("通过分享码获取题库失败: shareCode={}, error={}", shareCode, e.getMessage(), e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备类型
     */
    private String getDeviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}