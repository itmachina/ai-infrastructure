package com.ai.infrastructure;

import com.ai.infrastructure.core.AIInfrastructureApplication;

/**
 * OpenAI风格大模型使用示例
 * 展示如何在AI基础设施中集成和使用OpenAI兼容的大模型，
 * 包括智能任务分发、工具调用和子Agent创建等高级功能
 */
public class OpenAIModelUsageExample {
    
    public static void main(String[] args) {
        System.out.println("=== OpenAI风格大模型集成示例 ===\n");
        
        // 创建AI基础设施应用实例
        AIInfrastructureApplication application = new AIInfrastructureApplication(System.getenv("AI_API_KEY"));
        application.start();



        System.out.println("=== 示例完成 ===");
    }

}