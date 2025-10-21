package com.knowledge.questioncard.service;

import com.knowledge.questioncard.config.VolcEngineConfig;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
    
    // 缓存ArkService实例,避免每次都创建
    private volatile ArkService arkService;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Executor sseTaskExecutor;
    
    // 构造函数中初始化RestTemplate并配置超时
    @Autowired
    public VolcEngineService(Executor sseTaskExecutor, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 连接超时30秒
        factory.setReadTimeout(60000);    // 读取超时60秒
        this.restTemplate = new RestTemplate(factory);
        this.sseTaskExecutor = sseTaskExecutor;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 生成问答卡片内容
     * 
     * @param topic 主题
     * @param cardCount 卡片数量
     * @param difficulty 难度
     * @param language 语言
     * @return 生成的卡片内容JSON字符串
     */
    public String generateCards(String topic, Integer cardCount, String difficulty, String language) {
        try {
            // 获取或创建ArkService实例(单例模式)
            ArkService service = getOrCreateArkService();
            
            // 构建提示词
            String prompt = buildPrompt(topic, cardCount, difficulty, language);
            
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
            String response = service.createChatCompletion(request)
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
     * 获取或创建ArkService实例(双重检查锁定单例模式)
     */
    private ArkService getOrCreateArkService() {
        if (arkService == null) {
            synchronized (this) {
                if (arkService == null) {
                    arkService = ArkService.builder()
                            .apiKey(volcEngineConfig.getKey())
                            .baseUrl(volcEngineConfig.getEndpoint())
                            .build();
                    log.info("ArkService实例已创建并缓存");
                }
            }
        }
        return arkService;
    }
    
    /**
     * 流式生成问答卡片内容
     * 
     * @param topic 主题
     * @param cardCount 卡片数量
     * @param difficulty 难度
     * @param language 语言
     * @param withImages 是否生成图片描述
     * @param emitter SSE发射器
     * @return 累积的完整JSON内容
     */
    public String generateCardsStream(String topic, Integer cardCount, String difficulty, 
                                   String language, Boolean withImages, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        log.info("⏱️ [计时开始] 卡片生成任务启动 - 主题:{}, 数量:{}, 难度:{}", topic, cardCount, difficulty);
        
        try {
            // 获取或创建ArkService实例
            long serviceStartTime = System.currentTimeMillis();
            ArkService service = getOrCreateArkService();
            log.info("⏱️ [计时] ArkService初始化耗时: {}ms", System.currentTimeMillis() - serviceStartTime);
            
            // 构建提示词
            long promptStartTime = System.currentTimeMillis();
            String prompt = buildPrompt(topic, cardCount, difficulty, language);
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
            // 记录上一个token,用于智能添加空格
            final StringBuilder lastToken = new StringBuilder();
            
            // 使用CountDownLatch等待流式完成
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> errorRef = new AtomicReference<>();
            final AtomicLong firstTokenTime = new AtomicLong(0);
            final AtomicInteger tokenCount = new AtomicInteger(0);
            
            service.streamChatCompletion(request)
                    .subscribe(
                        response -> {
                            try {
                                if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                                    // 获取本次返回的增量内容
                                    String incrementalContent = response.getChoices().get(0).getMessage().getContent().toString();
                                    
                                    if (incrementalContent != null && !incrementalContent.isEmpty()) {
                                        // 记录首token时间
                                        if (firstTokenTime.get() == 0) {
                                            firstTokenTime.set(System.currentTimeMillis());
                                            log.info("⏱️ [计时] 首个token返回耗时: {}ms", firstTokenTime.get() - streamStartTime);
                                        }
                                        tokenCount.incrementAndGet();
                                        // 直接使用AI返回的内容(AI已按提示词要求添加空格)
                                        
                                        // 更新lastToken为当前内容的最后一个字符
                                        lastToken.setLength(0);
                                        if (incrementalContent.length() > 0) {
                                            lastToken.append(incrementalContent.charAt(incrementalContent.length() - 1));
                                        }
                                        
                                        // 累积到StringBuilder中
                                        accumulatedContent.append(incrementalContent);
                                        
                                        log.info("[SSE流式发送] 本次增量={}, 累积总长={}",
                                                incrementalContent,
                                                accumulatedContent.length());

                                        // 立即发送AI返回的内容 - 真正的实时流式
                                        emitter.send(SseEmitter.event()
                                                .name("message")
                                                .data(incrementalContent));
                                    }
                                }
                            } catch (Exception e) {
                                log.error("处理流式响应失败", e);
                                errorRef.set(e);
                                latch.countDown();
                            }
                        },
                        error -> {
                            log.error("流式调用火山引擎API失败: {}", error.getMessage());
                            errorRef.set(error);
                            try {
                                emitter.send(SseEmitter.event().name("error").data("生成失败: " + error.getMessage()));
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("发送错误事件失败", e);
                            }
                            latch.countDown();
                        },
                        () -> {
                            long streamEndTime = System.currentTimeMillis();
                            log.info("⏱️ [计时] 大模型流式调用完成 - 总耗时:{}ms, token数:{}, 平均速度:{} tokens/s",
                                streamEndTime - streamStartTime,
                                tokenCount.get(),
                                tokenCount.get() * 1000.0 / (streamEndTime - streamStartTime));
                            latch.countDown();
                        }
                    );
            
            // 等待流式完成(最多5分钟)
            try {
                if (!latch.await(5, TimeUnit.MINUTES)) {
                    log.error("流式响应超时");
                    emitter.send(SseEmitter.event().name("error").data("生成超时"));
                    emitter.complete();
                    return null;
                }
            } catch (InterruptedException e) {
                log.error("等待流式响应被中断", e);
                Thread.currentThread().interrupt();
                emitter.send(SseEmitter.event().name("error").data("生成被中断"));
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
                    JsonNode cardsArray = objectMapper.readTree(cardsJson);
                    
                    if (cardsArray.isArray()) {
                        List<Map<String, Object>> updatedCards = new ArrayList<>();
                        int totalCards = cardsArray.size();
                        int currentCard = 0;
                        
                        // 🚀 优化：使用CompletableFuture并行生成图片
                        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
                        long parallelStartTime = System.currentTimeMillis();
                        log.info("⏱️ [计时] 开始并行处理{}张卡片的图片", totalCards);
                        
                        for (JsonNode cardNode : cardsArray) {
                            final int cardIndex = ++currentCard;
                            
                            // 为每张卡片创建异步任务
                            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                                try {
                                    log.info("📝 处理第 {}/{} 张卡片", cardIndex, totalCards);
                                    Map<String, Object> card = new HashMap<>();
                                    
                                    // 安全获取字段值
                                    JsonNode questionNode = cardNode.get("question");
                                    JsonNode answerNode = cardNode.get("answer");
                                    
                                    if (questionNode == null || answerNode == null) {
                                        log.warn("卡片字段缺失，跳过: {}", cardNode.toString());
                                        return null;
                                    }
                                    
                                    String question = questionNode.asText();
                                    String answer = answerNode.asText();
                                    card.put("question", question);
                                    card.put("answer", answer);
                                    
                                    // 保留原始ID（如果存在）
                                    JsonNode idNode = cardNode.get("id");
                                    if (idNode != null) {
                                        card.put("id", idNode.asLong());
                                    }
                                    
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
                                    
                                    // 等待两个图片都生成完成
                                    String questionImage = questionImageFuture.join();
                                    String answerImage = answerImageFuture.join();
                                    
                                    card.put("questionImage", questionImage);
                                    card.put("answerImage", answerImage);
                                    
                                    log.info("✅ 第 {}/{} 张卡片图片处理完成", cardIndex, totalCards);
                                    
                                    // 发送进度通知
                                    try {
                                        emitter.send(SseEmitter.event().name("status")
                                            .data(String.format("第 %d/%d 张卡片图片生成完成", cardIndex, totalCards)));
                                    } catch (IOException e) {
                                        log.error("发送进度通知失败", e);
                                    }
                                    
                                    return card;
                                } catch (Exception e) {
                                    log.error("处理卡片异常", e);
                                    return null;
                                }
                            }, sseTaskExecutor);
                            
                            futures.add(future);
                        }
                        
                        // 等待所有卡片处理完成
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                        long parallelEndTime = System.currentTimeMillis();
                        log.info("⏱️ [计时] 所有卡片图片并行生成完成 - 耗时:{}ms", parallelEndTime - parallelStartTime);
                        
                        // 收集结果
                        for (CompletableFuture<Map<String, Object>> future : futures) {
                            Map<String, Object> card = future.join();
                            if (card != null) {
                                updatedCards.add(card);
                            }
                        }
                        
                        // 发送更新后的完整JSON（包含图片URL）
                        String updatedJson = objectMapper.writeValueAsString(updatedCards);
                        emitter.send(SseEmitter.event().name("images").data(updatedJson));
                        
                        // 🔑 关键修改：返回包含图片URL的完整JSON，而不是原始JSON
                        long imageEndTime = System.currentTimeMillis();
                        log.info("⏱️ [计时] 图片生成任务完成 - 总耗时:{}ms", imageEndTime - imageStartTime);
                        log.info("⏱️ [计时总结] 整个卡片生成流程完成 - 总耗时:{}ms", imageEndTime - startTime);
                        return updatedJson;
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
    private String buildPrompt(String topic, Integer cardCount, String difficulty, String language) {
        // 处理各种可能的语言代码和名称
        String languageName;
        boolean isEnglish = false;
        
        if (language == null || language.isEmpty()) {
            languageName = "中文"; // 默认中文
        } else {
            String lang = language.toLowerCase().trim();
            if (lang.equals("en") || lang.equals("英文") || lang.equals("english")) {
                languageName = "English";
                isEnglish = true;
            } else {
                languageName = "中文";
            }
        }
        
        // 根据语言选择不同的提示词模板
        if (isEnglish) {
            return buildEnglishPrompt(topic, cardCount, difficulty);
        } else {
            return buildChinesePrompt(topic, cardCount, difficulty);
        }
    }
    
    /**
     * 构建英文提示词(精简版)
     */
    private String buildEnglishPrompt(String topic, Integer cardCount, String difficulty) {
        return String.format(
            "Generate %d creative flashcards about '%s' (difficulty: %s).\n" +
            "\n" +
            "Requirements:\n" +
            "1. Each card must have a tricky, thought-provoking question\n" +
            "2. Answer should be accurate but explained in a fun, memorable way\n" +
            "3. Use emojis and creative metaphors to make it engaging\n" +
            "4. All content must be in English with proper spacing between words\n" +
            "\n" +
            "Output Format (JSON only):\n" +
            "[{\"question\":\"...\",\"answer\":\"...\"}]\n" +
            "\n" +
            "CRITICAL: Every English word MUST be separated by spaces. No word concatenation allowed!",
            cardCount, topic, difficulty
        );
    }
    
    /**
     * 构建中文提示词(超精简版 - 强调趣味性)
     */
    private String buildChinesePrompt(String topic, Integer cardCount, String difficulty) {
        return String.format(
            "🎯 生成%d张'%s'主题学习卡片(难度:%s)\n" +
            "\n" +
            "💥 出题风格:用最刁钻、最骚的角度提问!让人看到就想:卧槽还能这么问?!\n" +
            "🎉 答案风格:用表情包🤪、网络梗、沙雕比喻讲明白!让人秒懂还笑出声!\n" +
            "✅ 底线:知识必须正确,但表达必须骚气!\n" +
            "\n" +
            "📦 只返回JSON:[{\"question\":\"...\",\"answer\":\"...\"}]\n" +
            "\n" +
            "开整!🚀",
            cardCount, topic, difficulty
        );
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
            log.info("⏱️ [图片生成API] 火山引擎API调用完成 - 耗时:{}ms", apiEndTime - apiStartTime);
            
            // 将火山引擎的图片下载并上传到MinIO
            try {
                long minioStartTime = System.currentTimeMillis();
                String minioUrl = minioService.uploadFromUrl(volcImageUrl);
                long minioEndTime = System.currentTimeMillis();
                log.info("⏱️ [图片生成API] MinIO转存完成 - 耗时:{}ms, 总耗时:{}ms", 
                    minioEndTime - minioStartTime, minioEndTime - startTime);
                return minioUrl;
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                log.error("⏱️ [图片生成API] MinIO转存失败 - 耗时:{}ms", endTime - startTime, e);
                // 如果转存失败，返回原始URL作为降级方案
                return volcImageUrl;
            }
            
        } catch (HttpClientErrorException e) {
            log.error("生成图片失败 - HTTP错误: {}, 响应体: {}", e.getStatusCode(), e.getResponseBodyAsString());
            log.error("请求参数: prompt={}, size={}", prompt, size);
            return null;
        } catch (Exception e) {
            log.error("生成图片失败 - 异常: {}", e.getMessage(), e);
            return null;
        }
    }
}