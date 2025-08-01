package com.ai.infrastructure.agent.unified;

import com.ai.infrastructure.agent.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 统一MainAgent实现
 * 整合原有MainAgent、SubAgentManager、IntelligentAgentAllocator等功能
 * 基于新的统一Agent架构
 */
public class UnifiedMainAgent {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedMainAgent.class);
    
    private final String agentId;
    private final String name;
    private final UnifiedAgentContext context;
    private final Map<String, UnifiedAgent> managedAgents;
    
    // 系统性能监控
    private long startTime;
    private int totalProcessedTasks;
    private int successfulTasks;
    private int failedTasks;
    
    public UnifiedMainAgent(String agentId, String name, UnifiedAgentContext context) {
        this.agentId = agentId;
        this.name = name;
        this.context = context;
        this.managedAgents = new ConcurrentHashMap<>();
        
        this.startTime = System.currentTimeMillis();
        this.totalProcessedTasks = 0;
        this.successfulTasks = 0;
        this.failedTasks = 0;
        
        initializeDefaultAgents();
        logger.info("UnifiedMainAgent initialized: {} with {} agents", agentId, managedAgents.size());
    }
    
    /**
     * 初始化默认Agent
     */
    private void initializeDefaultAgents() {
        // 创建系统默认的Agent
        createDefaultAgent("i2a_agent", "I2A交互Agent", AgentType.I2A);
        createDefaultAgent("uh1_agent", "UH1用户处理Agent", AgentType.UH1);
        createDefaultAgent("kn5_agent", "KN5知识处理Agent", AgentType.KN5);
        createDefaultAgent("general_agent", "通用Agent", AgentType.GENERAL);
    }
    
    /**
     * 创建系统默认Agent
     */
    private void createDefaultAgent(String id, String name, AgentType type) {
        UnifiedAgent agent = new UnifiedAgent(id, name, type, context);
        managedAgents.put(id, agent);
        context.getCoordinator().registerAgent(agent);
        logger.debug("Created default agent: {} ({})", id, type);
    }
    
    /**
     * 创建新的专用Agent
     */
    public UnifiedAgent createAgent(String agentId, String name, AgentType agentType) {
        if (managedAgents.containsKey(agentId)) {
            logger.warn("Agent already exists: {}", agentId);
            return managedAgents.get(agentId);
        }
        
        UnifiedAgent agent = new UnifiedAgent(agentId, name, agentType, context);
        managedAgents.put(agentId, agent);
        context.getCoordinator().registerAgent(agent);
        
        logger.info("Created new agent: {} ({}) with type: {}", agentId, name, agentType);
        return agent;
    }
    
    /**
     * 移除Agent
     */
    public boolean removeAgent(String agentId) {
        UnifiedAgent removed = managedAgents.remove(agentId);
        if (removed != null) {
            context.getCoordinator().unregisterAgent(agentId);
            logger.info("Removed agent: {}", agentId);
            return true;
        }
        logger.warn("Agent not found for removal: {}", agentId);
        return false;
    }
    
    /**
     * 执行主要任务
     */
    public CompletableFuture<String> executeMainTask(String task) {
        logger.info("UnifiedMainAgent executing task: {}", task);
        
        totalProcessedTasks++;
        
        try {
            // 智能任务分配
            String result = executeTaskWithIntelligentAssignment(task);
            successfulTasks++;
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            failedTasks++;
            logger.error("Task execution failed", e);
            return CompletableFuture.completedFuture("任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 智能任务分配执行
     */
    private String executeTaskWithIntelligentAssignment(String task) {
        // 步骤1: 分析任务类型和复杂度
        TaskAnalysis analysis = analyzeTask(task);
        
        // 步骤2: 智能选择最佳Agent
        UnifiedAgent selectedAgent = selectOptimalAgent(analysis, task);
        
        if (selectedAgent == null) {
            // 回退选择: 使用通用Agent
            selectedAgent = managedAgents.get("general_agent");
        }
        
        if (selectedAgent == null || !selectedAgent.canAcceptTask()) {
            return createNewAgentForTask(analysis, task);
        }
        
        // 步骤3: 执行任务
        return selectedAgent.executeTask(task).join();
    }
    
    /**
     * 分析任务
     */
    private TaskAnalysis analyzeTask(String task) {
        TaskAnalysis analysis = new TaskAnalysis();
        String lowerTask = task.toLowerCase();
        
        // 任务复杂度评估
        analysis.complexity = Math.min(calculateTaskComplexity(task), 10);
        
        // 任务类型识别
        if (lowerTask.contains("交互") || lowerTask.contains("界面") || lowerTask.contains("用户")) {
            analysis.preferredAgentType = AgentType.I2A;
        } else if (lowerTask.contains("处理") || lowerTask.contains("解析") || lowerTask.contains("验证")) {
            analysis.preferredAgentType = AgentType.UH1;
        } else if (lowerTask.contains("知识") || lowerTask.contains("搜索") || lowerTask.contains("分析")) {
            analysis.preferredAgentType = AgentType.KN5;
        } else {
            analysis.preferredAgentType = AgentType.GENERAL;
        }
        
        // 协作需求评估
        analysis.requiresCollaboration = task.contains("协作") || task.contains("协作") || analysis.complexity > 6;
        
        return analysis;
    }
    
    /**
     * 计算任务复杂度
     */
    private int calculateTaskComplexity(String task) {
        String lowerTask = task.toLowerCase();
        int complexity = 0;
        
        if (lowerTask.contains("分析")) complexity += 2;
        if (lowerTask.contains("设计")) complexity += 2;
        if (lowerTask.contains("开发")) complexity += 3;
        if (lowerTask.contains("测试")) complexity += 1;
        if (lowerTask.contains("部署")) complexity += 1;
        if (lowerTask.contains("监控")) complexity += 1;
        if (lowerTask.contains("优化")) complexity += 2;
        if (lowerTask.contains("协作")) complexity += 2;
        if (lowerTask.contains("复杂")) complexity += 2;
        if (lowerTask.contains("简单")) complexity -= 1;
        
        return Math.max(1, complexity);
    }
    
    /**
     * 选择最佳Agent
     */
    private UnifiedAgent selectOptimalAgent(TaskAnalysis analysis, String task) {
        try {
            List<UnifiedAgent> suitableAgents = managedAgents.values().stream()
                .filter(agent -> agent.getAgentType() == analysis.preferredAgentType)
                .filter(UnifiedAgent::canAcceptTask)
                .collect(Collectors.toList());
            
            if (suitableAgents.isEmpty()) {
                suitableAgents = managedAgents.values().stream()
                    .filter(UnifiedAgent::canAcceptTask)
                    .collect(Collectors.toList());
            }
            
            // 选择负载最低的Agent
            return suitableAgents.stream()
                .min((a1, a2) -> Double.compare(a1.getLoadScore(), a2.getLoadScore()))
                .orElse(null);
                
        } catch (Exception e) {
            logger.error("Error selecting optimal agent", e);
            return null;
        }
    }
    
    /**
     * 为新任务创建专门Agent
     */
    private String createNewAgentForTask(TaskAnalysis analysis, String task) {
        String newAgentId = "agent_" + System.currentTimeMillis();
        UnifiedAgent newAgent = createAgent(newAgentId, "自动创建Agent", analysis.preferredAgentType);
        
        return newAgent.executeTask(task).join();
    }
    
    /**
     * 执行协作任务
     */
    public CompletableFuture<String> executeCollaborativeTask(String task, String[] participantAgentIds, String strategyType) {
        if (participantAgentIds == null || participantAgentIds.length == 0) {
            return executeMainTask(task);
        }
        
        // 验证参与者
        String[] validAgentIds = Arrays.stream(participantAgentIds)
            .filter(managedAgents::containsKey)
            .toArray(String[]::new);
        
        if (validAgentIds.length == 0) {
            return CompletableFuture.completedFuture("没有有效的参与Agent");
        }
        
        // 使用协调器执行协作任务
        String organizer = validAgentIds[0];
        String[] collaborators = validAgentIds.length > 1 ? 
            Arrays.copyOfRange(validAgentIds, 1, validAgentIds.length) :
            new String[0];
        
        return context.getCoordinator().coordinateCollaboration(organizer, collaborators, task);
    }
    
    /**
     * 获取所有Agent状态
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        
        status.put("mainAgentId", agentId);
        status.put("mainAgentName", name);
        status.put("totalAgents", managedAgents.size());
        status.put("activeAgents", getActiveAgentsCount());
        status.put("idleAgents", getIdleAgentsCount());
        status.put("errorAgents", getErrorAgentsCount());
        status.put("totalProcessedTasks", totalProcessedTasks);
        status.put("successfulTasks", successfulTasks);
        status.put("failedTasks", failedTasks);
        status.put("successRate", calculateSuccessRate());
        status.put("systemUptime", System.currentTimeMillis() - startTime);
        
        status.put("agentDetails", getAgentDetails());
        status.put("coordinationMetrics", context.getCoordinator().getCoordinationMetrics());
        
        return status;
    }
    
    /**
     * 获取Agent详细信息
     */
    private Map<String, Object> getAgentDetails() {
        Map<String, Object> details = new ConcurrentHashMap<>();
        
        managedAgents.forEach((id, agent) -> {
            Map<String, Object> agentInfo = new ConcurrentHashMap<>();
            agentInfo.put("name", agent.getName());
            agentInfo.put("type", agent.getAgentType());
            agentInfo.put("status", agent.getStatus());
            agentInfo.put("loadScore", agent.getLoadScore());
            agentInfo.put("active", agent.isActive());
            agentInfo.put("aborted", agent.isAborted());
            agentInfo.put("performance", agent.getPerformanceMetrics());
            agentInfo.put("capabilities", agent.getCapabilities());
            
            details.put(id, agentInfo);
        });
        
        return details;
    }
    
    /**
     * 获取资源使用统计
     */
    public String getResourceUsage() {
        StringBuilder usage = new StringBuilder();
        
        usage.append("=== 统一MainAgent资源使用统计 ===\n");
        usage.append("主Agent: ").append(name).append(" (").append(agentId).append(")\n");
        usage.append("运行时间: ").append((System.currentTimeMillis() - startTime) / 1000).append("秒\n");
        usage.append("总处理任务: ").append(totalProcessedTasks).append("\n");
        usage.append("成功任务: ").append(successfulTasks).append("\n");
        usage.append("失败任务: ").append(failedTasks).append("\n");
        usage.append("成功率: ").append(String.format("%.2f", calculateSuccessRate())).append("%\n");
        usage.append("管理Agent数量: ").append(managedAgents.size()).append("\n");
        usage.append("活跃Agent: ").append(getActiveAgentsCount()).append("\n");
        usage.append("空闲Agent: ").append(getIdleAgentsCount()).append("\n");
        usage.append("错误Agent: ").append(getErrorAgentsCount()).append("\n");
        
        usage.append("\n=== Agent详细信息 ===\n");
        managedAgents.forEach((id, agent) -> {
            usage.append(id).append(" (").append(agent.getAgentType()).append(")")
                 .append(" - 状态: ").append(agent.getStatus())
                 .append(" - 负载: ").append(String.format("%.2f", agent.getLoadScore()))
                 .append("\n");
        });
        
        usage.append("\n=== 协调器状态 ===\n");
        usage.append("注册Agent数: ").append(context.getCoordinator().getStatsCount()).append("\n");
        usage.append("协作请求数: ").append(context.getCoordinator().getCoordinationMetrics().getCollaborationRequestCount()).append("\n");
        usage.append("协作成功率: ").append(context.getCoordinator().getCoordinationMetrics().getSuccessRate()).append("%\n");
        
        return usage.toString();
    }
    
    // === 统计方法 ===
    
    private int getActiveAgentsCount() {
        return (int) managedAgents.values().stream()
            .filter(agent -> agent.isActive() || agent.getStatus().name().equals("RUNNING"))
            .count();
    }
    
    private int getIdleAgentsCount() {
        return (int) managedAgents.values().stream()
            .filter(agent -> agent.getStatus().name().equals("IDLE"))
            .count();
    }
    
    private int getErrorAgentsCount() {
        return (int) managedAgents.values().stream()
            .filter(agent -> agent.getStatus().name().equals("ERROR"))
            .count();
    }
    
    private double calculateSuccessRate() {
        return totalProcessedTasks > 0 ? (double) successfulTasks / totalProcessedTasks * 100 : 0.0;
    }
    
    /**
     * 关闭系统
     */
    public void shutdown() {
        logger.info("Shutting down UnifiedMainAgent: {}", agentId);
        
        // 停止所有Agent
        managedAgents.values().forEach(agent -> {
            try {
                agent.abort();
            } catch (Exception e) {
                logger.error("Error stopping agent: {}", agent.getAgentId(), e);
            }
        });
        
        // 关闭协调器
        context.getCoordinator().shutdown();
        
        managedAgents.clear();
        logger.info("UnifiedMainAgent shutdown completed: {}", agentId);
    }
    
    // === 内部类 ===
    
    static class TaskAnalysis {
        int complexity;
        AgentType preferredAgentType;
        boolean requiresCollaboration;
        
        TaskAnalysis() {
            this.complexity = 1;
            this.preferredAgentType = AgentType.GENERAL;
            this.requiresCollaboration = false;
        }
    }
}