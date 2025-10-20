package com.knowledge.questioncard.cache;

import com.knowledge.questioncard.dto.QuestionCardDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 题库生成结果缓存
 * 减少重复AI调用,提升响应速度
 */
@Slf4j
@Component
public class QuestionBankCache {
    
    // 缓存结构: key -> CacheEntry
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    // 缓存过期时间(分钟)
    private static final int CACHE_EXPIRY_MINUTES = 30;
    
    // 最大缓存条目数
    private static final int MAX_CACHE_SIZE = 100;
    
    /**
     * 生成缓存键
     */
    public String generateKey(String topic, Integer cardCount, String difficulty, String language) {
        return String.format("%s_%d_%s_%s", topic, cardCount, difficulty, language);
    }
    
    /**
     * 获取缓存的生成结果
     */
    public List<QuestionCardDTO> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        // 检查是否过期
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("缓存已过期: {}", key);
            return null;
        }
        
        log.info("命中缓存: {}", key);
        return entry.getCards();
    }
    
    /**
     * 缓存生成结果
     */
    public void put(String key, List<QuestionCardDTO> cards) {
        // 如果缓存已满,清理过期条目
        if (cache.size() >= MAX_CACHE_SIZE) {
            cleanupExpired();
            
            // 如果清理后仍然满,移除最旧的条目
            if (cache.size() >= MAX_CACHE_SIZE) {
                String oldestKey = cache.entrySet().stream()
                    .min((e1, e2) -> e1.getValue().getCreateTime().compareTo(e2.getValue().getCreateTime()))
                    .map(Map.Entry::getKey)
                    .orElse(null);
                if (oldestKey != null) {
                    cache.remove(oldestKey);
                    log.debug("缓存已满,移除最旧条目: {}", oldestKey);
                }
            }
        }
        
        cache.put(key, new CacheEntry(cards));
        log.info("已缓存生成结果: {}", key);
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debug("已清理过期缓存,当前缓存大小: {}", cache.size());
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.clear();
        log.info("已清空所有缓存");
    }
    
    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final List<QuestionCardDTO> cards;
        private final Date createTime;
        
        public CacheEntry(List<QuestionCardDTO> cards) {
            this.cards = cards;
            this.createTime = new Date();
        }
        
        public List<QuestionCardDTO> getCards() {
            return cards;
        }
        
        public Date getCreateTime() {
            return createTime;
        }
        
        public boolean isExpired() {
            long now = System.currentTimeMillis();
            long expiryTime = createTime.getTime() + (CACHE_EXPIRY_MINUTES * 60 * 1000L);
            return now > expiryTime;
        }
    }
}