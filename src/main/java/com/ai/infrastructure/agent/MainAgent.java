package com.ai.infrastructure.agent;

import com.ai.infrastructure.scheduler.TaskScheduler;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.model.OpenAIModelClient;
import com.ai.infrastructure.conversation.ConversationManager;
import com.ai.infrastructure.conversation.ContinuousExecutionManager;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * 主Agent类，负责协调和调度子Agent
 * 集成OpenAI风格大模型接口作为核心AI能力
 */
public class MainAgent extends BaseAgent {
    private TaskScheduler scheduler;
    private MemoryManager memoryManager;
    private SecurityManager securityManager;
    private ToolEngine toolEngine;
    private List<SubAgent> subAgents;
    private OpenAIModelClient openAIModelClient;
    private ContinuousExecutionManager continuousExecutionManager;
    
    public MainAgent(String agentId, String name) {
        super(agentId, name);
        this.subAgents = new ArrayList<>();
        initializeComponents();
    }
    
    /**
     * 设置OpenAI模型API密钥
     * @param apiKey API密钥
     */
    public void setOpenAIModelApiKey(String apiKey) {
        this.openAIModelClient = new OpenAIModelClient(apiKey);
        // 初始化ContinuousExecutionManager
        this.continuousExecutionManager = new ContinuousExecutionManager(toolEngine, openAIModelClient);
    }
    
    /**
     * 获取OpenAI模型客户端
     * @return OpenAI模型客户端
     */
    public OpenAIModelClient getOpenAIModelClient() {
        return this.openAIModelClient;
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        this.scheduler = new TaskScheduler(10); // 最大并发数10
        this.memoryManager = new MemoryManager();
        this.securityManager = new SecurityManager();
        this.toolEngine = new ToolEngine();
        // ContinuousExecutionManager将在设置API密钥时初始化
    }
    
    // 移除了旧的模型调用方法，现在使用独立的OpenAIModelClient
    
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
        // 如果配置了模型客户端，使用持续执行管理器
        if (openAIModelClient != null && continuousExecutionManager != null) {
            return continuousExecutionManager.executeTaskContinuously(task);
        }
        
        // 否则使用原有的执行方式
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
        // 当没有配置模型客户端时，使用原有的简单策略
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
     * 判断是否为复杂任务（作为回退策略的辅助）
     * @param task 任务描述
     * @return 是否可能为复杂任务
     */
    private boolean isComplexTask(String task) {
        // 更智能的复杂任务判断逻辑
        String lowerTask = task.toLowerCase();
        
        // 包含这些关键词的任务可能更复杂
        if (lowerTask.contains("project") || lowerTask.contains("plan") || 
            lowerTask.contains("design") || lowerTask.contains("develop") ||
            lowerTask.contains("implement") || lowerTask.contains("create") ||
            lowerTask.contains("build") || lowerTask.contains("analyze") ||
            lowerTask.contains("research") || lowerTask.contains("investigate")) {
            return true;
        }
        
        // 长任务可能更复杂
        if (task.length() > 150) {
            return true;
        }
        
        // 多步骤任务可能更复杂
        if (lowerTask.contains("step") || lowerTask.contains("phase") || 
            lowerTask.contains("first") || lowerTask.contains("then") ||
            lowerTask.contains("next") || lowerTask.contains("finally")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取所有子Agent
     * @return 子Agent列表
     */
    public List<SubAgent> getSubAgents() {
        return new ArrayList<>(subAgents);
    }
}