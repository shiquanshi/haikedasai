package com.knowledge.questioncard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
        // 拒绝策略:由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲时间(秒)
        executor.setKeepAliveSeconds(60);
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间(秒)
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}