package com.ai.infrastructure.example;

import com.ai.infrastructure.core.AIInfrastructureApplication;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * 持续执行功能演示示例
 * 展示如何使用AI基础设施的持续执行和对话管理功能
 */
public class ContinuousExecutionDemo {
    
    public static void main(String[] args) {
        System.out.println("=== AI基础设施持续执行功能演示 ===");
        
        // 创建应用实例
        AIInfrastructureApplication application = new AIInfrastructureApplication();
        application.start();
        
        // 如果提供了API密钥，设置它
        if (args.length > 0) {
            application.setOpenAIModelApiKey(args[0]);
            System.out.println("已设置OpenAI模型API密钥");
        } else {
            System.out.println("警告: 未提供API密钥，将使用回退机制");
        }
        
        // 演示单次任务执行
        demonstrateSingleTaskExecution(application);
        
        // 如果有API密钥，演示交互式对话
        if (args.length > 0) {
            demonstrateInteractiveConversation(application);
        }
        
        // 关闭应用
        application.shutdown();
        System.out.println("演示完成");
    }
    
    /**
     * 演示单次任务执行
     */
    private static void demonstrateSingleTaskExecution(AIInfrastructureApplication application) {
        System.out.println("\n--- 单次任务执行演示 ---");
        
        try {
            // 执行简单的计算任务
            CompletableFuture<String> task1 = application.executeTask("计算12345乘以6789的结果");
            System.out.println("任务1 - 计算结果: " + task1.join());
            
            // 执行知识问答任务
            CompletableFuture<String> task2 = application.executeTask("简要介绍人工智能的发展历史");
            System.out.println("任务2 - 问答结果: " + task2.join());
            
        } catch (Exception e) {
            System.err.println("执行单次任务时出错: " + e.getMessage());
        }
    }
    
    /**
     * 演示交互式对话
     */
    private static void demonstrateInteractiveConversation(AIInfrastructureApplication application) {
        System.out.println("\n--- 交互式对话演示 ---");
        System.out.println("输入您的问题或指令 (输入 'quit' 退出):");
        
        Scanner scanner = new Scanner(System.in);
        String input;
        
        while (true) {
            System.out.print("> ");
            input = scanner.nextLine();
            
            if ("quit".equalsIgnoreCase(input.trim())) {
                break;
            }
            
            if (!input.trim().isEmpty()) {
                try {
                    // 执行任务
                    CompletableFuture<String> result = application.executeTask(input);
                    String response = result.join();
                    
                    // 显示结果
                    System.out.println("AI助手: " + response);
                    System.out.println();
                } catch (Exception e) {
                    System.err.println("执行任务时出错: " + e.getMessage());
                }
            }
        }
        
        System.out.println("交互式对话演示结束");
    }
}