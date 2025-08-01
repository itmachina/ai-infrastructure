package com.ai.infrastructure;

import com.ai.infrastructure.core.AIInfrastructureApplication;
import com.ai.infrastructure.steering.RealtimeSteeringSystem;
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
        
        // 检查OpenAI API密钥是否配置
        String apiKey = System.getenv("AI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("OpenAI API key is required. Please set AI_API_KEY environment variable.");
            logger.error("Example: set AI_API_KEY=your_openai_api_key");
            logger.error("Or: export AI_API_KEY=your_openai_api_key");
            System.exit(1);
        }
        
        try {
            // 创建应用实例
            AIInfrastructureApplication application = new AIInfrastructureApplication(System.getenv("AI_API_KEY"));
            application.start();

            // 启动持续运行模式
            startContinuousMode(application);
        } catch (Exception e) {
            logger.error("Error running AI Infrastructure Application", e);
            System.exit(1);
        }
    }


    
    /**
     * 启动持续运行模式
     * @param application 应用实例
     */
    private static void startContinuousMode(AIInfrastructureApplication application) {
        logger.info("--- Starting Continuous Mode ---");
        logger.info("AI Infrastructure Application is now running continuously...");
        logger.info("Commands:");
        logger.info("  'help' - Show available commands");
        logger.info("  'status' - Show system status");
        logger.info("  'tasks' - Show task statistics");
        logger.info("  'quit' - Shutdown the application");
        logger.info("");
        
        Scanner scanner = new Scanner(System.in);
        String input;
        
        // 添加钩子，确保应用正常关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered, shutting down gracefully...");
            application.shutdown();
        }));
        
        while (true) {
            System.out.print("[AI-Infrastructure]> ");
            input = scanner.nextLine();
            
            if ("quit".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                logger.info("Shutting down AI Infrastructure Application...");
                break;
            }
            
            if ("help".equalsIgnoreCase(input)) {
                showHelp();
                continue;
            }
            
            if ("status".equalsIgnoreCase(input)) {
                showStatus(application);
                continue;
            }
            
            if ("tasks".equalsIgnoreCase(input)) {
                showTaskStatistics(application);
                continue;
            }
            
            if (!input.trim().isEmpty()) {
                try {
                    long startTime = System.currentTimeMillis();
                    application.executeTask(input);
                    long endTime = System.currentTimeMillis();
                    logger.info("Task completed in {}ms", endTime - startTime);
                } catch (Exception e) {
                    logger.error("Error executing task: " + e.getMessage());
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        
        // 正常关闭应用
        application.shutdown();
        logger.info("AI Infrastructure Application shutdown completed");
    }
    
    /**
     * 显示帮助信息
     */
    private static void showHelp() {
        System.out.println("\n=== AI Infrastructure Help ===");
        System.out.println("Available commands:");
        System.out.println("  help        - Show this help message");
        System.out.println("  status      - Show system status");
        System.out.println("  tasks       - Show task execution statistics");
        System.out.println("  quit        - Shutdown the application");
        System.out.println("  [any task]  - Execute the specified task using OpenAI model");
        System.out.println("\nRequirements:");
        System.out.println("  - OpenAI API key must be set (AI_API_KEY environment variable)");
        System.out.println("  - Internet connection required");
        System.out.println("\nExamples:");
        System.out.println("  Calculate 2+2");
        System.out.println("  What is the weather today?");
        System.out.println("  Write a Python function to sort an array");
        System.out.println("  Analyze this data and provide insights");
        System.out.println("  Help me debug this Java code");
        System.out.println("");
    }
    
    /**
     * 显示系统状态
     */
    private static void showStatus(AIInfrastructureApplication application) {
        System.out.println("\n=== System Status ===");
        RealtimeSteeringSystem steeringSystem = application.getRealtimeSteeringSystem();
        System.out.println("Realtime Steering System: " + steeringSystem.getStatusInfo());
        System.out.println("Memory Manager: Active");
        System.out.println("Security Manager: Active");
        System.out.println("Continuous Execution: Active");
        System.out.println("AI Mode: OpenAI GPT Integration");
        System.out.println("");
    }
    
    /**
     * 显示任务统计信息
     */
    private static void showTaskStatistics(AIInfrastructureApplication application) {
        System.out.println("\n=== Task Statistics ===");
        RealtimeSteeringSystem steeringSystem = application.getRealtimeSteeringSystem();
        
        System.out.println("AI Infrastructure Configuration:");
        System.out.println("  - AI Model: OpenAI GPT");
        System.out.println("  - Execution Mode: Continuous");
        System.out.println("  - Memory Management: 3-layer Architecture");
        System.out.println("  - Security: 6-layer Protection");
        System.out.println("  - Context: Persistent Memory");
        System.out.println("");
        System.out.println("System Components:");
        System.out.println("  - OpenAI Model Client: Active");
        System.out.println("  - Memory Manager: Active");
        System.out.println("  - Security Manager: Active");
        System.out.println("  - Continuous Execution: Active");
        System.out.println("  - Task Scheduler: Active");
        System.out.println("");
        System.out.println("Features:");
        System.out.println("  - Real-time conversation processing");
        System.out.println("  - Intelligent context management");
        System.out.println("  - Enhanced error recovery");
        System.out.println("  - Security validation");
        System.out.println("");
    }
}