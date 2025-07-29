package com.ai.infrastructure;

import com.ai.infrastructure.core.AIInfrastructureApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.Scanner;

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
            
            // 检查是否提供了API密钥作为参数
            if (args.length > 0) {
                application.setOpenAIModelApiKey(args[0]);
                logger.info("OpenAI model API key set from command line argument");
            }
            
            // 执行一些示例任务
            executeSampleTasks(application);
            
            // 启动交互式模式（如果提供了API密钥）
            if (args.length > 0) {
                startInteractiveMode(application);
            }
            
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
    
    /**
     * 启动交互式模式
     * @param application 应用实例
     */
    private static void startInteractiveMode(AIInfrastructureApplication application) {
        logger.info("--- Starting Interactive Mode ---");
        logger.info("Enter your tasks (type 'quit' to exit):");
        
        Scanner scanner = new Scanner(System.in);
        String input;
        
        while (true) {
            System.out.print("> ");
            input = scanner.nextLine();
            
            if ("quit".equalsIgnoreCase(input)) {
                break;
            }
            
            if (!input.trim().isEmpty()) {
                try {
                    CompletableFuture<String> result = application.executeTask(input);
                    String response = result.join();
                    System.out.println(response);
                } catch (Exception e) {
                    logger.error("Error executing task: " + e.getMessage());
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        
        logger.info("Exiting interactive mode");
    }
}