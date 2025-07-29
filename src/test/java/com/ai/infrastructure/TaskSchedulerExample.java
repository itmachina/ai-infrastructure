package com.ai.infrastructure;

import com.ai.infrastructure.scheduler.TaskScheduler;

import java.util.function.Function;

/**
 * TaskScheduler使用示例
 * 展示如何使用增强的任务调度器实现抢占式调度
 */
public class TaskSchedulerExample {
    
    public static void main(String[] args) {
        // 创建任务调度器，最大并发数为2
        TaskScheduler scheduler = new TaskScheduler(2);
        
        try {
            // 示例1: 基本任务调度
            System.out.println("=== 基本任务调度 ===");
            Function<String, String> basicProcessor = task -> {
                System.out.println("处理任务: " + task);
                try {
                    Thread.sleep(1000); // 模拟任务处理时间
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "完成: " + task;
            };
            
            String result1 = scheduler.scheduleTask("基本任务1", basicProcessor);
            System.out.println("结果: " + result1);
            
            // 示例2: 带优先级的任务调度
            System.out.println("\n=== 带优先级的任务调度 ===");
            Function<String, String> priorityProcessor = task -> {
                System.out.println("处理优先级任务: " + task);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "完成优先级任务: " + task;
            };
            
            // 调度低优先级任务
            String result2 = scheduler.scheduleTask("低优先级任务", priorityProcessor, 1);
            System.out.println("低优先级任务结果: " + result2);
            
            // 调度高优先级任务
            String result3 = scheduler.scheduleTask("高优先级任务", priorityProcessor, 10);
            System.out.println("高优先级任务结果: " + result3);
            
            // 示例3: 获取调度器状态
            System.out.println("\n=== 调度器状态 ===");
            System.out.println("最大并发数: " + scheduler.getMaxConcurrency());
            System.out.println("运行中任务数: " + scheduler.getRunningTaskCount());
            System.out.println("队列中任务数: " + scheduler.getQueuedTaskCount());
            
        } finally {
            // 关闭调度器
            scheduler.shutdown();
        }
    }
}