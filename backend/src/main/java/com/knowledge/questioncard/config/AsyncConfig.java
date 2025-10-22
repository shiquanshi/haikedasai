package com.knowledge.questioncard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 用于SSE流式生成等异步任务的线程池管理
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 配置SSE流式生成专用线程池
     * 核心线程数较大,避免blockingForEach阻塞导致超时
     */
    @Bean(name = "sseTaskExecutor")
    public Executor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(10);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀
        executor.setThreadNamePrefix("sse-task-");
        // 自定义拒绝策略:记录日志并尝试发送拒绝提示
        executor.setRejectedExecutionHandler(new CustomRejectedExecutionHandler());
        // 线程空闲时间(秒)
        executor.setKeepAliveSeconds(60);
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间(秒)
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    /**
     * 自定义拒绝策略
     * 用于在线程池和队列都满时处理拒绝任务
     */
    private static class CustomRejectedExecutionHandler implements java.util.concurrent.RejectedExecutionHandler {
        private static final Logger log = LoggerFactory.getLogger(CustomRejectedExecutionHandler.class);
        
        @Override
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor executor) {
            log.warn("❌ 线程池任务被拒绝执行: 当前活跃线程数={}, 队列大小={}", 
                    executor.getActiveCount(), executor.getQueue().size());
            
            // 使用CallerRunsPolicy作为兜底策略，确保任务能够执行
            // 但会在调用线程中执行，可能会导致调用线程阻塞
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(r, executor);
        }
    }
}