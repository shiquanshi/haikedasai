package com.knowledge.questioncard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 火山引擎配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "volcengine.api")
public class VolcEngineConfig {
    /**
     * API密钥
     */
    private String key;
    
    /**
     * API端点
     */
    private String endpoint;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 图片生成模型名称
     */
    private String imageModel;
}