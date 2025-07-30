package com.ai.infrastructure;

import com.ai.infrastructure.core.AIInfrastructureApplication;
import com.ai.infrastructure.steering.RealtimeSteeringSystem;
import com.ai.infrastructure.steering.Command;
import com.ai.infrastructure.steering.AsyncMessageQueue;
import com.ai.infrastructure.steering.QueueMessage;
import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.tools.ToolEngine;

import java.util.concurrent.CompletableFuture;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/**
 * 核心功能演示类
 * 展示AI基础设施项目的主要功能特性
 */
public class CoreFeaturesDemo {
    
    public static void main(String[] args) {
        System.out.println("=== AI Infrastructure核心功能演示 ===\n");
        
        try {
            // 演示1: 主应用基本功能
            demonstrateMainApplication();
//
//            // 演示2: 实时Steering系统
//            demonstrateRealtimeSteering();
//
//            // 演示3: 工具引擎功能
//            demonstrateToolEngine();
//
//            // 演示4: 内存管理功能
//            demonstrateMemoryManagement();
//
//            // 演示5: 多Agent协作
//            demonstrateMultiAgentCollaboration();
            
        } catch (Exception e) {
            System.err.println("演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 演示完成 ===");
    }
    
    /**
     * 演示1: 主应用基本功能
     */
    private static void demonstrateMainApplication() {
        System.out.println("1. 主应用基本功能演示:");
        
        // 创建应用实例
        AIInfrastructureApplication application = new AIInfrastructureApplication();
        application.setOpenAIModelApiKey(System.getenv("AI_API_KEY"));
        application.start();

        try {
            // 执行简单任务
            CompletableFuture<String> task1 = application.executeTask("计算 10 + 5");
            System.out.println("  任务1结果: " + task1.join());
            
            // 执行复杂任务
            CompletableFuture<String> task2 = application.executeTask("分析一个项目计划");
            System.out.println("  任务2结果: " + task2.join());
            
        } catch (Exception e) {
            System.err.println("  执行任务时出错: " + e.getMessage());
        } finally {
            application.shutdown();
        }
        
        System.out.println();
    }
    
    /**
     * 演示2: 实时Steering系统
     */
    private static void demonstrateRealtimeSteering() {
        System.out.println("2. 实时Steering系统演示:");
        
        try (RealtimeSteeringSystem system = new RealtimeSteeringSystem()) {
            system.start();

            // 发送一些命令
            system.sendCommand(new Command("prompt", "计算 25 * 4"));
            system.sendCommand(new Command("prompt", "读取配置文件"));
            
            // 等待一段时间让系统处理
            Thread.sleep(1000);
            
            System.out.println("  实时Steering系统交互完成");
            
        } catch (Exception e) {
            System.err.println("  实时Steering系统演示出错: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 演示3: 工具引擎功能
     */
    private static void demonstrateToolEngine() {
        System.out.println("3. 工具引擎功能演示:");
        
        ToolEngine toolEngine = new ToolEngine();
        
        // 演示各种工具
        String result1 = toolEngine.executeTool("calculate 12 * 8");
        System.out.println("  计算工具结果: " + result1);
        
        String result2 = toolEngine.executeTool("search AI infrastructure");
        System.out.println("  搜索工具结果: " + result2);
        
        System.out.println();
    }
    
    /**
     * 演示4: 内存管理功能
     */
    private static void demonstrateMemoryManagement() {
        System.out.println("4. 内存管理功能演示:");
        
        MemoryManager memoryManager = new MemoryManager();
        
        // 添加一些内存项
        memoryManager.updateContext("user_query", "分析数据集");
        memoryManager.updateContext("previous_result", "已完成初步分析");
        
        // 检查内存压力
        memoryManager.checkMemoryPressure();
        
        System.out.println("  内存管理操作完成");
        System.out.println();
    }
    
    /**
     * 演示5: 多Agent协作
     */
    private static void demonstrateMultiAgentCollaboration() {
        System.out.println("5. 多Agent协作演示:");
        
        MainAgent mainAgent = new MainAgent("demo-main", "Demo Main Agent");
        mainAgent.setOpenAIModelApiKey(System.getenv("AI_API_KEY"));
        // 执行任务，这将触发子Agent的创建
        CompletableFuture<String> task = mainAgent.executeTask("开发一个复杂的功能模块");
        
        try {
            String result = task.join();
            System.out.println("  协作任务结果: " + result);
            
            // 显示子Agent信息
            List<com.ai.infrastructure.agent.SubAgent> subAgents = mainAgent.getSubAgents();
            System.out.println("  创建的子Agent数量: " + subAgents.size());
            
        } catch (Exception e) {
            System.err.println("  协作任务执行出错: " + e.getMessage());
        }
        
        System.out.println();
    }
}