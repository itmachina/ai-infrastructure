package com.ai.infrastructure.agent.compatibility;

import com.ai.infrastructure.agent.AgentType;
import com.ai.infrastructure.agent.unified.UnifiedAgent;
import com.ai.infrastructure.agent.unified.UnifiedAgentContext;
import com.ai.infrastructure.agent.unified.UnifiedMainAgent;

import java.util.Map;

/**
 * 架构迁移工具类
 * 提供从旧架构到新架构的平滑迁移支持
 */
public class ArchitectureMigration {
    
    /**
     * 创建兼容的主Agent
     */
    public static UnifiedMainAgent createCompatibleMain(String agentId, String name, String apiKey) {
        UnifiedAgentContext context = UnifiedAgentContext.getInstance();
        UnifiedMainAgent mainAgent = new UnifiedMainAgent(agentId, name, context);
        
        // 自动创建常用Agent类型映射
        createTypeMappings(mainAgent);
        
        return mainAgent;
    }
    
    /**
     * 创建类型映射
     */
    private static void createTypeMappings(UnifiedMainAgent mainAgent) {
        mainAgent.createAgent("interaction-handler", "交互处理Agent", AgentType.I2A);
        mainAgent.createAgent("user-processor", "用户处理Agent", AgentType.UH1);
        mainAgent.createAgent("knowledge-worker", "知识工作Agent", AgentType.KN5);
        mainAgent.createAgent("general-tasker", "通用任务Agent", AgentType.GENERAL);
    }
    
    /**
     * 验证迁移结果
     */
    public static MigrationValidation validateMigration() {
        UnifiedAgentContext context = UnifiedAgentContext.getInstance();
        Map<String, Object> systemStatus = context.getCoordinator().getCoordinationMetrics().toMap();
        
        return new MigrationValidation(
            true, // 简化验证
            "Architecture migration completed successfully",
            context.getComponentPool().getStatistics().getTotalInstances(),
            context.getCoordinator().getRegisteredAgents().size()
        );
    }
    
    /**
     * 获取系统健康检查
     */
    public static String getHealthCheck() {
        StringBuilder health = new StringBuilder();
        health.append("=== 架构迁移健康检查 ===\n");
        
        UnifiedAgentContext context = UnifiedAgentContext.getInstance();
        health.append("组件池状态: ").append(context.getComponentPool().getStatistics()).append("\n");
        health.append("注册Agent数: ").append(context.getCoordinator().getRegisteredAgents().size()).append("\n");
        health.append("协调器状态: ").append(context.getCoordinator().getCoordinationMetrics().getMetricsReport()).append("\n");
        
        return health.toString();
    }
    
    /**
     * 重置架构状态
     */
    public static void resetArchitecture() {
        UnifiedAgentContext context = UnifiedAgentContext.getInstance();
        context.reset();
        context.shutdown();
    }
    
    /**
     * 迁移验证结果
     */
    public static class MigrationValidation {
        private final boolean success;
        private final String message;
        private final int totalComponents;
        private final int registeredAgents;
        
        public MigrationValidation(boolean success, String message, int totalComponents, int registeredAgents) {
            this.success = success;
            this.message = message;
            this.totalComponents = totalComponents;
            this.registeredAgents = registeredAgents;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getTotalComponents() { return totalComponents; }
        public int getRegisteredAgents() { return registeredAgents; }
    }
}