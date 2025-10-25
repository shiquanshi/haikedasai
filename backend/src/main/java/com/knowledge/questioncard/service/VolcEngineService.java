package com.knowledge.questioncard.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knowledge.questioncard.config.VolcEngineConfig;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;                                              
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 火山引擎AI服务
 */
@Slf4j
@Service
public class VolcEngineService {
    
    @Autowired
    private VolcEngineConfig volcEngineConfig;
    
    @Autowired
    private MinioService minioService;
    
    @Autowired
    private com.knowledge.questioncard.mapper.QuestionCardMapper questionCardMapper;
    
    @Autowired
    private ArkService arkService;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Executor sseTaskExecutor;
    
    // 构造函数中初始化RestTemplate并配置HTTP连接池
    @Autowired
    public VolcEngineService(Executor sseTaskExecutor, ObjectMapper objectMapper) {
        // 1. 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);                    // 最大总连接数
        connectionManager.setDefaultMaxPerRoute(20);           // 每个路由（域名）的最大连接数
        
        // 2. 配置请求超时
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))  // 从连接池获取连接超时
                .setConnectTimeout(Timeout.ofSeconds(60))            // 连接超时
                .setResponseTimeout(Timeout.ofSeconds(180))          // 响应超时（读取超时）
                .build();
        
        // 3. 创建 HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(Timeout.ofSeconds(30))     // 清理空闲连接30秒
                .build();
        
        // 4. 创建请求工厂
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        
        // 4. 初始化 RestTemplate
        this.restTemplate = new RestTemplate(factory);
        this.sseTaskExecutor = sseTaskExecutor;
        this.objectMapper = objectMapper;
        
        log.info("VolcEngineService initialized with HTTP connection pool - MaxTotal: 200, MaxPerRoute: 20");
    }
    
    /**
     * 批量生成问答卡片内容
     * 
     * @param topic 主题
     * @param cardCount 卡片数量
     * @param difficulty 难度
     * @param language 语言
     * @param scenario 应用场景
     * @return 生成的卡片内容JSON字符串
     */
    public String generateCards(String topic, Integer cardCount, String difficulty, String language, String scenario) {
        try {
            
            // 构建提示词
            String prompt = buildPrompt(topic, cardCount, difficulty, language, scenario);
            
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content("你是一位脑洞大开的知识魔法师🧙‍♂️,专门用奇奇怪怪的方式折磨...啊不,是启发学习者的大脑!你的任务是先出一道让人抓耳挠腮的刁钻问题,然后再用超级幽默、充满梗和表情包风格的方式把答案讲明白。记住:问题要像谜语一样烧脑,答案要像段子一样好笑!")
                    .build());
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(prompt)
                    .build());
            
            // 构建请求
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(volcEngineConfig.getModel())
                    .messages(messages)
                    .build();
            
            // 发送请求并获取响应
            String response = arkService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent()
                    .toString();
            
            log.info("AI生成卡片成功: topic={}, cardCount={}, difficulty={}, language={}", 
                    topic, cardCount, difficulty, language);
            
            return response;
            
        } catch (Exception e) {
            log.error("调用火山引擎API失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage());
        }
    }
    

    
    /**
     * 流式生成问答卡片内容
     * 
     * @param topic 主题
     * @param cardCount 卡片数量
     * @param difficulty 难度
     * @param language 语言
     * @param withImages 是否生成图片描述
     * @param scenario 应用场景
     * @param emitter SSE发射器
     * @return 累积的完整JSON内容
     */
    public String generateCardsStream(String topic, Integer cardCount, String difficulty, 
                                   String language, Boolean withImages, String scenario, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        log.info("⏱️ [计时开始] 卡片生成任务启动 - 主题:{}, 数量:{}, 难度:{}", topic, cardCount, difficulty);
        
        try {

            
            // 构建提示词
            long promptStartTime = System.currentTimeMillis();
            String prompt = buildPrompt(topic, cardCount, difficulty, language, scenario);
            log.info("⏱️ [计时] 提示词构建耗时: {}ms", System.currentTimeMillis() - promptStartTime);
            
            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content("你是一位脑洞大开的知识魔法师🧙‍♂️,专门用奇奇怪怪的方式折磨...啊不,是启发学习者的大脑!你的任务是先出一道让人抓耳挠腮的刁钻问题,然后再用超级幽默、充满梗和表情包风格的方式把答案讲明白。记住:问题要像谜语一样烧脑,答案要像段子一样好笑!")
                    .build());
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(prompt)
                    .build());
            
            // 构建请求
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(volcEngineConfig.getModel())
                    .messages(messages)
                    .build();
            
            // 发送流式请求 - 使用真正的异步流式处理
            long streamStartTime = System.currentTimeMillis();
            log.info("⏱️ [计时开始] 大模型流式调用开始");
            
            // 使用StringBuilder累积所有增量片段
            final StringBuilder accumulatedContent = new StringBuilder();
            
            // 使用CountDownLatch等待流式完成
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> errorRef = new AtomicReference<>();
            final AtomicLong firstTokenTime = new AtomicLong(0);
            final AtomicInteger tokenCount = new AtomicInteger(0);
            
            // 添加连接状态检查
            final AtomicBoolean connectionActive = new AtomicBoolean(true);
            
            // 添加连接超时和错误监听器（onCompletion监听器已移除，因为正常完成由Controller控制）
            emitter.onTimeout(() -> {
                connectionActive.set(false);
                latch.countDown();
                log.warn("⚠️ SSE连接已超时");
            });
            
            emitter.onError((ex) -> {
                connectionActive.set(false);
                latch.countDown();
                log.error("❌ SSE连接发生错误", ex);
            });
            
            arkService.streamChatCompletion(request)
                    .subscribe(
                        response -> {
                            try {
                                if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                                    // 获取本次返回的增量内容
                                    String incrementalContent = response.getChoices().get(0).getMessage().getContent().toString();
                                    // 获取思考过程内容（如果有）
                                    String reasoningContent = response.getChoices().get(0).getMessage().getReasoningContent();
                                    
                                    if (incrementalContent != null && !incrementalContent.isEmpty()) {
                                        // 记录首token时间
                                        if (firstTokenTime.get() == 0) {
                                            firstTokenTime.set(System.currentTimeMillis());
                                            log.info("⏱️ [计时] 首个token返回耗时: {}ms", firstTokenTime.get() - streamStartTime);
                                        }
                                        tokenCount.incrementAndGet();
                                        
                                        // 累积原始内容到StringBuilder中
                                        accumulatedContent.append(incrementalContent);
                                        
                                        // 直接发送增量内容，无需等待完整单词边界
                                        log.debug("[SSE流式发送] 增量长度={}, 累积总长={}",
                                                incrementalContent.length(),
                                                accumulatedContent.length());

                                        // 检查连接是否仍然活跃
                                if (!connectionActive.get()) {
                                    log.warn("连接已断开，停止发送数据");
                                    latch.countDown();
                                    return;
                                }
                                
                                // 直接发送增量内容
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(incrementalContent));
                            }
                                    
                                    // 如果有思考过程，作为单独事件发送
                                    if (reasoningContent != null && !reasoningContent.isEmpty()) {
                                        log.debug("[SSE流式发送] 思考过程长度={}", reasoningContent.length());
                                        // 检查连接是否仍然活跃
                                    if (!connectionActive.get()) {
                                        log.warn("连接已断开，停止发送思考过程");
                                        latch.countDown();
                                        return;
                                    }
                                    
                                    emitter.send(SseEmitter.event()
                                            .name("thinking")
                                            .data(reasoningContent));
                                }
                                }
                            } catch (Exception e) {
                            log.error("处理流式响应失败", e);
                            errorRef.set(e);
                            connectionActive.set(false);
                            latch.countDown();
                        }
                        },
                        error -> {
                            log.error("流式调用火山引擎API失败: {}", error.getMessage());
                            errorRef.set(error);
                            connectionActive.set(false);
                            try {
                                // 检查连接是否仍然活跃
                                if (connectionActive.get()) {
                                    emitter.send(SseEmitter.event().name("error").data("生成失败: " + error.getMessage()));
                                    emitter.complete();
                                }
                            } catch (IOException e) {
                                log.error("发送错误事件失败", e);
                            }
                            latch.countDown();
                        },
                        () -> {
                            try {
                                long streamEndTime = System.currentTimeMillis();
                                log.info("⏱️ [计时] 大模型流式调用完成 - 总耗时:{}ms, token数:{}, 平均速度:{} tokens/s",
                                    streamEndTime - streamStartTime,
                                    tokenCount.get(),
                                    tokenCount.get() * 1000.0 / (streamEndTime - streamStartTime));
                            } finally {
                                latch.countDown();
                            }
                        }
                    );
            
            // 等待流式完成(最多5分钟)
            try {
                if (!latch.await(5, TimeUnit.MINUTES)) {
                    log.error("流式响应超时");
                    try {
                        emitter.send(SseEmitter.event().name("error").data("生成超时"));
                    } catch (IOException e) {
                        log.error("发送超时错误消息失败", e);
                    }
                    emitter.complete();
                    return null;
                }
            } catch (InterruptedException e) {
                log.error("等待流式响应被中断", e);
                Thread.currentThread().interrupt();
                try {
                    emitter.send(SseEmitter.event().name("error").data("生成被中断"));
                } catch (IOException ex) {
                    log.error("发送中断错误消息失败", ex);
                }
                emitter.complete();
                return null;
            }
            
            // 检查是否有错误
            if (errorRef.get() != null) {
                log.error("流式处理出错", errorRef.get());
                return null;
            }

            // 如果需要生成图片描述
            if (withImages != null && withImages) {
                try {
                    long imageStartTime = System.currentTimeMillis();
                    log.info("⏱️ [计时开始] 图片生成任务启动");
                    emitter.send(SseEmitter.event().name("status").data("正在生成图片..."));
                    
                    String cardsJson = accumulatedContent.toString();
                    
                    // 清理JSON文本中的控制字符
                    cardsJson = cardsJson.replaceAll("\\p{Cntrl}", " ");
                    log.info("清理后的JSON内容: {}", cardsJson);
                    
                    // 解析JSON数组
                    ObjectMapper objectMapper = new ObjectMapper();
                    ArrayNode cardsArray = (ArrayNode) objectMapper.readTree(cardsJson);
                    
                    if (cardsArray.isArray()) {
                        int totalCards = cardsArray.size();
                        int currentCard = 0;
                        
                        // 🚀 优化：使用CompletableFuture并行生成图片，每完成一张立即发送
                        long parallelStartTime = System.currentTimeMillis();
                        log.info("⏱️ [计时] 开始并行处理{}张卡片的图片", totalCards);
                        
                        // 收集所有异步任务和图片URL
                        List<CompletableFuture<Map<String, String>>> allTasks = new ArrayList<>();
                        
                        for (JsonNode cardNode : cardsArray) {
                            final int cardIndex = ++currentCard;
                            
                            // 为每张卡片创建异步任务，完成后立即发送，并返回图片URL
                            CompletableFuture<Map<String, String>> cardTask = CompletableFuture.supplyAsync(() -> {
                                try {
                                    log.info("📝 处理第 {}/{} 张卡片", cardIndex, totalCards);
                                    
                                    // 安全获取字段值
                                    JsonNode questionNode = cardNode.get("question");
                                    JsonNode answerNode = cardNode.get("answer");
                                    
                                    if (questionNode == null || answerNode == null) {
                                        log.warn("卡片字段缺失，跳过: {}", cardNode.toString());
                                        return new HashMap<>();
                                    }
                                    
                                    String question = questionNode.asText();
                                    String answer = answerNode.asText();
                                    
                                    // 并行生成问题和答案的图片
                                    CompletableFuture<String> questionImageFuture = CompletableFuture.supplyAsync(() -> {
                                        try {
                                            log.info("🎨 生成问题图片描述: {}", question.substring(0, Math.min(50, question.length())));
                                            String questionDesc = generateImageDescription(question);
                                            if (questionDesc != null) {
                                                log.info("🖼️ 根据描述生成问题图片: {}", questionDesc);
                                                String img = generateImage(questionDesc, 1, "2048x2048");
                                                if (img == null) {
                                                    log.error("❌ 问题图片生成失败");
                                                }
                                                return img;
                                            } else {
                                                log.warn("⚠️ 问题图片描述生成失败");
                                                return null;
                                            }
                                        } catch (Exception e) {
                                            log.error("问题图片生成异常", e);
                                            return null;
                                        }
                                    }, sseTaskExecutor);
                                    
                                    CompletableFuture<String> answerImageFuture = CompletableFuture.supplyAsync(() -> {
                                        try {
                                            log.info("🎨 生成答案图片描述: {}", answer.substring(0, Math.min(50, answer.length())));
                                            String answerDesc = generateImageDescription(answer);
                                            if (answerDesc != null) {
                                                log.info("🖼️ 根据描述生成答案图片: {}", answerDesc);
                                                String img = generateImage(answerDesc, 1, "2048x2048");
                                                if (img == null) {
                                                    log.error("❌ 答案图片生成失败");
                                                }
                                                return img;
                                            } else {
                                                log.warn("⚠️ 答案图片描述生成失败");
                                                return null;
                                            }
                                        } catch (Exception e) {
                                            log.error("答案图片生成异常", e);
                                            return null;
                                        }
                                    }, sseTaskExecutor);
                                    
                                    // 等待两个图片都生成完成，设置统一超时机制(3分钟)
                                    String questionImage = null;
                                    String answerImage = null;
                                    try {
                                        questionImage = questionImageFuture.orTimeout(180, TimeUnit.SECONDS).join();
                                    } catch (CompletionException e) {
                                        log.warn("问题图片生成超时或失败（180秒），跳过", e);
                                    }
                                    try {
                                        answerImage = answerImageFuture.orTimeout(180, TimeUnit.SECONDS).join();
                                    } catch (CompletionException e) {
                                        log.warn("答案图片生成超时或失败（180秒），跳过", e);
                                    }
                                    
                                    log.info("✅ 第 {}/{} 张卡片图片处理完成", cardIndex, totalCards);
                                    
                                    // 立即发送这张卡片的图片数据
                                    try {
                                        Map<String, Object> cardImageData = new HashMap<>();
                                        cardImageData.put("question", question);
                                        cardImageData.put("answer", answer);
                                        cardImageData.put("questionImage", questionImage);
                                        cardImageData.put("answerImage", answerImage);
                                        cardImageData.put("index", cardIndex - 1); // 索引从0开始
                                        
                                        // 保留原始ID（如果存在）
                                        JsonNode idNode = cardNode.get("id");
                                        if (idNode != null) {
                                            cardImageData.put("id", idNode.asLong());
                                        }
                                        
                                        String cardJson = objectMapper.writeValueAsString(cardImageData);
                                        emitter.send(SseEmitter.event().name("image_single").data(cardJson));
                                        log.info("📤 已发送第 {}/{} 张卡片的图片数据", cardIndex, totalCards);
                                    } catch (IOException e) {
                                        log.error("发送卡片图片数据失败", e);
                                    }
                                    
                                    // 返回图片URL
                                    Map<String, String> imageUrls = new HashMap<>();
                                    imageUrls.put("questionImage", questionImage);
                                    imageUrls.put("answerImage", answerImage);
                                    imageUrls.put("index", String.valueOf(cardIndex - 1));
                                    return imageUrls;
                                    
                                } catch (Exception e) {
                                    log.error("处理卡片异常", e);
                                    return new HashMap<>();
                                }
                            }, sseTaskExecutor);
                            
                            allTasks.add(cardTask);
                        }
                        
                        // 等待所有图片生成任务完成，并更新原始JSON
                        try {
                            List<Map<String, String>> imageResults = allTasks.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList());
                            log.info("✅ 所有卡片图片生成完成");
                            
                            // 将图片URL添加到原始JSON中
                            for (Map<String, String> imageResult : imageResults) {
                                if (imageResult != null) {
                                    int index = Integer.parseInt(imageResult.get("index"));
                                    ObjectNode cardNode = (ObjectNode) cardsArray.get(index);
                                    cardNode.put("questionImage", imageResult.get("questionImage"));
                                    cardNode.put("answerImage", imageResult.get("answerImage"));
                                    log.info("已将图片URL添加到第{}张卡片的JSON中", index + 1);
                                }
                            }
                            
                            // 更新accumulatedContent为包含图片URL的完整JSON
                            accumulatedContent.setLength(0);
                            accumulatedContent.append(objectMapper.writeValueAsString(cardsArray));
                            log.info("已更新返回JSON，包含图片URL");
                        } catch (Exception e) {
                            log.error("等待图片生成任务时发生异常", e);
                        }
                        
                        long parallelEndTime = System.currentTimeMillis();
                        log.info("⏱️ [计时] 所有卡片图片任务已完成 - 总耗时:{}ms", parallelEndTime - parallelStartTime);
                        log.info("⏱️ [计时总结] 整个卡片生成流程完成 - 总耗时:{}ms", parallelEndTime - startTime);
                    }
                } catch (Exception e) {
                    log.error("生成图片描述失败", e);
                    // 即使图片描述生成失败,也继续完成流程
                }
            }
            
            // 不再发送done信号，而是返回累积的JSON内容
            log.info("AI流式生成卡片完成: topic={}, cardCount={}, difficulty={}, language={}, withImages={}", 
                    topic, cardCount, difficulty, language, withImages);
            
            return accumulatedContent.toString();
            
        } catch (Exception e) {
            log.error("流式调用火山引擎API失败", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("AI生成失败: " + e.getMessage()));
                emitter.completeWithError(e);
            } catch (IOException ex) {
                log.error("发送错误消息失败", ex);
            }
            return null;
        }
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(String topic, Integer cardCount, String difficulty, String language, String scenario) {
        // 处理各种可能的语言代码和名称
        String languageName;
        boolean isEnglish = false;
        boolean isForeignLanguage = false;
        
        if (language == null || language.isEmpty()) {
            languageName = "中文"; // 默认中文
        } else {
            String lang = language.toLowerCase().trim();
            if (lang.equals("en") || lang.equals("英文") || lang.equals("english")) {
                languageName = "English";
                isEnglish = true;
                isForeignLanguage = true;
            } else if (lang.equals("日语") || lang.equals("japanese") || lang.equals("ja")) {
                languageName = "Japanese";
                isForeignLanguage = true;
            } else if (lang.equals("韩语") || lang.equals("korean") || lang.equals("ko")) {
                languageName = "Korean";
                isForeignLanguage = true;
            } else if (lang.equals("法语") || lang.equals("french") || lang.equals("fr")) {
                languageName = "French";
                isForeignLanguage = true;
            } else if (lang.equals("德语") || lang.equals("german") || lang.equals("de")) {
                languageName = "German";
                isForeignLanguage = true;
            } else if (lang.equals("西班牙语") || lang.equals("spanish") || lang.equals("es")) {
                languageName = "Spanish";
                isForeignLanguage = true;
            } else if (lang.equals("俄语") || lang.equals("russian") || lang.equals("ru")) {
                languageName = "Russian";
                isForeignLanguage = true;
            } else {
                languageName = "中文";
            }
        }
        
        // 根据语言选择不同的提示词模板
        if (isEnglish) {
            return buildEnglishPrompt(topic, cardCount, difficulty, scenario);
        } else if (isForeignLanguage) {
            return buildForeignLanguagePrompt(topic, cardCount, difficulty, scenario, languageName);
        } else {
            return buildChinesePrompt(topic, cardCount, difficulty, scenario);
        }
    }
    
    /**
     * 构建英文提示词(精简版)
     */
    private String buildEnglishPrompt(String topic, Integer cardCount, String difficulty, String scenario) {
        StringBuilder promptBuilder = new StringBuilder(String.format(
            "Generate %d creative flashcards about '%s' (difficulty: %s).\n" +
            "\n" +
            "Requirements:\n" +
            "1. Each card must have a tricky, thought-provoking question\n" +
            "2. Answer should be accurate but explained in a fun, memorable way\n" +
            "3. Use emojis and creative metaphors to make it engaging\n" +
            "4. All content must be in English with proper spacing between words\n",
            cardCount, topic, difficulty
        ));
        
        // 如果提供了场景信息，添加到提示词中
        if (scenario != null && !scenario.trim().isEmpty()) {
            promptBuilder.append(String.format("5. Tailor questions and answers to the application scenario: '%s'\n", scenario.trim()));
        }
        
        promptBuilder.append(
            "\n" +
            "Output Format (JSON only):\n" +
            "[{\"question\":\"...\",\"answer\":\"...\"}]\n" +
            "\n" +
            "CRITICAL: Every English word MUST be separated by spaces. No word concatenation allowed!"
        );
        
        return promptBuilder.toString();
    }
    
    /**
     * 构建中文提示词(超精简版 - 强调趣味性)
     */
    private String buildChinesePrompt(String topic, Integer cardCount, String difficulty, String scenario) {
        StringBuilder promptBuilder = new StringBuilder(String.format(
            "🎯 生成%d张'%s'主题学习卡片(难度:%s)\n",
            cardCount, topic, difficulty
        ));
        
        // 如果提供了场景信息，添加到提示词中
        if (scenario != null && !scenario.trim().isEmpty()) {
            promptBuilder.append(String.format("🎭 应用场景:'%s'\n", scenario.trim()));
        }
        
        promptBuilder.append(
            "\n" +
            "💥 出题风格:用最刁钻、最骚的角度提问!让人看到就想:卧槽还能这么问?!\n" +
            "🎉 答案风格:用表情包🤪、网络梗、沙雕比喻讲明白!让人秒懂还笑出声!\n" +
            "✅ 底线:知识必须正确,但表达必须骚气!\n" +
            "\n" +
            "📦 只返回JSON:[{\"question\":\"...\",\"answer\":\"...\"}]\n" +
            "\n" +
            "开整!🚀"
        );
        
        return promptBuilder.toString();
    }
    
    /**
     * 构建外语提示词（日语、韩语、法语、德语、西班牙语、俄语）
     */
    private String buildForeignLanguagePrompt(String topic, Integer cardCount, String difficulty, String scenario, String languageName) {
        StringBuilder promptBuilder = new StringBuilder(String.format(
            "Generate %d creative flashcards about '%s' (difficulty: %s) in %s.\n" +
            "\n" +
            "Requirements:\n" +
            "1. Each card must have a tricky, thought-provoking question in %s\n" +
            "2. Answer should be accurate but explained in a fun, memorable way in %s\n" +
            "3. Use emojis and creative expressions to make it engaging\n" +
            "4. All content (question and answer) must be in %s\n",
            cardCount, topic, difficulty, languageName, languageName, languageName, languageName
        ));
        
        // 如果提供了场景信息，添加到提示词中
        if (scenario != null && !scenario.trim().isEmpty()) {
            promptBuilder.append(String.format("5. Tailor questions and answers to the application scenario: '%s'\n", scenario.trim()));
        }
        
        promptBuilder.append(
            "\n" +
            "Output Format (JSON only):\n" +
            "[{\"question\":\"...\",\"answer\":\"...\"}]\n" +
            "\n" +
            String.format("CRITICAL: All content must be in %s!", languageName)
        );
        
        return promptBuilder.toString();
    }
    
    /**
     * 为问答卡片生成配图
     * 
     * @param text 文本内容（问题或答案）
     * @return 生成的图片描述文本
     */
    public String generateImageDescription(String text) {
        long startTime = System.currentTimeMillis();
        log.info("⏱️ [图片描述API] 开始生成图片描述，文本长度: {}", text.length());
        
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + volcEngineConfig.getKey());
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", volcEngineConfig.getModel());
            
            // 构建消息内容
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            
            // 构建content数组
            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", "请为以下内容生成一个简洁有趣的配图描述，描述应该突出重点概念，适合用作教学卡片插图。内容：" + text);
            content.add(textContent);
            
            message.put("content", content);
            messages.add(message);
            requestBody.put("messages", messages);
            
            // 创建HTTP实体
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送POST请求
            String response = restTemplate.postForObject(
                volcEngineConfig.getEndpoint() + "/chat/completions",
                entity,
                String.class
            );
            
            // 解析响应
            JsonNode root = objectMapper.readTree(response);
            String imageDescription = root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
            
            long endTime = System.currentTimeMillis();
            log.info("⏱️ [图片描述API] 生成成功 - 耗时:{}ms, 描述长度:{}", endTime - startTime, imageDescription.length());
            return imageDescription;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("⏱️ [图片描述API] 生成失败 - 耗时:{}ms", endTime - startTime, e);
            return null;
        }
    }
    
    /**
     * 调用火山引擎图片生成API生成图片
     * 
     * @param prompt 图片描述提示词
     * @param n 生成图片数量，默认1
     * @param size 图片尺寸，如"1024x768"
     * @return 图片URL或生成结果
     */
    public String generateImage(String prompt, Integer n, String size) {
        long startTime = System.currentTimeMillis();
        log.info("⏱️ [图片生成API] 开始生成图片，提示词长度: {}", prompt.length());
        
        try {
            // 构建请求头
            long apiStartTime = System.currentTimeMillis();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + volcEngineConfig.getKey());
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", volcEngineConfig.getImageModel());
            requestBody.put("prompt", prompt);
            requestBody.put("n", n != null ? n : 1);
            requestBody.put("size", size != null ? size : "2048x2048");
            
            // 创建HTTP实体
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送POST请求到图片生成端点
            String response = restTemplate.postForObject(
                volcEngineConfig.getEndpoint() + "/images/generations",
                entity,
                String.class
            );
            
            // 解析响应获取图片URL
            JsonNode root = objectMapper.readTree(response);
            String volcImageUrl = root.path("data")
                .get(0)
                .path("url")
                .asText();
            
            long apiEndTime = System.currentTimeMillis();
            log.info("⏱️ [图片生成API] 火山引擎API调用完成 - 耗时:{}ms, URL: {}", apiEndTime - apiStartTime, volcImageUrl);
            
            // 直接返回豆包URL，不做MinIO转存
            return volcImageUrl;
            
        } catch (HttpClientErrorException e) {
            long endTime = System.currentTimeMillis();
            log.error("⏱️ [图片生成API] HTTP请求失败 - 耗时:{}ms, 状态码:{}, 响应体:{}", 
                endTime - startTime, e.getStatusCode(), e.getResponseBodyAsString());
            log.error("请求参数: model={}, prompt长度={}, size={}", 
                volcEngineConfig.getImageModel(), prompt.length(), size);
            return null;
        } catch (ResourceAccessException e) {
            long endTime = System.currentTimeMillis();
            log.error("⏱️ [图片生成API] 网络超时或连接失败 - 耗时:{}ms, 错误:{}", 
                endTime - startTime, e.getMessage());
            log.error("建议: 检查火山引擎API网络连接或增加超时时间");
            return null;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("⏱️ [图片生成API] 未知异常 - 耗时:{}ms, 类型:{}, 消息:{}", 
                endTime - startTime, e.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 异步将豆包URL的图片上传到MinIO并更新数据库
     * 
     * @param cardId 卡片ID
     * @param volcImageUrl 豆包返回的图片URL
     * @param isQuestionImage 是否为问题图片（true:questionImage, false:answerImage）
     */
    @Async
    public void uploadImageToMinioAndUpdateDb(Long cardId, String volcImageUrl, boolean isQuestionImage) {
        if (cardId == null || volcImageUrl == null || volcImageUrl.isEmpty()) {
            log.warn("[异步图片上传] 参数无效 - cardId: {}, volcImageUrl: {}", cardId, volcImageUrl);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        log.info("[异步图片上传] 开始处理 - cardId: {}, 图片类型: {}, URL: {}", 
            cardId, isQuestionImage ? "问题图片" : "答案图片", volcImageUrl);
        
        try {
            // 1. 从豆包URL下载并上传到MinIO
            String minioUrl = minioService.uploadFromUrl(volcImageUrl);
            
            if (minioUrl == null || minioUrl.isEmpty()) {
                log.error("[异步图片上传] MinIO上传失败，返回URL为空 - cardId: {}", cardId);
                return;
            }
            
            // 2. 更新数据库
            com.knowledge.questioncard.entity.QuestionCard card = new com.knowledge.questioncard.entity.QuestionCard();
            card.setId(cardId);
            if (isQuestionImage) {
                card.setQuestionImage(minioUrl);
            } else {
                card.setAnswerImage(minioUrl);
            }
            
            int updated = questionCardMapper.updateById(card);
            
            long endTime = System.currentTimeMillis();
            if (updated > 0) {
                log.info("[异步图片上传] 完成 - cardId: {}, MinIO URL: {}, 耗时: {}ms", 
                    cardId, minioUrl, endTime - startTime);
            } else {
                log.error("[异步图片上传] 数据库更新失败 - cardId: {}, 受影响行数: {}", cardId, updated);
            }
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("[异步图片上传] 失败 - cardId: {}, 耗时: {}ms, 错误: {}", 
                cardId, endTime - startTime, e.getMessage(), e);
        }
    }
}