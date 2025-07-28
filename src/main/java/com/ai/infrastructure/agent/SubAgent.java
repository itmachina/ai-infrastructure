package com.ai.infrastructure.agent;

import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * 子Agent类，负责执行专项任务
 * 基于Claude Code的I2A函数实现，支持完整的隔离执行环境
 */
public class SubAgent extends BaseAgent {
    private ToolEngine toolEngine;
    private MemoryManager memoryManager;
    private SecurityManager securityManager;
    private final AtomicBoolean isAborted;
    private final Set<String> allowedTools;
    private final int maxToolCalls;
    private final int maxExecutionTimeMs;
    private long startTime;
    
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
     * 执行任务 - 支持完整的Agent生命周期管理
     * @param task 任务描述
     * @return 执行结果
     */
    @Override
    public CompletableFuture<String> executeTask(String task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 记录开始时间
                startTime = System.currentTimeMillis();
                setStatus(AgentStatus.RUNNING);
                
                // 检查是否被中断
                if (isAborted.get()) {
                    setStatus(AgentStatus.ABORTED);
                    return "SubAgent aborted before execution";
                }
                
                // 执行任务前的安全检查
                if (!securityManager.validateInput(task)) {
                    setStatus(AgentStatus.ERROR);
                    return "Security validation failed for task: " + task;
                }
                
                // 执行任务
                String result = executeTaskWithMonitoring(task);
                
                // 更新内存
                memoryManager.updateContext(task, result);
                
                setStatus(AgentStatus.IDLE);
                return result;
            } catch (Exception e) {
                setStatus(AgentStatus.ERROR);
                return "Error executing task in SubAgent: " + e.getMessage();
            }
        });
    }
    
    /**
     * 执行任务并监控资源使用
     * @param task 任务描述
     * @return 执行结果
     */
    private String executeTaskWithMonitoring(String task) {
        int toolCallCount = 0;
        
        try {
            // 检查执行时间限制
            if (System.currentTimeMillis() - startTime > maxExecutionTimeMs) {
                throw new RuntimeException("SubAgent execution time limit exceeded: " + maxExecutionTimeMs + "ms");
            }
            
            // 检查工具调用次数限制
            if (toolCallCount >= maxToolCalls) {
                throw new RuntimeException("SubAgent tool call limit exceeded: " + maxToolCalls);
            }
            
            // 执行任务
            String result = toolEngine.executeTool(task);
            toolCallCount++;
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Task execution failed: " + e.getMessage(), e);
        }
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
     * 中断SubAgent执行
     */
    public void abort() {
        if (isAborted.compareAndSet(false, true)) {
            setStatus(AgentStatus.ABORTED);
            System.out.println("SubAgent " + getAgentId() + " aborted");
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
        return "Tool calls: 1"; // 简化实现
    }
}