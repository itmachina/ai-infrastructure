package com.ai.infrastructure.agent;

import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CancellationException;

/**
 * 子Agent类，负责执行专项任务
 * 基于Claude Code的I2A函数实现，支持完整的隔离执行环境
 */
public class SubAgent extends BaseAgent {
    private static final Logger logger = LoggerFactory.getLogger(SubAgent.class);
    
    private ToolEngine toolEngine;
    private MemoryManager memoryManager;
    private SecurityManager securityManager;
    private final AtomicBoolean isAborted;
    private final Set<String> allowedTools;
    private final int maxToolCalls;
    private final int maxExecutionTimeMs;
    private long startTime;
    private int toolCallCount;
    private String currentTask;
    
    public SubAgent(String agentId, String name) {
        super(agentId, name);
        this.toolEngine = new ToolEngine();
        this.memoryManager = new MemoryManager();
        this.securityManager = new SecurityManager();
        this.isAborted = new AtomicBoolean(false);
        this.allowedTools = new HashSet<>();
        this.maxToolCalls = 50; // 默认最大工具调用次数
        this.maxExecutionTimeMs = 300000; // 默认最大执行时间5分钟
        this.startTime = 0;
        this.toolCallCount = 0;
        
        // 初始化允许的工具列表（基于Claude Code的SubAgent工具白名单）
        initializeAllowedTools();
    }
    
    /**
     * 初始化允许的工具列表
     */
    private void initializeAllowedTools() {
        allowedTools.add("Calculate");
        allowedTools.add("Read");
        allowedTools.add("Write");
        allowedTools.add("Edit");
        allowedTools.add("Search");
        allowedTools.add("Bash");
        allowedTools.add("TodoRead");
        allowedTools.add("TodoWrite");
        // 禁止Task工具以防止递归调用
    }
    
    /**
     * 执行任务 - 增强版实现，支持智能工具调度和错误恢复
     * @param task 任务描述
     * @return 执行结果
     */
    @Override
    public CompletableFuture<String> executeTask(String task) {
        logger.debug("SubAgent {} executing task: {}", getAgentId(), task);
        
        // 检查是否已被中断
        if (isAborted.get()) {
            logger.warn("SubAgent {} aborted before execution", getAgentId());
            setStatus(AgentStatus.ABORTED);
            CompletableFuture<String> abortedResult = new CompletableFuture<>();
            abortedResult.complete("SubAgent aborted before execution");
            return abortedResult;
        }
        
        // 记录当前任务
        this.currentTask = task;
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 记录开始时间
                startTime = System.currentTimeMillis();
                setStatus(AgentStatus.RUNNING);
                logger.debug("SubAgent {} status set to RUNNING", getAgentId());
                
                // 检查是否被中断
                if (isAborted.get()) {
                    logger.warn("SubAgent {} aborted before execution", getAgentId());
                    setStatus(AgentStatus.ABORTED);
                    return "SubAgent aborted before execution";
                }
                
                // 执行任务前的安全检查
                if (!securityManager.validateInput(task)) {
                    logger.warn("Security validation failed for task in SubAgent {}: {}", getAgentId(), task);
                    setStatus(AgentStatus.ERROR);
                    return "Security validation failed for task: " + task;
                }
                
                // 执行任务
                String result = executeTaskWithMonitoring(task);
                logger.debug("SubAgent {} task executed with result: {}", getAgentId(), result);
                
                // 更新内存
                memoryManager.updateContext(task, result);
                logger.debug("SubAgent {} memory context updated", getAgentId());
                
                setStatus(AgentStatus.IDLE);
                logger.debug("SubAgent {} task execution completed successfully", getAgentId());
                return result;
            } catch (CancellationException e) {
                logger.warn("SubAgent {} task execution cancelled: {}", getAgentId(), e.getMessage());
                setStatus(AgentStatus.ABORTED);
                return "SubAgent task execution cancelled: " + e.getMessage();
            } catch (Exception e) {
                logger.error("Error executing task in SubAgent {}: {}", getAgentId(), e.getMessage(), e);
                setStatus(AgentStatus.ERROR);
                // 尝试错误恢复
                String recoveryResult = attemptErrorRecovery(task, e);
                if (recoveryResult != null) {
                    return recoveryResult;
                }
                return "Error executing task in SubAgent: " + e.getMessage();
            }
        });
    }
    
    /**
     * 执行任务并监控资源使用 - 增强版实现，支持智能工具调度
     * @param task 任务描述
     * @return 执行结果
     */
    private String executeTaskWithMonitoring(String task) {
        logger.debug("SubAgent {} executing task with monitoring: {}", getAgentId(), task);
        
        try {
            // 检查执行时间限制
            long currentTime = System.currentTimeMillis();
            long executionTime = currentTime - startTime;
            if (executionTime > maxExecutionTimeMs) {
                logger.warn("SubAgent {} execution time limit exceeded: {}ms > {}ms", 
                           getAgentId(), executionTime, maxExecutionTimeMs);
                throw new RuntimeException("SubAgent execution time limit exceeded: " + maxExecutionTimeMs + "ms");
            }
            
            // 检查工具调用次数限制
            if (toolCallCount >= maxToolCalls) {
                logger.warn("SubAgent {} tool call limit exceeded: {} >= {}", 
                           getAgentId(), toolCallCount, maxToolCalls);
                throw new RuntimeException("SubAgent tool call limit exceeded: " + maxToolCalls);
            }
            
            // 检查是否被中断
            if (isAborted.get()) {
                logger.warn("SubAgent {} aborted during execution", getAgentId());
                throw new CancellationException("SubAgent aborted during execution");
            }
            
            // 智能工具调度 - 根据任务类型选择合适的工具
            logger.debug("SubAgent {} executing tool: {}", getAgentId(), task);
            String result = executeToolWithIntelligentScheduling(task);
            toolCallCount++;
            logger.debug("SubAgent {} tool execution completed. Tool calls: {}", getAgentId(), toolCallCount);
            
            return result;
        } catch (Exception e) {
            logger.error("Task execution failed in SubAgent {}: {}", getAgentId(), e.getMessage(), e);
            throw new RuntimeException("Task execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 智能工具调度 - 根据任务类型选择合适的工具
     * @param task 任务描述
     * @return 执行结果
     */
    private String executeToolWithIntelligentScheduling(String task) {
        // 分析任务类型并选择合适的工具
        String toolName = determineToolForTask(task);
        
        // 检查工具是否被允许
        if (!isToolAllowed(toolName)) {
            logger.warn("Tool {} is not allowed for SubAgent {}", toolName, getAgentId());
            throw new SecurityException("Tool " + toolName + " is not allowed");
        }
        
        // 执行工具
        return toolEngine.executeTool(task);
    }
    
    /**
     * 根据任务类型确定合适的工具
     * @param task 任务描述
     * @return 工具名称
     */
    private String determineToolForTask(String task) {
        String lowerTask = task.toLowerCase();
        
        // 根据任务关键词匹配工具
        if (lowerTask.contains("calculate") || lowerTask.contains("计算") || 
            lowerTask.matches(".*[+\\-*/].*") || lowerTask.matches(".*[<>]=?\\s*.*")) {
            return "Calculate";
        } else if (lowerTask.contains("read") || lowerTask.contains("读取")) {
            return "Read";
        } else if (lowerTask.contains("write") || lowerTask.contains("写入")) {
            return "Write";
        } else if (lowerTask.contains("edit") || lowerTask.contains("编辑")) {
            return "Edit";
        } else if (lowerTask.contains("search") || lowerTask.contains("搜索")) {
            return "Search";
        } else if (lowerTask.contains("bash") || lowerTask.contains("shell")) {
            return "Bash";
        } else if (lowerTask.contains("todo")) {
            return "TodoWrite";
        }
        
        // 默认使用工具引擎处理
        return "ToolEngine";
    }
    
    /**
     * 检查工具是否被允许
     * @param toolName 工具名称
     * @return boolean
     */
    public boolean isToolAllowed(String toolName) {
        return allowedTools.contains(toolName);
    }
    
    /**
     * 获取允许的工具列表
     * @return Set<String>
     */
    public Set<String> getAllowedTools() {
        return new HashSet<>(allowedTools);
    }
    
    /**
     * 尝试错误恢复
     * @param task 任务描述
     * @param exception 异常
     * @return 恢复结果或null
     */
    private String attemptErrorRecovery(String task, Exception exception) {
        logger.debug("Attempting error recovery for task: {}", task);
        
        try {
            // 根据异常类型进行不同的恢复策略
            if (exception instanceof SecurityException) {
                // 安全异常，记录日志并返回错误信息
                logger.warn("Security exception during task execution: {}", exception.getMessage());
                return "Security error: " + exception.getMessage();
            } else if (exception instanceof RuntimeException) {
                // 运行时异常，尝试重新执行任务
                logger.warn("Runtime exception during task execution, attempting retry: {}", exception.getMessage());
                return retryTask(task);
            }
            
            // 默认情况下，记录错误并返回null表示无法恢复
            logger.warn("Unable to recover from exception: {}", exception.getMessage());
            return null;
        } catch (Exception recoveryException) {
            logger.error("Error during recovery attempt: {}", recoveryException.getMessage(), recoveryException);
            return null;
        }
    }
    
    /**
     * 重试任务
     * @param task 任务描述
     * @return 执行结果
     */
    private String retryTask(String task) {
        try {
            // 简单的重试机制，最多重试3次
            for (int i = 0; i < 3; i++) {
                try {
                    logger.debug("Retrying task (attempt {}): {}", i + 1, task);
                    return toolEngine.executeTool(task);
                } catch (Exception e) {
                    logger.warn("Retry attempt {} failed: {}", i + 1, e.getMessage());
                    if (i == 2) {
                        // 最后一次重试也失败了
                        throw e;
                    }
                    // 等待一段时间再重试
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            logger.error("All retry attempts failed: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * 中断SubAgent执行
     */
    public void abort() {
        if (isAborted.compareAndSet(false, true)) {
            setStatus(AgentStatus.ABORTED);
            logger.info("SubAgent {} aborted", getAgentId());
        }
    }
    
    /**
     * 检查是否被中断
     * @return boolean
     */
    public boolean isAborted() {
        return isAborted.get();
    }
    
    /**
     * 获取资源使用信息
     * @return String
     */
    public String getResourceUsage() {
        long executionTime = System.currentTimeMillis() - startTime;
        return String.format("Execution time: %dms", executionTime);
    }
    
    /**
     * 获取工具调用统计
     * @return String
     */
    public String getToolCallStats() {
        return "Tool calls: " + toolCallCount;
    }
}