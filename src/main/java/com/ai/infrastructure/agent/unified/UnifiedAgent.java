package com.ai.infrastructure.agent.unified;

import com.ai.infrastructure.agent.AgentStatus;
import com.ai.infrastructure.agent.AgentType;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.agent.unified.framework.TaskExecutionFramework;
import com.ai.infrastructure.agent.unified.coordinator.AgentCoordinator;
import com.ai.infrastructure.agent.unified.state.AgentStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 统一的Agent实现，解决原有的设计冲突
 * 提供灵活、可扩展的Agent架构
 */
public class UnifiedAgent {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedAgent.class);
    
    // 基础属性
    private final String agentId;
    private final String name;
    private final AgentType agentType;
    
    // 共享组件（通过池化管理）
    private final MemoryManager memoryManager;
    private final SecurityManager securityManager;
    private final ToolEngine toolEngine;
    
    // 核心框架
    private final TaskExecutionFramework executionFramework;
    private final AgentCoordinator coordinator;
    private final AgentStateManager stateManager;
    
    // Agent能力配置
    private final Map<String, Object> capabilities;
    private final AtomicBoolean isActive;
    private final AtomicBoolean isAborted;
    
    // 性能指标
    private final Map<String, Object> performanceMetrics;
    
    public UnifiedAgent(String agentId, String name, AgentType agentType, 
                       UnifiedAgentContext context) {
        this.agentId = agentId;
        this.name = name;
        this.agentType = agentType;
        
        // 从上下文获取共享组件
        this.memoryManager = context.getMemoryManager();
        this.securityManager = context.getSecurityManager();
        this.toolEngine = context.getToolEngine();
        
        // 初始化核心框架
        this.executionFramework = new TaskExecutionFramework(this, context);
        this.coordinator = context.getCoordinator();
        this.stateManager = new AgentStateManager(this);
        
        // 初始化配置和状态
        this.capabilities = new ConcurrentHashMap<>();
        this.isActive = new AtomicBoolean(false);
        this.isAborted = new AtomicBoolean(false);
        this.performanceMetrics = new ConcurrentHashMap<>();
        
        initializeCapabilities();
        logger.info("UnifiedAgent initialized: {} ({})", agentId, agentType);
    }
    
    /**
     * 初始化Agent能力
     */
    private void initializeCapabilities() {
        // 基础能力
        capabilities.put("maxConcurrency", 5);
        capabilities.put("maxExecutionTime", 300000); // 5分钟
        capabilities.put("maxRetries", 3);
        capabilities.put("timeout", 30000); // 30秒
        
        // 类型特定能力
        String[] typeCapabilities = agentType.getCapabilities();
        for (String capability : typeCapabilities) {
            capabilities.put(capability, true);
        }
        
        // 初始化性能指标
        performanceMetrics.put("totalTasks", 0);
        performanceMetrics.put("completedTasks", 0);
        performanceMetrics.put("failedTasks", 0);
        performanceMetrics.put("averageExecutionTime", 0L);
        performanceMetrics.put("lastActivityTime", System.currentTimeMillis());
    }
    
    /**
     * 执行任务 - 统一入口
     */
    public CompletableFuture<String> executeTask(String task) {
        logger.debug("UnifiedAgent {} executing task: {}", agentId, task);
        
        // 检查Agent状态
        if (isAborted.get()) {
            return CompletableFuture.completedFuture("Agent已中止");
        }
        
        if (!isActive.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture("Agent正在执行其他任务");
        }
        
        // 更新状态
        stateManager.setStatus(AgentStatus.RUNNING);
        performanceMetrics.put("lastActivityTime", System.currentTimeMillis());
        
        try {
            // 使用执行框架处理任务
            return executionFramework.execute(task)
                .whenComplete((result, throwable) -> {
                    isActive.set(false);
                    handleTaskCompletion(result, throwable);
                });
        } catch (Exception e) {
            isActive.set(false);
            stateManager.setStatus(AgentStatus.ERROR);
            logger.error("Task execution failed for agent {}: {}", agentId, e.getMessage(), e);
            return CompletableFuture.completedFuture("任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理任务完成
     */
    private void handleTaskCompletion(String result, Throwable throwable) {
        try {
            int totalTasks = (int) performanceMetrics.getOrDefault("totalTasks", 0) + 1;
            performanceMetrics.put("totalTasks", totalTasks);
            
            if (throwable == null) {
                // 任务成功
                int completedTasks = (int) performanceMetrics.getOrDefault("completedTasks", 0) + 1;
                performanceMetrics.put("completedTasks", completedTasks);
                stateManager.setStatus(AgentStatus.IDLE);
                
                // 更新内存
                if (memoryManager != null) {
                    memoryManager.updateContext("TASK_RESULT_" + agentId, result);
                }
                
                logger.debug("Agent {} completed task successfully", agentId);
            } else {
                // 任务失败
                int failedTasks = (int) performanceMetrics.getOrDefault("failedTasks", 0) + 1;
                performanceMetrics.put("failedTasks", failedTasks);
                stateManager.setStatus(AgentStatus.ERROR);
                
                logger.error("Agent {} failed to execute task: {}", agentId, throwable.getMessage(), throwable);
            }
            
        } catch (Exception e) {
            logger.error("Error handling task completion for agent {}: {}", agentId, e.getMessage(), e);
            stateManager.setStatus(AgentStatus.ERROR);
        }
    }
    
    /**
     * 协作执行任务
     */
    public CompletableFuture<String> collaborateWith(String[] partnerAgentIds, String task) {
        if (coordinator == null) {
            return CompletableFuture.completedFuture("协作器未初始化");
        }
        
        return coordinator.coordinateCollaboration(agentId, partnerAgentIds, task);
    }
    
    /**
     * 获取Agent状态
     */
    public AgentStatus getStatus() {
        return stateManager.getStatus();
    }
    
    /**
     * 中止Agent
     */
    public void abort() {
        if (isAborted.compareAndSet(false, true)) {
            stateManager.setStatus(AgentStatus.ABORTED);
            executionFramework.abort();
            logger.info("UnifiedAgent {} aborted", agentId);
        }
    }
    
    /**
     * 重置Agent状态
     */
    public void reset() {
        isAborted.set(false);
        isActive.set(false);
        stateManager.setStatus(AgentStatus.IDLE);
        performanceMetrics.clear();
        initializeCapabilities();
        logger.info("UnifiedAgent {} reset", agentId);
    }
    
    /**
     * 检查是否可以接受新任务
     */
    public boolean canAcceptTask() {
        return !isAborted.get() && 
               !isActive.get() && 
               stateManager.getStatus() == AgentStatus.IDLE &&
               getLoadScore() < 0.8; // 负载低于80%
    }
    
    /**
     * 获取负载分数
     */
    public double getLoadScore() {
        int totalTasks = (int) performanceMetrics.getOrDefault("totalTasks", 0);
        int completedTasks = (int) performanceMetrics.getOrDefault("completedTasks", 0);
        int failedTasks = (int) performanceMetrics.getOrDefault("failedTasks", 0);
        
        if (totalTasks == 0) return 0.0;
        
        double failureRate = (double) failedTasks / totalTasks;
        return failureRate * 0.3 + (isActive.get() ? 0.7 : 0.0);
    }
    
    /**
     * 获取性能指标
     */
    public Map<String, Object> getPerformanceMetrics() {
        return new ConcurrentHashMap<>(performanceMetrics);
    }
    
    /**
     * 获取能力配置
     */
    public Map<String, Object> getCapabilities() {
        return new ConcurrentHashMap<>(capabilities);
    }
    
    /**
     * 更新能力配置
     */
    public void updateCapability(String key, Object value) {
        capabilities.put(key, value);
        logger.debug("Agent {} capability updated: {} = {}", agentId, key, value);
    }
    
    // === Getter方法 ===
    
    public String getAgentId() {
        return agentId;
    }
    
    public String getName() {
        return name;
    }
    
    public AgentType getAgentType() {
        return agentType;
    }
    
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }
    
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    public ToolEngine getToolEngine() {
        return toolEngine;
    }
    
    public boolean isAborted() {
        return isAborted.get();
    }
    
    public boolean isActive() {
        return isActive.get();
    }
    
    public TaskExecutionFramework getExecutionFramework() {
        return executionFramework;
    }
    
    public AgentCoordinator getCoordinator() {
        return coordinator;
    }
}