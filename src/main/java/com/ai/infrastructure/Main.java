package com.ai.infrastructure;

import com.ai.infrastructure.core.AIInfrastructureApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 主类，应用入口点
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        logger.info("Starting AI Infrastructure Application...");
        
        try {
            // 创建应用实例
            AIInfrastructureApplication application = new AIInfrastructureApplication();
            application.start();
            
            // 执行一些示例任务
            executeSampleTasks(application);
            
            // 关闭应用
            application.shutdown();
            
            logger.info("AI Infrastructure Application finished successfully");
        } catch (Exception e) {
            logger.error("Error running AI Infrastructure Application", e);
            System.exit(1);
        }
    }
    
    /**
     * 执行示例任务
     * @param application 应用实例
     */
    private static void executeSampleTasks(AIInfrastructureApplication application) {
        logger.info("--- Executing Sample Tasks ---");
        
        try {
            // 任务1: 简单任务
            CompletableFuture<String> task1 = application.executeTask("Calculate 2+2");
            logger.info("Task 1 result: " + task1.join());
            
            // 任务2: 复杂任务
            CompletableFuture<String> task2 = application.executeTask("Analyze complex data set with statistics");
            logger.info("Task 2 result: " + task2.join());
            
            // 任务3: 文件读取任务
            CompletableFuture<String> task3 = application.executeTask("Read configuration file");
            logger.info("Task 3 result: " + task3.join());
            
            // 任务4: 搜索任务
            CompletableFuture<String> task4 = application.executeTask("Search for documentation about AI agents");
            logger.info("Task 4 result: " + task4.join());
            
            logger.info("--- Sample Tasks Completed ---");
        } catch (Exception e) {
            logger.error("Error executing sample tasks", e);
        }
    }
}