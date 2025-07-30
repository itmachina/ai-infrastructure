package com.ai.infrastructure.scheduler;

import com.ai.infrastructure.agent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多Agent协调器
 * 基于Claude Code分层多Agent架构实现Agent间的协调和协作
 */
public class MultiAgentCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(MultiAgentCoordinator.class);
    
    // Agent池管理
    private final Map<AgentType, List<SpecializedAgent>> agentPools = new ConcurrentHashMap<>();
    private final Map<String, SpecializedAgent> activeAgents = new ConcurrentHashMap<>();
    
    // 协调策略
    private final CoordinationStrategy coordinationStrategy;
    private final IntelligentTaskDecomposer taskDecomposer;
    
    // 协调统计
    private final Map<String, CoordinationMetrics> coordinationMetrics = new ConcurrentHashMap<>();
    
    public MultiAgentCoordinator() {
        this.coordinationStrategy = CoordinationStrategy.HIERARCHICAL;
        this.taskDecomposer = new IntelligentTaskDecomposer();
        initializeAgentPools();
    }
    
    /**
     * 初始化Agent池
     */
    private void initializeAgentPools() {
        // 初始化I2A Agent池
        List<SpecializedAgent> i2aAgents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            i2aAgents.add(new InteractionAgent("i2a_" + i, "I2A交互Agent-" + i));
        }
        agentPools.put(AgentType.I2A, i2aAgents);
        
        // 初始化UH1 Agent池
        List<SpecializedAgent> uh1Agents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            uh1Agents.add(new UserProcessingAgent("uh1_" + i, "UH1用户处理Agent-" + i));
        }
        agentPools.put(AgentType.UH1, uh1Agents);
        
        // 初始化KN5 Agent池
        List<SpecializedAgent> kn5Agents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            kn5Agents.add(new KnowledgeProcessingAgent("kn5_" + i, "KN5知识处理Agent-" + i));
        }
        agentPools.put(AgentType.KN5, kn5Agents);
        
        logger.info("Agent pools initialized - I2A: {}, UH1: {}, KN5: {}", 
                   i2aAgents.size(), uh1Agents.size(), kn5Agents.size());
    }
    
    /**
     * 协调执行任务
     */
    public CompletableFuture<String> coordinateTaskExecution(String task, IntelligentTaskDecomposer.TaskPriority priority) {
        logger.info("Coordinating task execution: {}", task);
        
        String coordinationId = "coord_" + System.currentTimeMillis();
        CoordinationMetrics metrics = new CoordinationMetrics(coordinationId, task);
        coordinationMetrics.put(coordinationId, metrics);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                metrics.startTime = System.currentTimeMillis();
                
                // 1. 任务分解
                IntelligentTaskDecomposer.TaskDecompositionRequest request = 
                    new IntelligentTaskDecomposer.TaskDecompositionRequest(
                        coordinationId,
                        task,
                        priority,
                        Optional.empty()
                    );
                
                IntelligentTaskDecomposer.TaskDecompositionResult decomposition = 
                    taskDecomposer.decomposeTask(request);
                
                metrics.decompositionTime = System.currentTimeMillis() - metrics.startTime;
                metrics.stepCount = decomposition.getSteps().size();
                
                // 2. Agent分配
                Map<IntelligentTaskDecomposer.TaskStep, SpecializedAgent> agentAssignments = 
                    assignAgents(decomposition);
                
                // 3. 任务执行
                String result = executeTaskWithCoordination(decomposition, agentAssignments, metrics);
                
                // 4. 结果整合
                String finalResult = integrateResults(decomposition, result);
                
                metrics.endTime = System.currentTimeMillis();
                metrics.totalTime = metrics.endTime - metrics.startTime;
                metrics.success = true;
                
                logger.info("Task coordination completed: {}, time: {}ms", coordinationId, metrics.totalTime);
                return finalResult;
                
            } catch (Exception e) {
                logger.error("Task coordination failed: {}", coordinationId, e);
                metrics.endTime = System.currentTimeMillis();
                metrics.totalTime = metrics.endTime - metrics.startTime;
                metrics.success = false;
                metrics.error = e.getMessage();
                
                return "Task coordination failed: " + e.getMessage();
            }
        });
    }
    
    /**
     * 分配Agent
     */
    private Map<IntelligentTaskDecomposer.TaskStep, SpecializedAgent> assignAgents(
            IntelligentTaskDecomposer.TaskDecompositionResult decomposition) {
        
        Map<IntelligentTaskDecomposer.TaskStep, SpecializedAgent> assignments = new HashMap<>();
        
        for (IntelligentTaskDecomposer.TaskStep step : decomposition.getSteps()) {
            AgentType agentType = convertToAgentType(step.getAgentType());
            SpecializedAgent agent = findOptimalAgent(agentType, step.getDescription());
            if (agent != null) {
                assignments.put(step, agent);
                logger.debug("Assigned {} to step: {}", agent.getAgentType().getDisplayName(), step.getStepId());
            }
        }
        
        return assignments;
    }
    
    /**
     * 转换Agent类型 (现在类型统一，无需转换)
     */
    private AgentType convertToAgentType(AgentType agentType) {
        return agentType;
    }
    
    /**
     * 查找最优Agent
     */
    private SpecializedAgent findOptimalAgent(AgentType agentType, String taskDescription) {
        List<SpecializedAgent> agents = agentPools.get(agentType);
        if (agents == null || agents.isEmpty()) {
            logger.warn("No agents available for type: {}", agentType);
            return null;
        }
        
        // 基于负载和能力选择最优Agent
        return agents.stream()
            .filter(SpecializedAgent::canAcceptTask)
            .min(Comparator.comparingDouble(SpecializedAgent::getLoadScore))
            .orElse(agents.stream()
                .min(Comparator.comparingDouble(SpecializedAgent::getLoadScore))
                .orElse(null));
    }
    
    /**
     * 协调执行任务
     */
    private String executeTaskWithCoordination(
            IntelligentTaskDecomposer.TaskDecompositionResult decomposition,
            Map<IntelligentTaskDecomposer.TaskStep, SpecializedAgent> agentAssignments,
            CoordinationMetrics metrics) {
        
        List<String> stepResults = new ArrayList<>();
        Map<String, String> completedSteps = new HashMap<>();
        
        for (IntelligentTaskDecomposer.TaskStep step : decomposition.getSteps()) {
            try {
                logger.debug("Executing step: {}", step.getStepId());
                
                // 检查依赖
                if (!checkDependencies(step, completedSteps)) {
                    stepResults.add("Step " + step.getStepId() + ": Dependencies not met");
                    continue;
                }
                
                // 获取分配的Agent
                SpecializedAgent agent = agentAssignments.get(step);
                if (agent == null) {
                    stepResults.add("Step " + step.getStepId() + ": No agent assigned");
                    continue;
                }
                
                // 执行步骤
                long stepStartTime = System.currentTimeMillis();
                CompletableFuture<String> stepFuture = agent.executeTask(step.getDescription());
                String stepResult = stepFuture.get(step.getEstimatedDuration(), java.util.concurrent.TimeUnit.MILLISECONDS);
                long stepDuration = System.currentTimeMillis() - stepStartTime;
                
                // 记录步骤结果
                completedSteps.put(step.getStepId(), stepResult);
                stepResults.add(stepResult);
                
                // 更新指标
                metrics.stepExecutionTimes.put(step.getStepId(), stepDuration);
                
                logger.debug("Step {} completed in {}ms", step.getStepId(), stepDuration);
                
            } catch (Exception e) {
                logger.error("Step execution failed: {}", step.getStepId(), e);
                stepResults.add("Step " + step.getStepId() + " failed: " + e.getMessage());
                metrics.failedSteps++;
            }
        }
        
        return String.join("\n", stepResults);
    }
    
    /**
     * 检查步骤依赖
     */
    private boolean checkDependencies(IntelligentTaskDecomposer.TaskStep step, Map<String, String> completedSteps) {
        for (String dependency : step.getDependencies()) {
            if (!completedSteps.containsKey(dependency)) {
                logger.debug("Dependency not met for step {}: {}", step.getStepId(), dependency);
                return false;
            }
        }
        return true;
    }
    
    /**
     * 整合结果
     */
    private String integrateResults(IntelligentTaskDecomposer.TaskDecompositionResult decomposition, String executionResult) {
        StringBuilder integratedResult = new StringBuilder();
        
        integratedResult.append("=== 多Agent协调执行结果 ===\n");
        integratedResult.append("任务ID: ").append(decomposition.getTaskId()).append("\n");
        integratedResult.append("复杂度: ").append(decomposition.getComplexity()).append("\n");
        integratedResult.append("步骤数量: ").append(decomposition.getSteps().size()).append("\n");
        integratedResult.append("估算时长: ").append(decomposition.getEstimatedDuration()).append("ms\n");
        integratedResult.append("\n");
        
        integratedResult.append("=== 执行结果 ===\n");
        integratedResult.append(executionResult).append("\n");
        
        integratedResult.append("\n=== Agent分配情况 ===\n");
        for (Map.Entry<String, AgentType> entry : decomposition.getAgentAssignments().entrySet()) {
            integratedResult.append("步骤 ")
                .append(entry.getKey())
                .append(": ")
                .append(entry.getValue().name())
                .append("\n");
        }
        
        return integratedResult.toString();
    }
    
    /**
     * 获取Agent池状态
     */
    public String getAgentPoolStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== Agent池状态 ===\n");
        
        for (Map.Entry<AgentType, List<SpecializedAgent>> entry : agentPools.entrySet()) {
            AgentType type = entry.getKey();
            List<SpecializedAgent> agents = entry.getValue();
            
            status.append(type.getDisplayName()).append(": ").append(agents.size()).append(" 个Agent\n");
            
            for (SpecializedAgent agent : agents) {
                status.append("  - ").append(agent.getAgentStatusInfo()).append("\n");
            }
            status.append("\n");
        }
        
        return status.toString();
    }
    
    /**
     * 获取协调指标
     */
    public String getCoordinationMetrics() {
        StringBuilder metrics = new StringBuilder();
        metrics.append("=== 协调指标 ===\n");
        
        for (CoordinationMetrics metric : coordinationMetrics.values()) {
            metrics.append(String.format(
                "协调ID: %s\n" +
                "任务: %s\n" +
                "总时间: %dms\n" +
                "步骤数: %d\n" +
                "成功: %s\n" +
                "失败步骤: %d\n",
                metric.coordinationId,
                metric.task,
                metric.totalTime,
                metric.stepCount,
                metric.success,
                metric.failedSteps
            ));
            
            if (!metric.stepExecutionTimes.isEmpty()) {
                metrics.append("步骤执行时间:\n");
                for (Map.Entry<String, Long> entry : metric.stepExecutionTimes.entrySet()) {
                    metrics.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("ms\n");
                }
            }
            
            if (metric.error != null) {
                metrics.append("错误: ").append(metric.error).append("\n");
            }
            
            metrics.append("\n");
        }
        
        return metrics.toString();
    }
    
    /**
     * 重置所有Agent状态
     */
    public void resetAllAgents() {
        for (List<SpecializedAgent> agents : agentPools.values()) {
            for (SpecializedAgent agent : agents) {
                agent.reset();
            }
        }
        coordinationMetrics.clear();
        logger.info("All agents reset");
    }
    
    /**
     * 获取活跃Agent数量
     */
    public int getActiveAgentCount() {
        return activeAgents.size();
    }
    
    /**
     * 获取Agent池统计
     */
    public Map<String, Object> getAgentPoolStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        for (Map.Entry<AgentType, List<SpecializedAgent>> entry : agentPools.entrySet()) {
            AgentType type = entry.getKey();
            List<SpecializedAgent> agents = entry.getValue();
            
            Map<String, Object> typeStats = new HashMap<>();
            typeStats.put("total", agents.size());
            typeStats.put("active", agents.stream().mapToInt(a -> a.getActiveTasks()).sum());
            typeStats.put("completed", agents.stream().mapToInt(a -> a.getCompletedTasks()).sum());
            typeStats.put("failed", agents.stream().mapToInt(a -> a.getFailedTasks()).sum());
            typeStats.put("averageLoad", agents.stream().mapToDouble(SpecializedAgent::getLoadScore).average().orElse(0.0));
            
            stats.put(type.name(), typeStats);
        }
        
        return stats;
    }
    
    /**
     * 协调策略枚举
     */
    private enum CoordinationStrategy {
        SEQUENTIAL,    // 顺序执行
        PARALLEL,      // 并行执行
        HIERARCHICAL,  // 层次执行
        ADAPTIVE       // 自适应执行
    }
    
    /**
     * 协调指标类
     */
    private static class CoordinationMetrics {
        String coordinationId;
        String task;
        long startTime;
        long endTime;
        long totalTime;
        long decompositionTime;
        int stepCount;
        boolean success;
        int failedSteps;
        String error;
        Map<String, Long> stepExecutionTimes = new HashMap<>();
        
        CoordinationMetrics(String coordinationId, String task) {
            this.coordinationId = coordinationId;
            this.task = task;
        }
    }
}