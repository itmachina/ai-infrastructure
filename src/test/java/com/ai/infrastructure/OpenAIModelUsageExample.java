package com.ai.infrastructure;

import com.ai.infrastructure.core.AIInfrastructureApplication;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * OpenAI风格大模型使用示例
 * 展示如何在AI基础设施中集成和使用OpenAI兼容的大模型，
 * 包括智能任务分发、工具调用和子Agent创建等高级功能
 */
public class OpenAIModelUsageExample {
    
    public static void main(String[] args) {
        System.out.println("=== OpenAI风格大模型集成示例 ===\n");
        
        // 创建AI基础设施应用实例
        AIInfrastructureApplication application = new AIInfrastructureApplication();
        application.start();
        
        // 设置OpenAI模型API密钥
        // 注意：在实际使用中，请从安全的配置源获取API密钥
        String apiKey = System.getenv("API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // 尝试从命令行参数获取
            if (args.length > 0) {
                apiKey = args[0];
            }
        }
        
        if (apiKey != null && !apiKey.isEmpty()) {
            application.setOpenAIModelApiKey(apiKey);
            System.out.println("API密钥已设置，将使用OpenAI风格大模型处理任务");
            System.out.println("默认模型: Qwen3-235B-A22B-Instruct\n");
        } else {
            System.out.println("未设置API密钥，将使用默认工具引擎处理任务\n");
        }
        
        // 示例1: 智能任务分发 - 让模型决定如何处理任务
//        demonstrateIntelligentTaskDispatch(application);
        
        // 示例2: 工具调用 - 模型决定调用特定工具
        demonstrateToolCalling(application);
        
        // 示例3: 子Agent创建 - 复杂任务的专门处理
//        demonstrateSubAgentCreation(application);
        
        // 示例4: 多轮对话和持续执行
//        demonstrateContinuousExecution(application);
        
        // 示例5: 交互式模式
//        if (apiKey != null && !apiKey.isEmpty()) {
//            demonstrateInteractiveMode(application);
//        }
        
        System.out.println("=== 示例完成 ===");
    }
    
    /**
     * 演示智能任务分发功能
     */
    private static void demonstrateIntelligentTaskDispatch(AIInfrastructureApplication application) {
        System.out.println("1. 智能任务分发演示:");
        System.out.println("   让模型决定如何处理不同类型的任务\n");
        
        // 简单知识问答
        CompletableFuture<String> task1 = application.executeTask(
            "什么是量子计算？请用简单易懂的语言解释。"
        );
        System.out.println("   任务1 - 知识问答: " + task1.join());
        
        // 数学计算
        CompletableFuture<String> task2 = application.executeTask(
            "计算圆周率π的前10位小数"
        );
        System.out.println("   任务2 - 数学计算: " + task2.join());
        
        System.out.println();
    }
    
    /**
     * 演示工具调用功能
     */
    private static void demonstrateToolCalling(AIInfrastructureApplication application) {
        System.out.println("2. 工具调用演示:");
        System.out.println("   模型决定调用特定工具来完成任务\n");
        
        // 本地搜索任务
        CompletableFuture<String> task1 = application.executeTask(
            "搜索关于人工智能伦理的最新研究进展"
        );
        System.out.println("   任务1 - 本地搜索任务: " + task1.join());
//
//        // 网页搜索任务（使用百度搜索）
//        CompletableFuture<String> task2 = application.executeTask(
//            "web_search 2025年最新的人工智能技术发展趋势"
//        );
//        System.out.println("   任务2 - 网页搜索任务: " + task2.join());
//
//        // 计算任务
//        CompletableFuture<String> task3 = application.executeTask(
//            "计算2的10次方等于多少"
//        );
//        System.out.println("   任务3 - 计算任务: " + task3.join());
        
        System.out.println();
    }
    
    /**
     * 演示子Agent创建功能
     */
    private static void demonstrateSubAgentCreation(AIInfrastructureApplication application) {
        System.out.println("3. 子Agent创建演示:");
        System.out.println("   复杂任务由专门的子Agent处理\n");
        
        // 复杂项目规划任务
        CompletableFuture<String> task1 = application.executeTask(
            "设计一个完整的网站开发项目计划，包括需求分析、技术选型、开发阶段、测试策略和部署方案"
        );
        System.out.println("   任务1 - 项目规划: " + task1.join());
        
        // 复杂分析任务
        CompletableFuture<String> task2 = application.executeTask(
            "分析全球气候变化对农业的影响，并提出应对策略"
        );
        System.out.println("   任务2 - 复杂分析: " + task2.join());
        
        System.out.println();
    }
    
    /**
     * 演示持续执行和多轮对话功能
     */
    private static void demonstrateContinuousExecution(AIInfrastructureApplication application) {
        System.out.println("4. 持续执行演示:");
        System.out.println("   多步骤任务的连续执行\n");
        
        // 多步骤任务
        CompletableFuture<String> task1 = application.executeTask(
            "首先介绍机器学习的基本概念，然后解释监督学习和无监督学习的区别，最后给出实际应用示例"
        );
        System.out.println("   任务1 - 多步骤解释: " + task1.join());
        
        System.out.println();
    }
    
    /**
     * 演示交互式模式
     */
    private static void demonstrateInteractiveMode(AIInfrastructureApplication application) {
        System.out.println("5. 交互式模式演示:");
        System.out.println("   支持多轮对话的交互式AI助手");
        System.out.println("   输入 'quit' 退出交互模式\n");
        
        Scanner scanner = new Scanner(System.in);
        String input;
        
        // 简单的交互式对话
        System.out.println("AI助手: 您好！我是您的AI助手，可以帮您解答问题、执行任务或进行讨论。");
        System.out.println("AI助手: 请告诉我您需要什么帮助？\n");
        
        for (int i = 0; i < 3; i++) { // 限制3轮对话以避免过长
            System.out.print("您: ");
            input = scanner.nextLine();
            
            if ("quit".equalsIgnoreCase(input.trim())) {
                break;
            }
            
            if (!input.trim().isEmpty()) {
                try {
                    CompletableFuture<String> result = application.executeTask(input);
                    String response = result.join();
                    System.out.println("AI助手: " + response + "\n");
                } catch (Exception e) {
                    System.out.println("AI助手: 抱歉，处理您的请求时出现了错误。\n");
                }
            }
        }
        
        System.out.println("交互式模式演示结束\n");
    }
}