package com.ai.infrastructure;

import com.ai.infrastructure.scheduler.TaskScheduler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * TaskScheduler功能演示
 * 展示增强的任务调度器的抢占式调度能力
 */
public class Demo {
    
    public static void main(String[] args) {
        System.out.println("=== TaskScheduler增强功能演示 ===\n");
        
        // 创建任务调度器，最大并发数为2
        TaskScheduler scheduler = new TaskScheduler(2);
        
        try {
            // 演示1: 基本任务调度
            System.out.println("1. 基本任务调度演示:");
            Function<String, String> basicProcessor = task -> {
                System.out.println("  执行任务: " + task);
                try {
                    Thread.sleep(1000); // 模拟任务处理时间
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "完成: " + task;
            };
            
            String result1 = scheduler.scheduleTask("基本任务1", basicProcessor);
            System.out.println("  结果: " + result1 + "\n");
            
            // 演示2: 带优先级的任务调度
            System.out.println("2. 优先级任务调度演示:");
            Function<String, String> priorityProcessor = task -> {
                System.out.println("  执行优先级任务: " + task);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "完成优先级任务: " + task;
            };
            
            // 启动低优先级任务（在新线程中）
            CompletableFuture<String> lowPriorityFuture = CompletableFuture.supplyAsync(() -> 
                scheduler.scheduleTask("低优先级任务", priorityProcessor, 1));
            
            // 启动高优先级任务（在新线程中）
            CompletableFuture<String> highPriorityFuture = CompletableFuture.supplyAsync(() -> 
                scheduler.scheduleTask("高优先级任务", priorityProcessor, 10));
            
            try {
                String lowResult = lowPriorityFuture.get();
                String highResult = highPriorityFuture.get();
                System.out.println("  低优先级任务结果: " + lowResult);
                System.out.println("  高优先级任务结果: " + highResult);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            System.out.println();
            
            // 演示3: 任务状态监控
            System.out.println("3. 任务状态监控演示:");
            System.out.println("  最大并发数: " + scheduler.getMaxConcurrency());
            System.out.println("  运行中任务数: " + scheduler.getRunningTaskCount());
            System.out.println("  队列中任务数: " + scheduler.getQueuedTaskCount());
            System.out.println();
            
            // 演示4: 并发任务执行
            System.out.println("4. 并发任务执行演示:");
            Function<String, String> concurrentProcessor = task -> {
                System.out.println("  并发执行任务: " + task);
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "并发任务完成: " + task;
            };
            
            // 启动多个并发任务
            CompletableFuture<String> concurrentTask1 = CompletableFuture.supplyAsync(() -> 
                scheduler.scheduleTask("并发任务1", concurrentProcessor));
            CompletableFuture<String> concurrentTask2 = CompletableFuture.supplyAsync(() -> 
                scheduler.scheduleTask("并发任务2", concurrentProcessor));
            CompletableFuture<String> concurrentTask3 = CompletableFuture.supplyAsync(() -> 
                scheduler.scheduleTask("并发任务3", concurrentProcessor));
            
            try {
                String result2 = concurrentTask1.get();
                String result3 = concurrentTask2.get();
                String result4 = concurrentTask3.get();
                System.out.println("  " + result2);
                System.out.println("  " + result3);
                System.out.println("  " + result4);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            System.out.println();
            
        } finally {
            // 关闭调度器
            scheduler.shutdown();
        }
        
        System.out.println("=== 演示完成 ===");
    }
}