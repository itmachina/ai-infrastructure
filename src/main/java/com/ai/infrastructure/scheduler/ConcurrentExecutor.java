package com.ai.infrastructure.scheduler;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * 并发执行器 - 基于Claude Code的UH1函数实现
 * 支持异步生成器的并发执行
 */
public class ConcurrentExecutor {
    
    /**
     * 并发执行多个异步生成器
     * @param generators 异步生成器列表
     * @param maxConcurrency 最大并发数
     * @param <T> 生成器返回的类型
     * @return CompletableFuture<List<T>> 所有结果的列表
     */
    public static <T> CompletableFuture<List<T>> executeConcurrently(List<CompletableFuture<T>> generators, int maxConcurrency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 限制并发数
                int actualConcurrency = Math.min(maxConcurrency, generators.size());
                
                // 使用信号量控制并发
                java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(actualConcurrency);
                
                // 收集所有结果
                List<T> results = new ArrayList<>();
                
                // 批量执行所有生成器
                @SuppressWarnings("unchecked")
                CompletableFuture<Void>[] futures = new CompletableFuture[generators.size()];
                for (int i = 0; i < generators.size(); i++) {
                    final int index = i;
                    futures[i] = CompletableFuture.runAsync(() -> {
                        try {
                            semaphore.acquire();
                            T result = generators.get(index).join();
                            synchronized (results) {
                                results.add(result);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        } finally {
                            semaphore.release();
                        }
                    });
                }
                
                // 等待所有任务完成
                CompletableFuture.allOf(futures).join();
                
                return results;
            } catch (Exception e) {
                throw new RuntimeException("Concurrent execution failed", e);
            }
        });
    }
    
    /**
     * 并发执行调度器 - 基于Claude Code的UH1函数实现
     * 支持异步生成器的并发执行，实现真正的流式处理
     * @param generators 异步生成器迭代器
     * @param maxConcurrency 最大并发数
     * @param <T> 生成器返回的类型
     * @return CompletableFuture<Void>
     */
    public static <T> CompletableFuture<Void> executeWithConcurrencyControl(Iterator<CompletableFuture<T>> generators, int maxConcurrency) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 使用队列管理活跃的Promise
                Queue<CompletableFuture<T>> activePromises = new ConcurrentLinkedQueue<>();
                Set<CompletableFuture<T>> completedPromises = new HashSet<>();
                
                // 启动初始的并发任务
                int startedCount = 0;
                while (startedCount < maxConcurrency && generators.hasNext()) {
                    CompletableFuture<T> generator = generators.next();
                    activePromises.offer(generator);
                    startedCount++;
                }
                
                // 并发执行循环
                while (!activePromises.isEmpty()) {
                    // 等待任何一个生成器产生结果
                    CompletableFuture<T> completedFuture = awaitAny(activePromises, 30, TimeUnit.SECONDS);
                    if (completedFuture != null) {
                        // 移除已完成的Promise
                        activePromises.remove(completedFuture);
                        completedPromises.add(completedFuture);
                        
                        // 如果还有未启动的生成器，启动新的生成器
                        if (generators.hasNext()) {
                            CompletableFuture<T> nextGenerator = generators.next();
                            activePromises.offer(nextGenerator);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Concurrent execution with control failed", e);
            }
        });
    }
    
    /**
     * 等待任意一个CompletableFuture完成
     * @param futures CompletableFuture队列
     * @param timeout 超时时间
     * @param unit 时间单位
     * @param <T> 类型
     * @return CompletableFuture<T> 完成的future，如果超时则返回null
     */
    private static <T> CompletableFuture<T> awaitAny(Queue<CompletableFuture<T>> futures, long timeout, TimeUnit unit) {
        try {
            // 创建一个CompletableFuture数组
            @SuppressWarnings("unchecked")
            CompletableFuture<T>[] futureArray = futures.toArray(new CompletableFuture[0]);
            
            // 使用anyOf等待任意一个完成
            CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(futureArray);
            
            // 等待完成或超时
            anyFuture.get(timeout, unit);
            
            // 找到完成的future并返回
            for (CompletableFuture<T> future : futures) {
                if (future.isDone()) {
                    return future;
                }
            }
            
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException e) {
            return null;
        }
    }
}