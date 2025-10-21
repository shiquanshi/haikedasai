package com.knowledge.questioncard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.questioncard.dto.QuestionBankDTO;
import com.knowledge.questioncard.dto.QuestionCardDTO;
import com.knowledge.questioncard.entity.QuestionBank;
import com.knowledge.questioncard.entity.QuestionCard;
import com.knowledge.questioncard.mapper.QuestionBankMapper;
import com.knowledge.questioncard.mapper.QuestionCardMapper;
import com.knowledge.questioncard.cache.QuestionBankCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionBankService {
    private final QuestionBankMapper questionBankMapper;
    private final QuestionCardMapper questionCardMapper;
    private final DocumentParserService documentParserService;
    private final VolcEngineService volcEngineService;
    private final QuestionBankCache questionBankCache;
    private final MinioService minioService;

    /**
     * 保存流式生成的卡片到数据库
     */
    @Transactional
    public List<QuestionCardDTO> saveStreamGeneratedCards(String cardsJson, String topic, String difficulty, String language, Long userId, String scenario) {
        try {
            log.info("收到的卡片JSON: {}", cardsJson);
            // 创建题库
            QuestionBank bank = new QuestionBank();
            // 使用"AI生成 - 日期 - topic"格式作为题库名称
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            bank.setName("AI生成 - " + timestamp + " - " + topic);
            // 描述中包含所有生成信息
            bank.setDescription("主题: " + topic + ", 难度: " + difficulty + ", 语言: " + language + 
                (!scenario.isEmpty() ? ", 场景: " + scenario : ""));
            bank.setTopic(topic);
            bank.setType("ai");
            bank.setDifficulty(difficulty);
            bank.setLanguage(language);
            bank.setUserId(String.valueOf(userId));
            bank.setCreatedAt(new java.util.Date());
            questionBankMapper.insert(bank);
            
            // 解析JSON并创建卡片
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode cardsArray = objectMapper.readTree(cardsJson);
            
            List<QuestionCard> cards = new ArrayList<>();
            java.util.Date now = new java.util.Date();
            
            if (cardsArray.isArray()) {
                for (JsonNode cardNode : cardsArray) {
                    QuestionCard card = new QuestionCard();
                    card.setBankId(bank.getId());
                    card.setQuestion(cardNode.get("question").asText());
                    card.setAnswer(cardNode.get("answer").asText());
                    
                    // 处理可选的图片字段
                    if (cardNode.has("questionImage") && !cardNode.get("questionImage").isNull()) {
                        card.setQuestionImage(cardNode.get("questionImage").asText());
                    }
                    if (cardNode.has("answerImage") && !cardNode.get("answerImage").isNull()) {
                        card.setAnswerImage(cardNode.get("answerImage").asText());
                    }
                    
                    card.setCreatedAt(now);
                    cards.add(card);
                }
            }
            
            // 批量插入数据库
            if (!cards.isEmpty()) {
                batchInsertCards(cards);
                // 更新题库卡片数量统计
                updateBankStatistics(bank.getId());
            }
            
            // 转换为DTO返回
            return cards.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("保存流式生成的卡片失败", e);
            throw new RuntimeException("保存卡片失败: " + e.getMessage());
        }
    }

    /**
     * AI生成题库
     */
    @Transactional
    public List<QuestionCardDTO> generateAIBank(String topic, Integer cardCount, String difficulty, String language, Long userId, String scenario) {
        // 先检查缓存
        String cacheKey = questionBankCache.generateKey(topic, cardCount, difficulty, language);
        List<QuestionCardDTO> cachedCards = questionBankCache.get(cacheKey);
        if (cachedCards != null) {
            log.info("从缓存返回结果,主题: {}, 卡片数: {}", topic, cachedCards.size());
            return cachedCards;
        }
        
        // 创建题库
        QuestionBank bank = new QuestionBank();
        // 使用"AI生成 - 日期 - topic"格式作为题库名称
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        bank.setName("AI生成 - " + timestamp + " - " + topic);
        // 描述中包含所有生成信息
        bank.setDescription("主题: " + topic + ", 难度: " + difficulty + ", 语言: " + language + 
            (!scenario.isEmpty() ? ", 场景: " + scenario : ""));
        bank.setTopic(topic);
        bank.setType("ai");
        bank.setUserId(String.valueOf(userId));
        bank.setCreatedAt(new java.util.Date());
        questionBankMapper.insert(bank);

        // 使用火山引擎AI生成问答卡片
        List<QuestionCard> cards = generateCardsFromAI(topic, bank.getId(), cardCount, difficulty, language, scenario);
        
        // 批量插入优化 - 一次性设置所有卡片的创建时间
        java.util.Date now = new java.util.Date();
        cards.forEach(card -> card.setCreatedAt(now));
        
        // 批量插入数据库
        if (!cards.isEmpty()) {
            batchInsertCards(cards);
            // 更新题库卡片数量统计
            updateBankStatistics(bank.getId());
        }

        List<QuestionCardDTO> cardDTOs = cards.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        // 缓存结果
        questionBankCache.put(cacheKey, cardDTOs);
        log.info("已缓存生成结果,主题: {}, 卡片数: {}", topic, cardDTOs.size());
        
        return cardDTOs;
    }

    /**
     * 获取系统推荐题库
     */
    public List<QuestionBankDTO> getSystemBanks(String topic) {
        List<QuestionBank> banks = questionBankMapper.selectByTopicAndType(topic, "system");
        return banks.stream()
                .map(this::convertToBankDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户自定义题库
     */
    public List<QuestionBankDTO> getUserCustomBanks(Long userId) {
        List<QuestionBank> banks = questionBankMapper.searchBanks(
            null, // topic
            "custom", // type
            null, // difficulty
            null, // tags
            userId,
            null, // minCards
            null, // maxCards
            0, // offset
            100, // limit - 最多返回100个
            "created_at", // sortBy
            "DESC" // sortOrder - 最新创建的在前
        );
        return banks.stream()
                .map(this::convertToBankDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定题库的卡片
     */
    public List<QuestionCardDTO> getBankCards(Long bankId) {
        List<QuestionCard> cards = questionCardMapper.selectByBankId(bankId);
        return cards.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建自定义题库
     */
    @Transactional
    public QuestionBankDTO createCustomBank(String name, String description, String topic, String difficulty, String language, Long userId) {
        QuestionBank bank = new QuestionBank();
        bank.setName(name);
        bank.setDescription(description != null ? description : "");
        bank.setTopic(topic);
        bank.setType("custom");
        bank.setDifficulty(difficulty != null ? difficulty : "medium"); // 设置默认难度
        bank.setLanguage(language != null ? language : "中文"); // 设置默认语言
        bank.setUserId(String.valueOf(userId));
        bank.setCreatedAt(new java.util.Date());
        questionBankMapper.insert(bank);
        
        return convertToBankDTO(bank);
    }

    /**
     * 上传自定义文档生成题库
     */
    @Transactional
    public List<QuestionCardDTO> uploadCustomBank(MultipartFile file, String topic, Long userId) {
        try {
            // 解析文档内容
            String content = documentParserService.parseDocument(file);

            // 创建题库
            QuestionBank bank = new QuestionBank();
            bank.setName("自定义 - " + file.getOriginalFilename());
            bank.setDescription("基于上传文档生成的题库");
            bank.setTopic(topic);
            bank.setType("custom");
            bank.setDifficulty("medium"); // 设置默认难度
            bank.setLanguage("中文"); // 设置默认语言
            bank.setUserId(String.valueOf(userId));
            bank.setCreatedAt(new java.util.Date());
            questionBankMapper.insert(bank);

            // 从文档内容生成问答卡片
            List<QuestionCard> cards = generateQuestionsFromContent(content, bank.getId());
            for (QuestionCard card : cards) {
                card.setCreatedAt(new java.util.Date());
                questionCardMapper.insert(card);
            }

            return cards.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 模拟生成问答卡片
     */
    private List<QuestionCard> generateMockQuestions(String topic, Long bankId, Integer cardCount, String difficulty, String language) {
        List<QuestionCard> allCards = new ArrayList<>();

        if ("财务".equals(topic)) {
            allCards.add(createCard("什么是资产负债表?", 
                "资产负债表是反映企业在某一特定日期财务状况的会计报表,主要包括资产、负债和所有者权益三大部分。", bankId));
            allCards.add(createCard("流动资产包括哪些?", 
                "流动资产主要包括:现金、银行存款、应收账款、预付账款、存货、短期投资等在一年内或超过一年的一个营业周期内变现或耗用的资产。", bankId));
            allCards.add(createCard("什么是会计等式?", 
                "会计等式是:资产 = 负债 + 所有者权益。这是复式记账的理论基础,反映了企业资金的来源和运用关系。", bankId));
            allCards.add(createCard("利润表的作用是什么?", 
                "利润表反映企业在一定会计期间的经营成果,展示收入、费用和利润的形成过程,是评价企业盈利能力的重要报表。", bankId));
            allCards.add(createCard("什么是折旧?", 
                "折旧是指固定资产在使用过程中,因损耗而逐渐转移到产品成本或费用中的那部分价值,是固定资产价值的分摊过程。", bankId));
        } else if ("税务".equals(topic)) {
            allCards.add(createCard("增值税的计算公式是什么?", 
                "增值税应纳税额 = 销项税额 - 进项税额。销项税额 = 销售额 × 税率,进项税额为购进货物或服务时支付的增值税。", bankId));
            allCards.add(createCard("什么是小规模纳税人?", 
                "小规模纳税人是指年销售额在规定标准以下,会计核算不健全的增值税纳税人。小规模纳税人适用简易计税方法,征收率通常为3%。", bankId));
            allCards.add(createCard("企业所得税的税率是多少?", 
                "我国企业所得税的基本税率为25%。符合条件的小型微利企业可享受20%的优惠税率,高新技术企业适用15%的优惠税率。", bankId));
            allCards.add(createCard("什么是进项税额?", 
                "进项税额是指纳税人购进货物、加工修理修配劳务、服务、无形资产或者不动产,支付或者负担的增值税额。", bankId));
            allCards.add(createCard("个人所得税起征点是多少?", 
                "目前个人所得税综合所得的起征点(基本减除费用标准)为每月5000元,即每年6万元。", bankId));
        } else {
            allCards.add(createCard("什么是" + topic + "?", 
                "这是一个关于" + topic + "的基础问题,需要根据具体领域来详细解答。", bankId));
            allCards.add(createCard(topic + "的核心概念是什么?", 
                topic + "包含多个核心概念,需要系统学习和理解。", bankId));
            allCards.add(createCard(topic + "在实践中如何应用?", 
                "在实际工作中," + topic + "的应用需要结合具体场景和需求。", bankId));
            allCards.add(createCard(topic + "的发展趋势?", 
                topic + "正在不断发展演进,需要持续关注最新动态。", bankId));
            allCards.add(createCard(topic + "的最佳实践?", 
                "实施" + topic + "时应该遵循业界公认的最佳实践。", bankId));
        }

        // 根据cardCount参数返回指定数量的卡片
        List<QuestionCard> cards = new ArrayList<>();
        int count = Math.min(cardCount, allCards.size());
        for (int i = 0; i < count; i++) {
            cards.add(allCards.get(i));
        }

        return cards;
    }

    /**
     * 从文档内容生成问答卡片
     */
    private List<QuestionCard> generateQuestionsFromContent(String content, Long bankId) {
        List<QuestionCard> cards = new ArrayList<>();
        
        // 简单实现:将文档按段落分割,每段生成一个问答
        String[] paragraphs = content.split("\\n\\n");
        for (int i = 0; i < Math.min(paragraphs.length, 10); i++) {
            String para = paragraphs[i].trim();
            if (!para.isEmpty() && para.length() > 20) {
                cards.add(createCard(
                    "关于文档第" + (i + 1) + "部分的内容?",
                    para.substring(0, Math.min(para.length(), 500)),
                    bankId
                ));
            }
        }

        if (cards.isEmpty()) {
            cards.add(createCard("文档的主要内容是什么?", 
                content.substring(0, Math.min(content.length(), 500)), bankId));
        }

        return cards;
    }

    private QuestionCard createCard(String question, String answer, Long bankId) {
        QuestionCard card = new QuestionCard();
        card.setQuestion(question);
        card.setAnswer(answer);
        card.setBankId(bankId);
        return card;
    }

    private QuestionCardDTO convertToDTO(QuestionCard card) {
        QuestionCardDTO dto = new QuestionCardDTO();
        dto.setId(card.getId());
        dto.setBankId(card.getBankId());
        dto.setQuestion(card.getQuestion());
        dto.setAnswer(card.getAnswer());
        dto.setQuestionImage(card.getQuestionImage());
        dto.setAnswerImage(card.getAnswerImage());
        dto.setCreatedAt(card.getCreatedAt());
        dto.setUpdatedAt(card.getUpdatedAt());
        return dto;
    }


    /**
     * 使用火山引擎AI生成问答卡片
     */
    private List<QuestionCard> generateCardsFromAI(String topic, Long bankId, Integer cardCount, String difficulty, String language, String scenario) {
        try {
            // 调用火山引擎AI生成内容
            String aiResponse = volcEngineService.generateCards(topic, cardCount, difficulty, language, scenario);
            
            // 解析JSON响应
            List<QuestionCard> cards = parseAIResponse(aiResponse, bankId);
            
            // 如果AI生成的数量不足,用模拟数据补充
            if (cards.size() < cardCount) {
                List<QuestionCard> mockCards = generateMockQuestions(topic, bankId, cardCount - cards.size(), difficulty, language);
                cards.addAll(mockCards);
            }
            
            return cards;
        } catch (Exception e) {
            // AI生成失败时使用模拟数据
            return generateMockQuestions(topic, bankId, cardCount, difficulty, language);
        }
    }
    
    /**
     * 解析AI返回的JSON格式响应
     */
    private List<QuestionCard> parseAIResponse(String jsonResponse, Long bankId) {
        List<QuestionCard> cards = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            
            // 清理可能的markdown代码块标记
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7);
            }
            if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse.substring(3);
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
            jsonResponse = jsonResponse.trim();
            
            // 解析JSON数组
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    String question = node.has("question") ? node.get("question").asText() : null;
                    String answer = node.has("answer") ? node.get("answer").asText() : null;
                    
                    if (question != null && answer != null && !question.isEmpty() && !answer.isEmpty()) {
                        cards.add(createCard(question, answer, bankId));
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败返回空列表
        }
        return cards;
    }

    /**
     * 批量插入问答卡片
     */
    private void batchInsertCards(List<QuestionCard> cards) {
        // 使用MyBatis批量插入,每批100条
        int batchSize = 100;
        for (int i = 0; i < cards.size(); i += batchSize) {
            int end = Math.min(i + batchSize, cards.size());
            List<QuestionCard> batch = cards.subList(i, end);
            for (QuestionCard card : batch) {
                questionCardMapper.insert(card);
            }
        }
    }

    private QuestionBankDTO convertToBankDTO(QuestionBank bank) {
        QuestionBankDTO dto = new QuestionBankDTO();
        dto.setId(bank.getId());
        dto.setName(bank.getName());
        dto.setDescription(bank.getDescription());
        dto.setTopic(bank.getTopic());
        dto.setCardCount(bank.getCardCount());
        dto.setDifficulty(bank.getDifficulty());
        dto.setLanguage(bank.getLanguage());
        return dto;
    }
    
    /**
     * 高级搜索题库（分页）
     */
    public com.knowledge.questioncard.dto.PageResponse<QuestionBankDTO> searchBanks(
            com.knowledge.questioncard.dto.BankSearchRequest request) {
        // 执行查询
        List<QuestionBank> banks = questionBankMapper.searchBanks(
            request.getTopic(),
            request.getType(),
            request.getDifficulty(),
            request.getTags(),
            request.getUserId(),
            request.getMinCards(),
            request.getMaxCards(),
            request.getOffset(),
            request.getPageSize(),
            request.getSortBy(),
            request.getSortOrder()
        );
        
        // 统计总数
        Long total = questionBankMapper.countBanks(
            request.getTopic(),
            request.getType(),
            request.getDifficulty(),
            request.getTags(),
            request.getUserId(),
            request.getMinCards(),
            request.getMaxCards()
        );
        
        // 转换为DTO
        List<QuestionBankDTO> bankDTOs = banks.stream()
                .map(this::convertToBankDTO)
                .collect(Collectors.toList());
        
        return new com.knowledge.questioncard.dto.PageResponse<>(
            bankDTOs, total, request.getPage(), request.getPageSize()
        );
    }
    
    /**
     * 增加题库浏览次数
     */
    public void incrementViewCount(Long bankId) {
        questionBankMapper.incrementViewCount(bankId);
    }
    
    /**
     * 更新题库统计信息
     */
    public void updateBankStatistics(Long bankId) {
        int cardCount = questionCardMapper.countByBankId(bankId);
        questionBankMapper.updateStatistics(bankId, cardCount);
    }
    
    /**
     * 批量添加卡片到指定题库
     */
    @Transactional
    public List<QuestionCardDTO> addCardsToBank(Long targetBankId, List<Long> cardIds, Long userId) {
        // 验证目标题库是否存在
        QuestionBank targetBank = questionBankMapper.selectById(targetBankId);
        if (targetBank == null) {
            throw new RuntimeException("目标题库不存在");
        }
        
        // 检查权限：system类型题库允许所有用户添加卡片，custom类型题库只能创建者添加
        if ("custom".equals(targetBank.getType())) {
            String userIdStr = String.valueOf(userId);
            if (targetBank.getUserId() == null || !targetBank.getUserId().equals(userIdStr)) {
                throw new RuntimeException("无权限向此题库添加卡片");
            }
        } else if ("ai".equals(targetBank.getType())) {
            throw new RuntimeException("AI生成的题库不允许手动添加卡片");
        }
        
        // 获取要复制的卡片
        List<QuestionCard> newCards = new ArrayList<>();
        java.util.Date now = new java.util.Date();
        
        for (Long cardId : cardIds) {
            QuestionCard originalCard = questionCardMapper.selectById(cardId);
            if (originalCard != null) {
                QuestionCard newCard = new QuestionCard();
                newCard.setQuestion(originalCard.getQuestion());
                newCard.setAnswer(originalCard.getAnswer());
                newCard.setQuestionImage(originalCard.getQuestionImage());
                newCard.setAnswerImage(originalCard.getAnswerImage());
                newCard.setBankId(targetBankId);
                newCard.setCreatedAt(now);
                newCard.setUpdatedAt(now);
                newCards.add(newCard);
            }
        }
        
        if (newCards.isEmpty()) {
            throw new RuntimeException("没有找到有效的卡片");
        }
        
        // 批量插入新卡片
        questionCardMapper.batchInsert(newCards);
        
        // 更新题库统计信息
        updateBankStatistics(targetBankId);
        
        return newCards.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 批量添加卡片内容到指定题库（用于AI生成的临时卡片）
     */
    @Transactional
    public List<QuestionCardDTO> addCardContentsToBank(Long targetBankId, List<Map<String, String>> cardContents, Long userId) {
        // 验证目标题库是否存在
        QuestionBank targetBank = questionBankMapper.selectById(targetBankId);
        if (targetBank == null) {
            throw new RuntimeException("目标题库不存在");
        }
        
        // 检查权限：system类型题库允许所有用户添加卡片，custom类型题库只能创建者添加
        if ("custom".equals(targetBank.getType())) {
            String userIdStr = String.valueOf(userId);
            if (targetBank.getUserId() == null || !targetBank.getUserId().equals(userIdStr)) {
                throw new RuntimeException("无权限向此题库添加卡片");
            }
        } else if ("ai".equals(targetBank.getType())) {
            throw new RuntimeException("AI生成的题库不允许手动添加卡片");
        }
        
        // 创建新卡片
        List<QuestionCard> newCards = new ArrayList<>();
        java.util.Date now = new java.util.Date();
        
        for (Map<String, String> content : cardContents) {
            QuestionCard newCard = new QuestionCard();
            newCard.setQuestion(content.get("question"));
            newCard.setAnswer(content.get("answer"));
            newCard.setQuestionImage(content.get("questionImage"));
            newCard.setAnswerImage(content.get("answerImage"));
            newCard.setBankId(targetBankId);
            newCard.setCreatedAt(now);
            newCards.add(newCard);
        }
        
        if (newCards.isEmpty()) {
            throw new RuntimeException("没有有效的卡片内容");
        }
        
        // 批量插入新卡片
        questionCardMapper.batchInsert(newCards);
        
        // 更新题库统计信息
        updateBankStatistics(targetBankId);
        
        return newCards.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 为卡片JSON添加图片描述
     */
    public String addImageDescriptions(String cardsJson) {
        try {
            // 清理JSON文本中的控制字符
            cardsJson = cardsJson.replaceAll("\\p{Cntrl}", " ");
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cardsArray = mapper.readTree(cardsJson);
            
            List<Object> cardsWithImages = new ArrayList<>();
            
            for (JsonNode card : cardsArray) {
                String question = card.get("question").asText();
                String answer = card.get("answer").asText();
                
                // 先生成图片描述，再生成真实图片
                String questionDesc = volcEngineService.generateImageDescription(question);
                String questionImageUrl = null;
                if (questionDesc != null) {
                    // 使用1024x1024尺寸(1048576像素),满足火山引擎最小921600像素要求
                    questionImageUrl = volcEngineService.generateImage(questionDesc, 1, "1024x1024");
                }
                
                String answerDesc = volcEngineService.generateImageDescription(answer);
                String answerImageUrl = null;
                if (answerDesc != null) {
                    // 使用1024x1024尺寸(1048576像素),满足火山引擎最小921600像素要求
                    answerImageUrl = volcEngineService.generateImage(answerDesc, 1, "1024x1024");
                }
                
                // 构建带图片URL的卡片对象
                java.util.Map<String, String> cardWithImage = new java.util.HashMap<>();
                cardWithImage.put("question", question);
                cardWithImage.put("answer", answer);
                cardWithImage.put("questionImage", questionImageUrl);
                cardWithImage.put("answerImage", answerImageUrl);
                
                cardsWithImages.add(cardWithImage);
            }
            
            return mapper.writeValueAsString(cardsWithImages);
        } catch (Exception e) {
            log.error("添加图片描述失败", e);
            return cardsJson; // 失败时返回原始JSON
        }
    }

    /**
     * 从Excel导入题库卡片
     */
    @Transactional
    public QuestionBankDTO importBankFromExcel(MultipartFile file, String bankName, String description, String difficulty, String language, Long userId) throws IOException {
        log.info("开始导入Excel文件: {}", file.getOriginalFilename());

        // 创建新题库
        QuestionBank bank = new QuestionBank();
        bank.setName(bankName != null && !bankName.trim().isEmpty() ? bankName : "导入题库 - " + new java.util.Date());
        bank.setDescription(description != null && !description.trim().isEmpty() ? description : "从Excel文件导入的题库");
        bank.setTopic("通用"); // 设置默认主题
        bank.setType("custom");
        bank.setDifficulty(difficulty != null && !difficulty.trim().isEmpty() ? difficulty : "medium"); // 设置难度
        bank.setLanguage(language != null && !language.trim().isEmpty() ? language : "中文"); // 设置语言
        bank.setUserId(String.valueOf(userId));
        bank.setCreatedAt(new java.util.Date());
        questionBankMapper.insert(bank);
        log.info("创建题库成功: {}", bank.getName());

        // 读取Excel文件
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        List<QuestionCard> cards = new ArrayList<>();
        java.util.Date now = new java.util.Date();

        // 从第二行开始读取(跳过表头)
        int successCount = 0;
        int skipCount = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                skipCount++;
                continue;
            }

            // 读取问题和答案(必填)
            Cell questionCell = row.getCell(0);
            Cell answerCell = row.getCell(1);

            if (questionCell == null || answerCell == null) {
                log.warn("第{}行缺少必填字段,跳过", i + 1);
                skipCount++;
                continue;
            }

            String question = getCellValueAsString(questionCell);
            String answer = getCellValueAsString(answerCell);

            if (question.trim().isEmpty() || answer.trim().isEmpty()) {
                log.warn("第{}行问题或答案为空,跳过", i + 1);
                skipCount++;
                continue;
            }

            // 创建卡片
            QuestionCard card = new QuestionCard();
            card.setBankId(bank.getId());
            card.setQuestion(question);
            card.setAnswer(answer);

            // 读取可选的图片URL
            Cell questionImageCell = row.getCell(2);
            if (questionImageCell != null) {
                String questionImage = getCellValueAsString(questionImageCell);
                if (!questionImage.trim().isEmpty()) {
                    card.setQuestionImage(questionImage);
                }
            }

            Cell answerImageCell = row.getCell(3);
            if (answerImageCell != null) {
                String answerImage = getCellValueAsString(answerImageCell);
                if (!answerImage.trim().isEmpty()) {
                    card.setAnswerImage(answerImage);
                }
            }

            card.setCreatedAt(now);
            cards.add(card);
            successCount++;
        }

        workbook.close();

        // 批量插入卡片
        if (!cards.isEmpty()) {
            for (QuestionCard card : cards) {
                questionCardMapper.insert(card);
            }
        }

        // 更新题库卡片数量
        bank.setCardCount(cards.size());
        questionBankMapper.updateById(bank);

        log.info("Excel导入完成: 题库={}, 成功导入{}张卡片, 跳过{}行", bank.getName(), successCount, skipCount);

        // 返回题库信息
        return convertToBankDTO(bank);
    }

    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    /**
     * 导出题库卡片为Excel
     */
    public void exportBankToExcel(Long bankId, Long userId, HttpServletResponse response) throws IOException {
        // 获取题库信息
        QuestionBank bank = questionBankMapper.selectById(bankId);
        if (bank == null) {
            throw new RuntimeException("题库不存在");
        }



        // 获取卡片列表
        List<QuestionCard> cards = questionCardMapper.selectByBankId(bankId);

        // 创建Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("题库卡片");

        // 创建标题样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 创建内容样式
        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setWrapText(true);
        contentStyle.setVerticalAlignment(VerticalAlignment.TOP);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"序号", "问题", "答案", "问题图片URL", "答案图片URL"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        for (QuestionCard card : cards) {
            Row row = sheet.createRow(rowNum++);

            Cell cell0 = row.createCell(0);
            cell0.setCellValue(rowNum - 1);
            cell0.setCellStyle(contentStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(card.getQuestion());
            cell1.setCellStyle(contentStyle);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(card.getAnswer());
            cell2.setCellStyle(contentStyle);

            Cell cell3 = row.createCell(3);
            cell3.setCellValue(card.getQuestionImage() != null ? card.getQuestionImage() : "");
            cell3.setCellStyle(contentStyle);

            Cell cell4 = row.createCell(4);
            cell4.setCellValue(card.getAnswerImage() != null ? card.getAnswerImage() : "");
            cell4.setCellStyle(contentStyle);
        }

        // 自动调整列宽
        sheet.setColumnWidth(0, 2000);  // 序号列
        sheet.setColumnWidth(1, 10000); // 问题列
        sheet.setColumnWidth(2, 10000); // 答案列
        sheet.setColumnWidth(3, 8000);  // 问题图片URL列
        sheet.setColumnWidth(4, 8000);  // 答案图片URL列

        // 设置响应头
        String fileName = URLEncoder.encode(bank.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

        // 写入响应流
        try (OutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            outputStream.flush();
        } finally {
            workbook.close();
        }

        log.info("题库 {} 导出成功,共 {} 张卡片", bank.getName(), cards.size());
    }
    
    /**
     * 更新题库信息
     */
    @Transactional
    public void updateBank(Long bankId, QuestionBankDTO bankDTO, Long userId) {
        // 查询题库
        QuestionBank bank = questionBankMapper.findById(bankId);
        if (bank == null) {
            throw new RuntimeException("题库不存在");
        }
        
        // 检查权限：只有创建者可以编辑自定义题库
        if ("custom".equals(bank.getType())) {
            if (bank.getUserId() == null || !bank.getUserId().equals(String.valueOf(userId))) {
                throw new RuntimeException("无权限编辑此题库");
            }
        }
        
        // AI题库不允许编辑
        if ("ai".equals(bank.getType())) {
            throw new RuntimeException("AI题库不允许编辑");
        }
        
        // 更新题库信息
        bank.setName(bankDTO.getName());
        bank.setDescription(bankDTO.getDescription());
        bank.setDifficulty(bankDTO.getDifficulty());
        bank.setLanguage(bankDTO.getLanguage());
        bank.setTags(bankDTO.getTags());
        bank.setUpdatedAt(new java.util.Date());
        
        questionBankMapper.update(bank);
        
        // 清除缓存
        questionBankCache.clear();
        
        log.info("题库 {} 更新成功", bankId);
    }
    
    /**
     * 删除题库
     */
    @Transactional
    public void deleteBank(Long bankId, Long userId) {
        // 查询题库
        QuestionBank bank = questionBankMapper.findById(bankId);
        if (bank == null) {
            throw new RuntimeException("题库不存在");
        }
        
        log.info("删除题库权限检查 - bankId: {}, bank.getType(): {}, bank.getUserId(): {}, userId: {}", 
                bankId, bank.getType(), bank.getUserId(), userId);
        
        // 检查权限：只有创建者可以删除自定义题库
        if ("custom".equals(bank.getType())) {
            if (bank.getUserId() == null || !bank.getUserId().equals(String.valueOf(userId))) {
                throw new RuntimeException("无权限删除此题库");
            }
        }
        
        // AI题库不允许删除
        if ("ai".equals(bank.getType())) {
            throw new RuntimeException("AI题库不允许删除");
        }
        
        // 删除题库中的所有卡片
        questionCardMapper.deleteByBankId(bankId);
        
        // 删除题库
        questionBankMapper.delete(bankId);
        
        // 清除缓存
        questionBankCache.clear();
        
        log.info("题库 {} 删除成功", bankId);
    }
    
    /**
     * 删除单个题库卡片
     */
    @Transactional
    public void deleteCard(Long cardId, Long userId) {
        // 查询卡片
        QuestionCard card = questionCardMapper.selectById(cardId);
        if (card == null) {
            throw new RuntimeException("卡片不存在");
        }
        
        // 查询卡片所属题库
        QuestionBank bank = questionBankMapper.findById(card.getBankId());
        if (bank == null) {
            throw new RuntimeException("题库不存在");
        }
        
        // 检查权限：只有自定义题库的创建者可以删除卡片
        if ("custom".equals(bank.getType())) {
            if (bank.getUserId() == null || !bank.getUserId().equals(String.valueOf(userId))) {
                throw new RuntimeException("无权限删除此卡片");
            }
        } else if ("ai".equals(bank.getType())) {
            // AI题库不允许删除卡片
            throw new RuntimeException("AI题库的卡片不允许删除");
        }
        // system类型题库允许所有用户删除卡片
        
        // 删除卡片
        questionCardMapper.deleteById(cardId);
        
        // 更新题库卡片数
        bank.setCardCount(bank.getCardCount() - 1);
        bank.setUpdatedAt(new java.util.Date());
        questionBankMapper.update(bank);
        
        // 清除缓存
        questionBankCache.clear();
        
        log.info("卡片 {} 删除成功，题库 {} 卡片数更新为 {}", cardId, bank.getId(), bank.getCardCount());
    }
    
    /**
     * 新增单个卡片到题库
     */
    @Transactional
    public QuestionCardDTO addCard(Long bankId, String question, String answer,
                                    String questionImage, String answerImage,
                                    Long userId) {
        // 查询题库
        QuestionBank bank = questionBankMapper.findById(bankId);
        if (bank == null) {
            throw new RuntimeException("题库不存在");
        }
        
        // 调试日志
        log.info("添加卡片权限检查 - 题库ID: {}, 题库userId: {}, 当前userId: {}, 题库类型: {}", 
                 bankId, bank.getUserId(), userId, bank.getType());
        
        // 检查权限：system类型题库允许所有用户添加卡片，custom类型题库只能创建者添加
        if ("custom".equals(bank.getType())) {
            // 自定义题库：只有创建者可以添加卡片
            String userIdStr = String.valueOf(userId);
            if (bank.getUserId() == null || !bank.getUserId().equals(userIdStr)) {
                log.error("权限检查失败 - 题库userId: {} ({}), 当前userId: {} ({})",
                         bank.getUserId(), bank.getUserId() == null ? "null" : bank.getUserId().getClass().getName(),
                         userId, userId == null ? "null" : userId.getClass().getName());
                throw new RuntimeException("无权限向此题库添加卡片");
            }
        } else if ("ai".equals(bank.getType())) {
            // AI生成的题库不允许添加卡片
            throw new RuntimeException("AI生成的题库不允许手动添加卡片");
        }
        // system类型题库允许所有用户添加卡片，无需检查权限
        
        // 处理图片上传到MinIO
        if (questionImage != null && questionImage.startsWith("data:image")) {
            questionImage = minioService.uploadBase64Image(questionImage);
        }
        if (answerImage != null && answerImage.startsWith("data:image")) {
            answerImage = minioService.uploadBase64Image(answerImage);
        }
        
        // 创建新卡片
        QuestionCard card = new QuestionCard();
        card.setBankId(bankId);
        card.setQuestion(question);
        card.setAnswer(answer);
        card.setQuestionImage(questionImage);
        card.setAnswerImage(answerImage);
        card.setCreatedAt(new java.util.Date());
        card.setUpdatedAt(new java.util.Date());
        
        // 保存卡片
        questionCardMapper.insert(card);
        
        // 更新题库卡片数
        bank.setCardCount(bank.getCardCount() + 1);
        bank.setUpdatedAt(new java.util.Date());
        questionBankMapper.update(bank);
        
        // 清除缓存
        questionBankCache.clear();
        
        log.info("卡片添加成功到题库 {}", bankId);
        
        return convertToDTO(card);
    }
}