package com.ai.infrastructure.agent.unified.framework;

import com.ai.infrastructure.agent.AgentStatus;
import com.ai.infrastructure.agent.unified.UnifiedAgent;
import com.ai.infrastructure.agent.unified.UnifiedAgentContext;
import com.ai.infrastructure.conversation.ConversationManager;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.model.OpenAIModelClient;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 统一的任务执行框架
 * 提供标准化的任务执行流程和错误处理机制
 */
public class TaskExecutionFramework {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionFramework.class);
    
    // 执行环境
    private final UnifiedAgent agent;
    private final UnifiedAgentContext context;
    private final ExecutorService executorService;
    
    // 执行状态
    private final AtomicBoolean isAborted;
    private final Map<String, TaskExecution> activeExecutions;
    private final Map<String, TaskResult> executionResults;
    
    // 执行配置
    private final Map<String, Object> executionConfig;
    
    public TaskExecutionFramework(UnifiedAgent agent, UnifiedAgentContext context) {
        this.agent = agent;
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(5); // 可配置的线程池
        this.isAborted = new AtomicBoolean(false);
        this.activeExecutions = new ConcurrentHashMap<>();
        this.executionResults = new ConcurrentHashMap<>();
        
        this.executionConfig = new ConcurrentHashMap<>();
        initializeExecutionConfig();
        
        logger.info("TaskExecutionFramework initialized for agent: {}", agent.getAgentId());
    }
    
    /**
     * 初始化执行配置
     */
    private void initializeExecutionConfig() {
        executionConfig.put("defaultTimeout", 30000L);
        executionConfig.put("maxRetries", 3);
        executionConfig.put("enableMonitoring", true);
        executionConfig.put("enableMetrics", true);
        executionConfig.put("enableRetry", true);
        executionConfig.put("enableFallback", true);
    }
    
    /**
     * 执行任务 - 主要入口
     */
    public CompletableFuture<String> execute(String task) {
        String executionId = generateExecutionId();
        TaskExecution execution = new TaskExecution(executionId, task);
        
        logger.debug("Starting task execution {} for agent: {}", executionId, agent.getAgentId());
        
        activeExecutions.put(executionId, execution);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeTaskWithFramework(executionId, task);
            } finally {
                activeExecutions.remove(executionId);
            }
        }, executorService).whenComplete((result, throwable) -> {
            executionResults.put(executionId, new TaskResult(result, throwable));
            logger.debug("Task execution {} completed for agent: {}", executionId, agent.getAgentId());
        });
    }
    
    /**
     * 使用框架执行任务
     */
    private String executeTaskWithFramework(String executionId, String task) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 任务预处理
            String processedTask = preprocessTask(task);
            
            // 2. 安全检查
            if (!validateSecurity(processedTask)) {
                throw new SecurityException("安全检查失败: " + processedTask);
            }
            
            // 3. 内存检查
            if (!checkMemoryPressure()) {
                throw new RuntimeException("内存压力过大，无法执行任务");
            }
            
            // 4. 任务执行
            String result = executeTaskWithRetry(processedTask);
            
            // 5. 结果后处理
            String finalResult = postprocessResult(result);
            
            // 6. 更新指标
            updateExecutionMetrics(startTime, finalResult, null);
            
            return finalResult;
            
        } catch (Exception e) {
            logger.error("Task execution failed for agent {}: {}", agent.getAgentId(), e.getMessage(), e);
            
            // 尝试降级处理
            String fallbackResult = handleFallback(task, e);
            if (fallbackResult != null) {
                return fallbackResult;
            }
            
            // 更新失败指标
            updateExecutionMetrics(startTime, null, e);
            
            throw new RuntimeException("任务执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 任务预处理
     */
    private String preprocessTask(String task) {
        logger.debug("Preprocessing task for agent: {}", agent.getAgentId());
        
        // 基础清理
        String processed = task.trim();
        
        // 添加Agent上下文信息
        if (agent.getAgentType() != null) {
            processed = String.format("[AgentType:%s] %s", agent.getAgentType(), processed);
        }
        
        return processed;
    }
    
    /**
     * 安全验证
     */
    private boolean validateSecurity(String task) {
        SecurityManager securityManager = agent.getSecurityManager();
        if (securityManager == null) {
            logger.warn("SecurityManager not available for agent: {}", agent.getAgentId());
            return true; // 降级处理
        }
        
        boolean isValid = securityManager.validateInput(task);
        if (!isValid) {
            logger.warn("Security validation failed for agent {}: {}", agent.getAgentId(), task);
        }
        
        return isValid;
    }
    
    /**
     * 检查内存压力
     */
    private boolean checkMemoryPressure() {
        MemoryManager memoryManager = agent.getMemoryManager();
        if (memoryManager == null) {
            return true; // 降级处理
        }
        
        try {
            memoryManager.checkMemoryPressure();
            return true;
        } catch (Exception e) {
            logger.warn("Memory pressure check failed for agent {}: {}", agent.getAgentId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 带重试的任务执行
     */
    private String executeTaskWithRetry(String task) {
        int maxRetries = (int) executionConfig.getOrDefault("maxRetries", 3);
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("Task execution attempt {} for agent: {}", attempt, agent.getAgentId());
                
                // 执行任务
                String result = executeTaskInternal(task);
                
                // 检查是否需要中止
                if (isAborted.get()) {
                    throw new RuntimeException("执行被中止");
                }
                
                return result;
                
            } catch (Exception e) {
                logger.warn("Task execution attempt {} failed for agent {}: {}", 
                           attempt, agent.getAgentId(), e.getMessage());
                
                if (attempt == maxRetries) {
                    throw new RuntimeException("任务执行失败，已达最大重试次数", e);
                }
                
                // 等待后重试
                try {
                    Thread.sleep(calculateRetryDelay(attempt));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
            }
        }
        
        throw new RuntimeException("任务执行失败");
    }
    
    /**
     * 内部任务执行
     */
    private String executeTaskInternal(String task) {
        ToolEngine toolEngine = agent.getToolEngine();
        if (toolEngine == null) {
            throw new RuntimeException("ToolEngine not available");
        }
        
        // 根据Agent类型选择执行策略
        return executeByAgentType(task);
    }
    
    /**
     * 根据Agent类型执行任务
     */
    private String executeByAgentType(String task) {
        switch (agent.getAgentType()) {
            case I2A:
                return executeI2ATask(task);
            case UH1:
                return executeUH1Task(task);
            case KN5:
                return executeKN5Task(task);
            case GENERAL:
            default:
                return executeGeneralTask(task);
        }
    }
    
    /**
     * 执行I2A类型任务
     */
    private String executeI2ATask(String task) {
        // 实现I2A Agent的任务逻辑
        if (task.contains("交互") || task.contains("界面") || task.contains("展示")) {
            return String.format("[I2A Agent] 交互处理完成: %s", task);
        } else {
            return String.format("[I2A Agent] 通用任务处理完成: %s", task);
        }
    }
    
    /**
     * 执行UH1类型任务
     */
    private String executeUH1Task(String task) {
        // 实现UH1 Agent的任务逻辑
        if (task.contains("处理") || task.contains("解析") || task.contains("验证")) {
            return String.format("[UH1 Agent] 用户请求处理完成: %s", task);
        } else {
            return String.format("[UH1 Agent] 通用任务处理完成: %s", task);
        }
    }
    
    /**
     * 执行KN5类型任务
     */
    private String executeKN5Task(String task) {
        // 实现KN5 Agent的任务逻辑
        if (task.contains("知识") || task.contains("分析") || task.contains("推理")) {
            return String.format("[KN5 Agent] 知识处理完成: %s", task);
        } else {
            return String.format("[KN5 Agent] 通用任务处理完成: %s", task);
        }
    }
    
    /**
     * 执行通用任务 - 基于ConversationManager的高级智能决策
     */
    private String executeGeneralTask(String task) {
        logger.info("Executing general task with ConversationManager AI for agent: {}", agent.getAgentId());
        
        try {
            // 1. 获取大模型客户端和上下文
            OpenAIModelClient modelClient = getModelClient();
            if (modelClient == null) {
                logger.warn("Model client not available, falling back to direct tool execution");
                return executeWithFallback(task);
            }
            
            // 2. 使用ConversationManager进行智能决策
            ConversationManager conversationManager = new ConversationManager();
            
            // 设置Agent上下文
            String agentContext = String.format("Agent ID: %s, Type: %s", agent.getAgentId(), agent.getAgentType());
            conversationManager.addMessageToHistory("system", agentContext);
            
            // 3. 使用标准化的系统提示
            String systemMessage = conversationManager.getSystemMessage();
            
            // 4. 构建完整消息进行智能决策
            List<Map<String, String>> messages = conversationManager.buildCompleteMessages(systemMessage, task);
            
            // 5. 使用messages API调用模型
            String aiDecision = modelClient.callModelWithMessages(messages);
            
            // 6. 使用ConversationManager处理响应
            ToolEngine toolEngine = agent.getToolEngine();
            String processedResponse = conversationManager.processModelResponse(aiDecision, toolEngine, modelClient);
            
            return processedResponse;
            
        } catch (Exception e) {
            logger.error("ConversationManager-based task execution failed for agent {}: {}", agent.getAgentId(), e.getMessage());
            // 降级到工具执行
            return executeWithFallback(task);
        }
    }
    
    /**
     * 获取大模型客户端
     */
    private OpenAIModelClient getModelClient() {
        // 尝试从Agent上下文中获取模型客户端
        try {
            return context.getModelClient();
        } catch (Exception e) {
            logger.warn("Failed to get model client from context: {}", e.getMessage());
        }
        
        // 尝试从系统配置获取
        try {
            String apiKey = System.getenv("AI_API_KEY");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                return new OpenAIModelClient(apiKey);
            }
        } catch (Exception e) {
            logger.warn("Failed to create model client from environment: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 构建智能决策提示
     */
    private String buildIntelligentDecisionPrompt(String task) {
        // 获取可用工具信息
        ToolEngine toolEngine = agent.getToolEngine();
        String availableTools = "- 读取: 读取文件内容\n- 写入: 写入文件内容\n- 搜索: 在文件中搜索内容\n- 计算: 执行数学计算\n- 网络搜索: 搜索网络信息";
        
        // 获取Agent上下文信息
        String agentContext = String.format(
            "Agent类型: %s, Agent ID: %s, 当前状态: %s",
            agent.getAgentType(),
            agent.getAgentId(),
            agent.getStatus()
        );
        
        // 构建综合提示
        return String.format("""
            你是一个智能任务执行助手，需要基于可用工具和上下文信息来决定如何执行任务。
            
            【Agent上下文】
            %s
            
            【任务描述】
            %s
            
            【可用工具】
            %s
            
            【执行指导】
            请分析这个任务，决定最佳执行策略：
            1. 如果需要文件操作、数据处理、搜索或计算，请选择合适的工具并返回JSON格式的执行指令
            2. 如果任务可以通过直接回答解决，请直接返回答案
            3. 如果任务需要工具组合，请设计合理的执行流程
            
            【返回格式要求】
            如果需要使用工具，请返回JSON：
            {
                "action": "use_tool",
                "tool_name": "工具名称",
                "parameters": {
                    "参数名": "参数值"
                },
                "explanation": "执行说明"
            }
            
            如果直接回答，请返回：
            {
                "action": "direct_response",
                "response": "回答内容",
                "explanation": "回答说明"
            }
            
            请做出明智的选择并给出详细的执行说明。
            """, agentContext, task, availableTools);
    }
    
    /**
     * 执行AI决策
     */
    private String executeAIDecision(String originalTask, String aiDecision) {
        try {
            // 解析JSON响应
            Map<String, Object> decision = parseJsonDecision(aiDecision);
            
            String action = (String) decision.getOrDefault("action", "direct_response");
            
            switch (action) {
                case "use_tool":
                    return executeWithTool(decision);
                case "direct_response":
                    return executeDirectResponse(decision);
                default:
                    logger.warn("Unknown action in AI decision: {}", action);
                    return executeWithFallback(originalTask);
            }
            
        } catch (Exception e) {
            logger.error("Failed to parse AI decision: {}", e.getMessage());
            return executeWithFallback(originalTask);
        }
    }
    
    /**
     * 解析JSON决策
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonDecision(String aiDecision) {
        // 简单的JSON解析，实际项目中建议使用更健壮的JSON库
        Map<String, Object> result = new ConcurrentHashMap<>();
        
        try {
            // 查找JSON对象
            String jsonStart = aiDecision.indexOf("{") >= 0 ? 
                aiDecision.substring(aiDecision.indexOf("{")) : aiDecision;
            
            if (jsonStart.contains("{") && jsonStart.contains("}")) {
                // 提取JSON内容
                String jsonContent = jsonStart.substring(jsonStart.indexOf("{"), 
                    jsonStart.lastIndexOf("}") + 1);
                
                // 简单解析（生产环境建议使用Gson等库）
                if (jsonContent.contains("\"action\"")) {
                    String actionPart = jsonContent.substring(
                        jsonContent.indexOf("\"action\"") + 9);
                    actionPart = actionPart.substring(actionPart.indexOf("\"") + 1);
                    actionPart = actionPart.substring(0, actionPart.indexOf("\""));
                    result.put("action", actionPart);
                }
                
                if (jsonContent.contains("\"response\"")) {
                    String responsePart = jsonContent.substring(
                        jsonContent.indexOf("\"response\"") + 11);
                    responsePart = responsePart.substring(responsePart.indexOf("\"") + 1);
                    responsePart = responsePart.substring(0, responsePart.indexOf("\""));
                    result.put("response", responsePart);
                }
                
                if (jsonContent.contains("\"explanation\"")) {
                    String explanationPart = jsonContent.substring(
                        jsonContent.indexOf("\"explanation\"") + 15);
                    explanationPart = explanationPart.substring(explanationPart.indexOf("\"") + 1);
                    explanationPart = explanationPart.substring(0, explanationPart.indexOf("\""));
                    result.put("explanation", explanationPart);
                }
            }
            
            // 如果没有解析到JSON，返回默认响应
            if (result.isEmpty()) {
                result.put("action", "direct_response");
                result.put("response", aiDecision);
                result.put("explanation", "AI直接响应");
            }
            
        } catch (Exception e) {
            logger.warn("JSON parsing failed, using direct response: {}", e.getMessage());
            result.put("action", "direct_response");
            result.put("response", aiDecision);
            result.put("explanation", "解析失败，使用AI原始响应");
        }
        
        return result;
    }
    
    /**
     * 使用工具执行决策
     */
    private String executeWithTool(Map<String, Object> decision) {
        try {
            String toolName = (String) decision.get("tool_name");
            Map<String, Object> parameters = (Map<String, Object>) decision.get("parameters");
            String explanation = (String) decision.get("explanation");
            
            logger.info("AI decided to use tool '{}' with parameters: {}", toolName, parameters);
            
            ToolEngine toolEngine = agent.getToolEngine();
            if (toolEngine == null) {
                throw new RuntimeException("ToolEngine not available");
            }
            
            // 执行工具调用
            String toolResult = toolEngine.executeToolWithParameters(toolName, parameters);
            
            return String.format("[AI决策] %s\n[工具执行结果] %s", explanation, toolResult);
            
        } catch (Exception e) {
            logger.error("Tool execution failed: {}", e.getMessage());
            return String.format("[AI决策工具执行失败] %s\n[错误信息] %s", 
                decision.get("explanation"), e.getMessage());
        }
    }
    
    /**
     * 直接响应
     */
    private String executeDirectResponse(Map<String, Object> decision) {
        String response = (String) decision.get("response");
        String explanation = (String) decision.get("explanation");
        
        logger.info("AI decided to respond directly: {}", explanation);
        
        return String.format("[AI直接响应] %s\n%s", explanation, response);
    }
    
    /**
     * 降级执行
     */
    private String executeWithFallback(String task) {
        logger.info("Falling back to direct tool execution for task: {}", task);
        
        try {
            ToolEngine toolEngine = agent.getToolEngine();
            if (toolEngine == null) {
                throw new RuntimeException("ToolEngine not available for fallback");
            }
            
            String result = toolEngine.executeTool(task);
            return String.format("[降级执行] 直接工具调用结果: %s", result);
            
        } catch (Exception e) {
            logger.error("Fallback execution also failed: {}", e.getMessage());
            return String.format("[执行失败] 无法完成任务 '%s': %s", task, e.getMessage());
        }
    }
    
    /**
     * 结果后处理
     */
    private String postprocessResult(String result) {
        logger.debug("Postprocessing result for agent: {}", agent.getAgentId());
        
        // 添加Agent标识
        String postprocessed = String.format("[%s] %s", agent.getAgentId(), result);
        
        // 更新内存
        MemoryManager memoryManager = agent.getMemoryManager();
        if (memoryManager != null) {
            memoryManager.updateContext("EXECUTION_RESULT", postprocessed);
        }
        
        return postprocessed;
    }
    
    /**
     * 处理降级
     */
    private String handleFallback(String task, Exception e) {
        if (!(boolean) executionConfig.getOrDefault("enableFallback", true)) {
            return null;
        }
        
        logger.warn("Attempting fallback for agent {}: {}", agent.getAgentId(), e.getMessage());
        
        try {
            // 简单的降级处理
            return String.format("[Fallback] 无法执行任务 '%s'，原因: %s", task, e.getMessage());
        } catch (Exception fallbackException) {
            logger.error("Fallback failed for agent {}: {}", agent.getAgentId(), fallbackException.getMessage());
            return null;
        }
    }
    
    /**
     * 更新执行指标
     */
    private void updateExecutionMetrics(long startTime, String result, Exception error) {
        if (!(boolean) executionConfig.getOrDefault("enableMetrics", true)) {
            return;
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // 这里可以扩展更详细的指标收集
        logger.debug("Execution metrics for agent {}: time={}ms, error={}", 
                   agent.getAgentId(), executionTime, error != null);
    }
    
    /**
     * 计算重试延迟
     */
    private long calculateRetryDelay(int attempt) {
        // 指数退避策略
        return (long) (1000 * Math.pow(2, attempt - 1));
    }
    
    /**
     * 生成执行ID
     */
    private String generateExecutionId() {
        return String.format("%s_%d_%s", agent.getAgentId(), System.currentTimeMillis(), 
                           Thread.currentThread().getName());
    }
    
    /**
     * 中止执行
     */
    public void abort() {
        if (isAborted.compareAndSet(false, true)) {
            logger.info("Aborting task execution for agent: {}", agent.getAgentId());
            
            // 中止所有活跃执行
            for (String executionId : activeExecutions.keySet()) {
                logger.debug("Aborting execution: {}", executionId);
                // 这里可以实现更具体的中止逻辑
            }
            
            // 关闭线程池
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 获取活跃执行信息
     */
    public Map<String, TaskExecution> getActiveExecutions() {
        return new ConcurrentHashMap<>(activeExecutions);
    }
    
    /**
     * 获取执行结果
     */
    public Map<String, TaskResult> getExecutionResults() {
        return new ConcurrentHashMap<>(executionResults);
    }
    
    /**
     * 获取执行配置
     */
    public Map<String, Object> getExecutionConfig() {
        return new ConcurrentHashMap<>(executionConfig);
    }
    
    /**
     * 更新执行配置
     */
    public void updateExecutionConfig(String key, Object value) {
        executionConfig.put(key, value);
        logger.debug("Execution config updated for agent {}: {} = {}", 
                   agent.getAgentId(), key, value);
    }
    
    /**
     * 任务执行信息
     */
    public static class TaskExecution {
        private final String executionId;
        private final String task;
        private final long startTime;
        
        public TaskExecution(String executionId, String task) {
            this.executionId = executionId;
            this.task = task;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getExecutionId() {
            return executionId;
        }
        
        public String getTask() {
            return task;
        }
        
        public long getStartTime() {
            return startTime;
        }
    }
    
    /**
     * 任务结果
     */
    public static class TaskResult {
        private final String result;
        private final Throwable error;
        private final long completionTime;
        
        public TaskResult(String result, Throwable error) {
            this.result = result;
            this.error = error;
            this.completionTime = System.currentTimeMillis();
        }
        
        public String getResult() {
            return result;
        }
        
        public Throwable getError() {
            return error;
        }
        
        public long getCompletionTime() {
            return completionTime;
        }
        
        public boolean isSuccess() {
            return error == null;
        }
    }
}