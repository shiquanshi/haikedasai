package com.knowledge.questioncard.util;

import java.util.regex.Pattern;

/**
 * SSE流式分词工具类
 * 实现英文单词边界检测，确保单词完整性
 */
public class StreamTokenizer {
    
    // 缓冲区，存储不完整的内容
    private final StringBuilder buffer = new StringBuilder();
    
    // 英文单词边界正则：字母、数字、连字符、撇号
    private static final Pattern WORD_CHAR = Pattern.compile("[a-zA-Z0-9'-]");
    
    // JSON特殊字符
    private static final String JSON_SPECIAL_CHARS = "{}[]\",:\n\r\t";
    
    /**
     * 处理增量内容，返回可以立即发送的部分
     * 
     * @param incrementalContent 从AI返回的增量内容
     * @return 可以安全发送的内容（保证英文单词完整性）
     */
    public String processIncrement(String incrementalContent) {
        if (incrementalContent == null || incrementalContent.isEmpty()) {
            return "";
        }
        
        // 将增量内容添加到缓冲区
        buffer.append(incrementalContent);
        
        // 查找最后一个安全的分割点
        int safeEndIndex = findSafeEndIndex(buffer.toString());
        
        if (safeEndIndex <= 0) {
            // 没有找到安全分割点，暂时不发送
            return "";
        }
        
        // 提取可以安全发送的部分
        String toSend = buffer.substring(0, safeEndIndex);
        
        // 从缓冲区移除已发送的部分
        buffer.delete(0, safeEndIndex);
        
        return toSend;
    }
    
    /**
     * 获取缓冲区剩余内容（流结束时调用）
     */
    public String flush() {
        String remaining = buffer.toString();
        buffer.setLength(0);
        return remaining;
    }
    
    /**
     * 查找安全的分割点
     * 返回可以安全切分的最大索引（不包含该位置）
     */
    private int findSafeEndIndex(String content) {
        int length = content.length();
        
        if (length == 0) {
            return 0;
        }
        
        // 从后往前查找安全分割点
        for (int i = length - 1; i >= 0; i--) {
            char c = content.charAt(i);
            
            // 1. JSON特殊字符后面是安全的
            if (JSON_SPECIAL_CHARS.indexOf(c) >= 0) {
                return i + 1;
            }
            
            // 2. 空格后面是安全的
            if (Character.isWhitespace(c)) {
                return i + 1;
            }
            
            // 3. 如果当前是单词字符，但下一个不是，也是安全的
            if (i < length - 1) {
                char next = content.charAt(i + 1);
                boolean currentIsWord = WORD_CHAR.matcher(String.valueOf(c)).matches();
                boolean nextIsWord = WORD_CHAR.matcher(String.valueOf(next)).matches();
                
                // 单词字符 -> 非单词字符：安全边界
                if (currentIsWord && !nextIsWord) {
                    return i + 1;
                }
            }
        }
        
        // 如果整个内容都是单词字符，保留最后10个字符在缓冲区
        // 防止单词被截断
        if (length > 10) {
            return length - 10;
        }
        
        return 0;
    }
    
    /**
     * 重置tokenizer状态
     */
    public void reset() {
        buffer.setLength(0);
    }
    
    /**
     * 获取当前缓冲区大小
     */
    public int getBufferSize() {
        return buffer.length();
    }
}