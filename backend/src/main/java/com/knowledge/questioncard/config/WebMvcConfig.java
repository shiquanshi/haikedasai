package com.knowledge.questioncard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final JwtInterceptor jwtInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**", "/question-bank/**") // 拦截/api和/question-bank路径
                .excludePathPatterns(
                    "/api/user/register",  // 排除注册
                    "/api/user/login",     // 排除登录
                    "/api/user/validate-token", // 排除token验证
                    "/api/share/plaza",  // 排除分享广场(公开访问)
                    "/api/share/detail/**",  // 排除分享详情(公开访问)
                    "/api/battle/**",  // 排除对战相关API
                    "/question-bank/share/**"  // 排除分享相关API
                );
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:3000", "http://localhost:3001", "http://localhost:3002")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    /**
     * 配置异步请求支持
     * 设置异步请求超时时间为10分钟，与SseEmitter超时时间保持一致
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步请求超时时间为10分钟（600000毫秒）
        configurer.setDefaultTimeout(600000L);
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册JavaTimeModule以支持Java 8日期时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 设置时区为东八区（Asia/Shanghai），解决日期时间序列化时的时区问题
        objectMapper.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
        // 设置序列化时包含null值（默认行为，显式设置以确保一致性）
        objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
        return objectMapper;
    }
}