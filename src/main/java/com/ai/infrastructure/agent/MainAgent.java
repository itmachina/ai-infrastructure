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
        // 优先使用OpenAI大模型处理任务，如果配置了API密钥
        if (openAIModelClient != null) {
            // 让模型决定如何处理任务
            return handleTaskWithModel(task);
        } else {
            // 回退到原有的简单策略
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
    }
    
    /**
     * 使用模型决定如何处理任务
     * @param task 任务描述
     * @return 处理结果
     */
    private String handleTaskWithModel(String task) {
        // 构造提示词，让模型决定如何处理任务
        String systemMessage = "你是一个智能任务分发系统，需要分析用户任务并决定最佳处理方式。\n\n" +
                "处理选项说明:\n" +
                "1. DIRECT: 直接回答 - 适用于:\n" +
                "   - 简单问题回答\n" +
                "   - 知识性问题\n" +
                "   - 解释说明类任务\n" +
                "   - 数学计算等可以直接解决的任务\n\n" +
                "2. TOOL: 使用工具 - 适用于:\n" +
                "   - 需要读取文件内容\n" +
                "   - 需要写入或修改文件\n" +
                "   - 需要执行搜索操作\n" +
                "   - 需要执行计算任务\n" +
                "   - 需要运行命令行工具\n\n" +
                "3. SUBAGENT: 创建子Agent - 适用于:\n" +
                "   - 复杂的多步骤任务\n" +
                "   - 需要长时间执行的任务\n" +
                "   - 需要专门技能处理的任务\n" +
                "   - 项目规划或设计类任务\n\n" +
                "回复格式要求:\n" +
                "必须严格按照以下格式之一回复:\n" +
                "DIRECT: <你的直接回答>\n" +
                "TOOL: <工具名称> <工具参数>\n" +
                "SUBAGENT: <子Agent处理说明>\n\n" +
                "示例:\n" +
                "DIRECT: 这是一个简单的数学问题，答案是42。\n" +
                "TOOL: search 搜索人工智能的最新发展\n" +
                "SUBAGENT: 这是一个复杂的项目规划任务，需要分解为多个步骤。";
        
        String prompt = "任务: " + task + "\n\n" +
                "请分析这个任务并决定最佳处理方式。请严格按照指定格式回复。";
        
        // 调用模型获取决策
        String decision = openAIModelClient.callModel(prompt, systemMessage);
        
        // 根据模型的决策处理任务
        return executeModelDecision(task, decision);
    }
    
    /**
     * 执行模型的决策
     * @param task 任务描述
     * @param decision 模型的决策
     * @return 处理结果
     */
    private String executeModelDecision(String task, String decision) {
        try {
            if (decision.startsWith("DIRECT:")) {
                // 直接返回模型的回答
                return decision.substring(7).trim();
            } else if (decision.startsWith("TOOL:")) {
                // 使用工具引擎执行任务
                String toolCommand = decision.substring(5).trim();
                String result = toolEngine.executeTool(toolCommand);
                return "工具执行结果: " + result;
            } else if (decision.startsWith("SUBAGENT:")) {
                // 创建子Agent处理任务
                SubAgent subAgent = new SubAgent("sub-" + System.currentTimeMillis(), "SubAgent for: " + task);
                addSubAgent(subAgent);
                String result = subAgent.executeTask(task).join();
                return "子Agent处理结果: " + result;
            } else {
                // 如果模型没有按照预期格式回复，记录日志并直接使用模型回答
                System.out.println("警告: 模型未按预期格式回复，原始回复: " + decision);
                return "模型回复: " + decision;
            }
        } catch (Exception e) {
            // 处理执行过程中可能出现的异常
            System.err.println("执行模型决策时发生错误: " + e.getMessage());
            return "执行任务时发生错误: " + e.getMessage() + "。请尝试重新提交任务或联系系统管理员。";
        }
    }
    
    /**
     * 判断是否为复杂任务（作为模型决策的辅助）
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