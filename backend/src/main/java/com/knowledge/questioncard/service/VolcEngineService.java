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
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 构造函数中初始化RestTemplate并配置超时
    public VolcEngineService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 连接超时30秒
        factory.setReadTimeout(60000);    // 读取超时60秒
        this.restTemplate = new RestTemplate(factory);
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
        try {
            // 获取或创建ArkService实例
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
            
            // 发送流式请求 - 使用真正的异步流式处理
            // 使用StringBuilder累积所有增量片段
            final StringBuilder accumulatedContent = new StringBuilder();
            // 记录上一个token,用于智能添加空格
            final StringBuilder lastToken = new StringBuilder();
            
            // 使用CountDownLatch等待流式完成
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> errorRef = new AtomicReference<>();
            
            service.streamChatCompletion(request)
                    .subscribe(
                        response -> {
                            try {
                                if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                                    // 获取本次返回的增量内容
                                    String incrementalContent = response.getChoices().get(0).getMessage().getContent().toString();
                                    
                                    if (incrementalContent != null && !incrementalContent.isEmpty()) {
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
                            log.info("流式响应完成");
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
                    log.info("✨ 开始为流式生成的卡片添加图片描述");
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
                        
                        for (JsonNode cardNode : cardsArray) {
                            currentCard++;
                            log.info("📝 处理第 {}/{} 张卡片", currentCard, totalCards);
                            Map<String, Object> card = new HashMap<>();
                            
                            // 安全获取字段值
                            JsonNode questionNode = cardNode.get("question");
                            JsonNode answerNode = cardNode.get("answer");
                            
                            if (questionNode == null || answerNode == null) {
                                log.warn("卡片字段缺失，跳过: {}", cardNode.toString());
                                continue;
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
                            
                            // 发送进度通知
                            emitter.send(SseEmitter.event().name("status")
                                .data(String.format("正在为第 %d/%d 张卡片生成图片描述...", currentCard, totalCards)));
                            
                            // 先生成问题的图片描述，再生成图片URL
                            log.info("🎨 生成问题图片描述: {}", question.substring(0, Math.min(50, question.length())));
                            String questionDesc = generateImageDescription(question);
                            String questionImage = null;
                            if (questionDesc != null) {
                                log.info("🖼️ 根据描述生成问题图片: {}", questionDesc);
                                questionImage = generateImage(questionDesc, 1, "2048x2048");
                                if (questionImage == null) {
                                    log.error("❌ 问题图片生成失败");
                                }
                            } else {
                                log.warn("⚠️ 问题图片描述生成失败");
                            }
                            card.put("questionImage", questionImage);
                            
                            // 先生成答案的图片描述，再生成图片URL
                            log.info("🎨 生成答案图片描述: {}", answer.substring(0, Math.min(50, answer.length())));
                            String answerDesc = generateImageDescription(answer);
                            String answerImage = null;
                            if (answerDesc != null) {
                                log.info("🖼️ 根据描述生成答案图片: {}", answerDesc);
                                answerImage = generateImage(answerDesc, 1, "2048x2048");
                                if (answerImage == null) {
                                    log.error("❌ 答案图片生成失败");
                                }
                            } else {
                                log.warn("⚠️ 答案图片描述生成失败");
                            }
                            card.put("answerImage", answerImage);
                            
                            updatedCards.add(card);
                            log.info("✅ 第 {}/{} 张卡片图片处理完成", currentCard, totalCards);
                            
                            // 发送进度通知
                            emitter.send(SseEmitter.event().name("status")
                                .data(String.format("第 %d/%d 张卡片图片生成完成", currentCard, totalCards)));
                        }
                        
                        // 发送更新后的完整JSON（包含图片URL）
                        String updatedJson = objectMapper.writeValueAsString(updatedCards);
                        emitter.send(SseEmitter.event().name("images").data(updatedJson));
                        
                        // 🔑 关键修改：返回包含图片URL的完整JSON，而不是原始JSON
                        log.info("🎉 图片生成完成，返回包含图片URL的完整JSON");
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
        if (language == null || language.isEmpty()) {
            languageName = "中文"; // 默认中文
        } else {
            String lang = language.toLowerCase().trim();
            if (lang.equals("zh") || lang.equals("中文") || lang.equals("chinese")) {
                languageName = "中文";
            } else if (lang.equals("en") || lang.equals("英文") || lang.equals("english")) {
                languageName = "英文";
            } else {
                languageName = "中文"; // 其他情况默认中文
            }
        }
        
        return String.format(
            "🎭 开启搞怪模式!请生成%d张关于'%s'主题的超级无敌搞笑学习卡片!\n" +
            "\n" +
            "📋 出题要求(第一步 - 折磨灵魂时刻):\n" +
            "1. 难度:%s (但要用最刁钻的角度提问!)\n" +
            "2. 语言:%s (问题和答案都必须用%s!)\n" +
            "3. 问题风格指南:\n" +
            "   - 可以用反常识的角度问\n" +
            "   - 可以设置脑筋急转弯式的陷阱\n" +
            "   - 可以用夸张搞笑的比喻\n" +
            "   - 让人看到题目就想:卧槽这也能这么问?!\n" +
            "\n" +
            "💡 解题要求(第二步 - 拯救智商时刻):\n" +
            "1. 答案必须用%s书写,要准确but要用最骚的方式讲明白\n" +
            "2. 多用表情符号、网络流行梗、沙雕比喻\n" +
            "3. 可以脑补段子、编顺口溜、讲小故事\n" +
            "4. 让人看完答案会心一笑:原来如此,这么记确实忘不了!\n" +
            "\n" +
            "⚠️ 注意:虽然搞怪,但知识点必须正确!我们是认真搞笑的教育工作者!\n" +
            "\n" +
            "📦 输出格式(只返回JSON,不要废话):\n" +
            "[{\"question\":\"问题(搞怪版)\",\"answer\":\"答案(段子版)\"}]\n" +
            "\n" +
            "🚨 重要格式要求(必须严格遵守!):\n" +
            "\n" +
            "📝 英文单词空格规则(极其重要!):\n" +
            "- 如果生成的内容是英文,每个英文单词之间必须有空格\n" +
            "- 绝对不能出现单词粘连的情况(例如: 错误写法 Yourfriendbrags, 正确写法 Your friend brags)\n" +
            "- 中文内容正常书写,不需要额外空格\n" +
            "- 混合语言时,英文部分每个单词之间要有空格\n" +
            "\n" +
            "示例对比:\n" +
            "✅ 正确: \"Your friend brags about his new car\"\n" +
            "❌ 错误: \"Yourfriendbragsabouthisnewcar\"\n" +
            "✅ 正确: \"What is the capital of France?\"\n" +
            "❌ 错误: \"WhatisthecapitalofFrance?\"\n" +
            "\n" +
            "⚠️ 注意: 英文单词之间的空格是必需的,这不是可选项!\n" +
            "\n" +
            "📦 JSON格式要求:\n" +
            "- JSON格式必须规范,字符串中的引号要转义\n" +
            "- 保持文本可读性\n" +
            "\n" +
            "现在,释放你的洪荒之力吧!🚀",
            cardCount, topic, difficulty, languageName, languageName, languageName
        );
    }
    
    /**
     * 为问答卡片生成配图
     * 
     * @param text 文本内容（问题或答案）
     * @return 生成的图片描述文本
     */
    public String generateImageDescription(String text) {
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
            
            log.info("图片描述生成成功: {}", imageDescription);
            return imageDescription;
            
        } catch (Exception e) {
            log.error("生成图片描述失败", e);
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
        try {
            // 构建请求头
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
            
            log.info("火山引擎图片生成成功，URL: {}", volcImageUrl);
            
            // 将火山引擎的图片下载并上传到MinIO
            try {
                String minioUrl = minioService.uploadFromUrl(volcImageUrl);
                log.info("图片已转存到MinIO: {}", minioUrl);
                return minioUrl;
            } catch (Exception e) {
                log.error("转存图片到MinIO失败，返回原始URL: {}", volcImageUrl, e);
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