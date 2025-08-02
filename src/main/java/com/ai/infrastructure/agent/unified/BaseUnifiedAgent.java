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
 * 统一Agent的抽象基类
 */
public abstract class BaseUnifiedAgent {
    protected static final Logger logger = LoggerFactory.getLogger(BaseUnifiedAgent.class);

    // 基础属性
    protected final String agentId;
    protected final String name;
    protected final AgentType agentType;

    // 共享组件
    protected final MemoryManager memoryManager;
    protected final SecurityManager securityManager;
    protected final ToolEngine toolEngine;

    // 核心框架
    protected final AgentCoordinator coordinator;
    protected final AgentStateManager stateManager;

    // Agent能力和状态
    protected final Map<String, Object> capabilities;
    protected final AtomicBoolean isActive;
    protected final AtomicBoolean isAborted;
    protected final Map<String, Object> performanceMetrics;

    public BaseUnifiedAgent(String agentId, String name, AgentType agentType, UnifiedAgentContext context) {
        this.agentId = agentId;
        this.name = name;
        this.agentType = agentType;

        this.memoryManager = context.getMemoryManager();
        this.securityManager = context.getSecurityManager();
        this.toolEngine = context.getToolEngine();
        this.coordinator = context.getCoordinator();
        this.stateManager = new AgentStateManager(this);

        this.capabilities = new ConcurrentHashMap<>();
        this.isActive = new AtomicBoolean(false);
        this.isAborted = new AtomicBoolean(false);
        this.performanceMetrics = new ConcurrentHashMap<>();

        initializeCapabilities();
    }

    private void initializeCapabilities() {
        capabilities.put("maxConcurrency", 5);
        capabilities.put("maxExecutionTime", 300000);
        capabilities.put("maxRetries", 3);
        capabilities.put("timeout", 30000);
        String[] typeCapabilities = agentType.getCapabilities();
        for (String capability : typeCapabilities) {
            capabilities.put(capability, true);
        }
        performanceMetrics.put("totalTasks", 0);
        performanceMetrics.put("completedTasks", 0);
        performanceMetrics.put("failedTasks", 0);
        performanceMetrics.put("averageExecutionTime", 0L);
        performanceMetrics.put("lastActivityTime", System.currentTimeMillis());
    }

    public CompletableFuture<String> executeTask(String task) {
        if (isAborted.get()) {
            return CompletableFuture.completedFuture("Agent has been aborted.");
        }
        if (!isActive.compareAndSet(false, true)) {
            // This check is to prevent concurrent execution of tasks on the same agent instance.
            logger.warn("Agent {} is already active and cannot accept new tasks now.", agentId);
            return CompletableFuture.completedFuture("Agent is busy.");
        }

        stateManager.setStatus(AgentStatus.RUNNING);
        performanceMetrics.put("lastActivityTime", System.currentTimeMillis());
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 安全检查
                if (!securityManager.validateInput(task)) {
                    throw new SecurityException("Task input validation failed.");
                }
                // 内存压力检查
                memoryManager.checkMemoryPressure();
                
                // 调用子类实现的具体任务处理逻辑
                return processTask(task);
            } catch (Exception e) {
                logger.error("Error processing task in Agent {}: {}", agentId, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }).whenComplete((result, throwable) -> {
            long duration = System.currentTimeMillis() - startTime;
            handleTaskCompletion(result, throwable, duration);
            isActive.set(false);
        });
    }
    
    /**
     * 抽象的任务处理方法，由子类实现
     * @param task 任务描述
     * @return 任务结果
     */
    public abstract String processTask(String task);

    private void handleTaskCompletion(String result, Throwable throwable, long duration) {
        int totalTasks = (int) performanceMetrics.get("totalTasks") + 1;
        performanceMetrics.put("totalTasks", totalTasks);

        if (throwable == null) {
            int completedTasks = (int) performanceMetrics.get("completedTasks") + 1;
            performanceMetrics.put("completedTasks", completedTasks);
            stateManager.setStatus(AgentStatus.IDLE);
            memoryManager.updateContext("TASK_RESULT_" + agentId, result);
            logger.debug("Agent {} completed task successfully.", agentId);
        } else {
            int failedTasks = (int) performanceMetrics.get("failedTasks") + 1;
            performanceMetrics.put("failedTasks", failedTasks);
            stateManager.setStatus(AgentStatus.ERROR);
            logger.error("Agent {} failed task execution.", agentId, throwable);
        }
        
        // 更新平均执行时间
        long avgTime = ((long) performanceMetrics.get("averageExecutionTime") * (totalTasks - 1) + duration) / totalTasks;
        performanceMetrics.put("averageExecutionTime", avgTime);
    }

    public void abort() {
        if (isAborted.compareAndSet(false, true)) {
            stateManager.setStatus(AgentStatus.ABORTED);
            logger.info("UnifiedAgent {} aborted", agentId);
        }
    }

    public boolean canAcceptTask() {
        return !isAborted.get() && !isActive.get() && stateManager.getStatus() == AgentStatus.IDLE && getLoadScore() < 0.8;
    }

    public double getLoadScore() {
        int totalTasks = (int) performanceMetrics.getOrDefault("totalTasks", 0);
        if (totalTasks == 0) return 0.0;
        double failureRate = (double) (int) performanceMetrics.get("failedTasks") / totalTasks;
        return failureRate * 0.3 + (isActive.get() ? 0.7 : 0.0);
    }

    // Getters
    public String getAgentId() { return agentId; }
    public String getName() { return name; }
    public AgentType getAgentType() { return agentType; }
    public AgentStatus getStatus() { return stateManager.getStatus(); }
    public Map<String, Object> getCapabilities() { return new ConcurrentHashMap<>(capabilities); }
    public Map<String, Object> getPerformanceMetrics() { return new ConcurrentHashMap<>(performanceMetrics); }
    public boolean isActive() { return isActive.get(); }
    public boolean isAborted() { return isAborted.get(); }
    public MemoryManager getMemoryManager() { return memoryManager; }
    public SecurityManager getSecurityManager() { return securityManager; }
}
