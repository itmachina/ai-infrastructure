package com.ai.infrastructure.agent.unified;

import com.ai.infrastructure.agent.AgentType;
import java.util.concurrent.CompletableFuture;

/**
 * 统一架构演示类
 * 用于验证编译和功能
 */
public class UnifiedArchitectureDemo {
    
    /**
     * 演示新架构的基本使用
     */
    public static void main(String[] args) {
        System.out.println("=== 统一Agent架构演示 ===");
        
        // 创建系统上下文
        UnifiedAgentContext context = UnifiedAgentContext.getInstance();
        
        // 创建主Agent
        UnifiedMainAgent mainAgent = new UnifiedMainAgent("demo-main", "演示主Agent", context);
        
        try {
            // 创建不同类型的Agent
            mainAgent.createAgent("i2a-demo", "交互演示Agent", AgentType.I2A);
            mainAgent.createAgent("uh1-demo", "处理演示Agent", AgentType.UH1);
            mainAgent.createAgent("kn5-demo", "知识演示Agent", AgentType.KN5);
            
            System.out.println("创建了3个演示Agent");
            
            // 执行基本任务
            CompletableFuture<String> result1Future = mainAgent.executeMainTask("分析用户需求界面");
            String result1 = result1Future.join();
            System.out.println("任务1结果: " + result1);
            
            CompletableFuture<String> result2Future = mainAgent.executeMainTask("处理用户请求数据");
            String result2 = result2Future.join();
            System.out.println("任务2结果: " + result2);
            
            // 协作任务
            String[] collaborators = {"i2a-demo", "kn5-demo"};
            String collabResult = mainAgent.executeCollaborativeTask(
                "协作完成复杂分析", 
                collaborators, 
                "adaptive"
            ).join();
            System.out.println("协作结果: " + collabResult);
            
            // 系统状态
            System.out.println("\n=== 系统状态 ===");
            System.out.println(mainAgent.getResourceUsage());
            
        } finally {
            // 清理资源
            mainAgent.shutdown();
            context.shutdown();
        }
        
        System.out.println("\n=== 演示完成 ===");
    }
}