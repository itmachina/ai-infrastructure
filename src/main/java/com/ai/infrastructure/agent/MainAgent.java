package com.ai.infrastructure.agent;

import com.ai.infrastructure.scheduler.TaskScheduler;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

/**
 * 主Agent类，负责协调和调度子Agent
 */
public class MainAgent extends BaseAgent {
    private TaskScheduler scheduler;
    private MemoryManager memoryManager;
    private SecurityManager securityManager;
    private ToolEngine toolEngine;
    private List<SubAgent> subAgents;
    
    public MainAgent(String agentId, String name) {
        super(agentId, name);
        this.subAgents = new ArrayList<>();
        initializeComponents();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        this.scheduler = new TaskScheduler(10); // 最大并发数10
        this.memoryManager = new MemoryManager();
        this.securityManager = new SecurityManager();
        this.toolEngine = new ToolEngine();
    }
    
    /**
     * 添加子Agent
     * @param subAgent 子Agent
     */
    public void addSubAgent(SubAgent subAgent) {
        this.subAgents.add(subAgent);
    }
    
    /**
     * 执行任务
     * @param task 任务描述
     * @return 执行结果
     */
    @Override
    public CompletableFuture<String> executeTask(String task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                setStatus(AgentStatus.RUNNING);
                
                // 安全检查
                if (!securityManager.validateInput(task)) {
                    throw new SecurityException("Task validation failed");
                }
                
                // 内存管理检查
                memoryManager.checkMemoryPressure();
                
                // 调度任务执行
                String result = scheduler.scheduleTask(task, this::processTask);
                
                // 更新内存
                memoryManager.updateContext(task, result);
                
                setStatus(AgentStatus.IDLE);
                return result;
            } catch (Exception e) {
                setStatus(AgentStatus.ERROR);
                return "Error executing task: " + e.getMessage();
            }
        });
    }
    
    /**
     * 处理任务的核心逻辑
     * @param task 任务描述
     * @return 处理结果
     */
    private String processTask(String task) {
        // 检查是否需要创建子Agent来处理复杂任务
        if (isComplexTask(task)) {
            SubAgent subAgent = new SubAgent("sub-" + System.currentTimeMillis(), "SubAgent for: " + task);
            addSubAgent(subAgent);
            return subAgent.executeTask(task).join();
        } else {
            // 使用工具引擎处理简单任务
            return toolEngine.executeTool(task);
        }
    }
    
    /**
     * 判断是否为复杂任务
     * @param task 任务描述
     * @return 是否为复杂任务
     */
    private boolean isComplexTask(String task) {
        // 简单的复杂任务判断逻辑
        return task.contains("complex") || task.contains("analyze") || task.length() > 100;
    }
    
    /**
     * 获取所有子Agent
     * @return 子Agent列表
     */
    public List<SubAgent> getSubAgents() {
        return new ArrayList<>(subAgents);
    }
}