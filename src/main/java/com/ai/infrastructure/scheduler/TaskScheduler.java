package com.ai.infrastructure.scheduler;

import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 任务调度器，支持并发控制
 */
public class TaskScheduler {
    private final ExecutorService executorService;
    private final Semaphore concurrencyLimiter;
    private final int maxConcurrency;
    
    public TaskScheduler(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
        this.executorService = Executors.newCachedThreadPool();
        this.concurrencyLimiter = new Semaphore(maxConcurrency);
    }
    
    /**
     * 调度任务执行
     * @param task 任务描述
     * @param taskProcessor 任务处理器
     * @return 执行结果
     */
    public String scheduleTask(String task, Function<String, String> taskProcessor) {
        try {
            // 获取执行许可
            concurrencyLimiter.acquire();
            
            // 提交任务执行
            Future<String> future = executorService.submit(() -> {
                try {
                    return taskProcessor.apply(task);
                } finally {
                    // 释放执行许可
                    concurrencyLimiter.release();
                }
            });
            
            // 等待并返回结果
            return future.get(120, TimeUnit.SECONDS); // 120秒超时
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Task execution interrupted: " + e.getMessage();
        } catch (ExecutionException e) {
            return "Task execution failed: " + e.getCause().getMessage();
        } catch (TimeoutException e) {
            return "Task execution timed out: " + e.getMessage();
        }
    }
    
    /**
     * 获取最大并发数
     * @return 最大并发数
     */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }
    
    /**
     * 关闭调度器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}