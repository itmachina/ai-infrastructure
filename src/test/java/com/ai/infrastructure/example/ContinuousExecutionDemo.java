package com.ai.infrastructure.example;

import com.ai.infrastructure.core.AIInfrastructureApplication;

/**
 * 持续执行功能演示示例
 * 展示如何使用AI基础设施的持续执行和对话管理功能
 */
public class ContinuousExecutionDemo {
    
    public static void main(String[] args) {
        System.out.println("=== AI基础设施持续执行功能演示 ===");
        
        // 创建应用实例
        AIInfrastructureApplication application = new AIInfrastructureApplication(System.getenv("AI_API_KEY"));
        application.start();
        
        // 关闭应用
        application.shutdown();
        System.out.println("演示完成");
    }

}