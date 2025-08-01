package com.ai.infrastructure.agent;

import com.ai.infrastructure.config.ToolConfigManager;
import com.ai.infrastructure.conversation.ContinuousExecutionManager;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.model.OpenAIModelClient;
import com.ai.infrastructure.scheduler.IntelligentAgentAllocator;
import com.ai.infrastructure.scheduler.TaskScheduler;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.steering.UserMessage;
import com.ai.infrastructure.tools.ToolEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 主Agent类，负责协调和调度子Agent
 * 集成OpenAI风格大模型接口作为核心AI能力
 */
public class MainAgent extends BaseAgent {
    private static final Logger logger = LoggerFactory.getLogger(MainAgent.class);

    private TaskScheduler scheduler;
    private MemoryManager memoryManager;
    private SecurityManager securityManager;
    private ToolEngine toolEngine;
    private List<SubAgent> subAgents;
    private OpenAIModelClient openAIModelClient;
    private ContinuousExecutionManager continuousExecutionManager;
    private SubAgentManager subAgentManager;
    private IntelligentAgentAllocator intelligentAgentAllocator;

    // 防止死循环的支持
    private final ThreadLocal<Boolean> isProcessingRealtimeTask = ThreadLocal.withInitial(() -> false);
    private final AtomicBoolean isAborted = new AtomicBoolean(false);
    private final AtomicBoolean isExecuting = new AtomicBoolean(false);

    // 配置管理器
    private final ToolConfigManager configManager;

    public MainAgent(String agentId, String name, String apiKey) {
        super(agentId, name);
        this.subAgents = new ArrayList<>();
        this.configManager = ToolConfigManager.getInstance();
        initializeComponents(apiKey);
    }


    /**
     * 获取OpenAI模型客户端
     *
     * @return OpenAI模型客户端
     */
    public OpenAIModelClient getOpenAIModelClient() {
        return this.openAIModelClient;
    }

    /**
     * 初始化组件
     */
    private void initializeComponents(String apiKey) {
        this.scheduler = new TaskScheduler(10); // 最大并发数10
        this.memoryManager = new MemoryManager();
        this.securityManager = new SecurityManager();
        this.toolEngine = new ToolEngine();
        this.subAgentManager = new SubAgentManager(this, memoryManager, toolEngine, securityManager);

        // 创建Agent池用于智能分配
        this.openAIModelClient = new OpenAIModelClient(apiKey);
        // 初始化ContinuousExecutionManager和智能Agent分配器
        this.continuousExecutionManager = new ContinuousExecutionManager(toolEngine, openAIModelClient);
        this.intelligentAgentAllocator = new IntelligentAgentAllocator(
                createAgentPools(), new HashMap<>(), apiKey);
    }

    /**
     * 创建Agent池
     */
    private Map<AgentType, List<SpecializedAgent>> createAgentPools() {
        Map<AgentType, List<SpecializedAgent>> agentPools = new HashMap<>();

        // I2A Agent池
        List<SpecializedAgent> i2aAgents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            i2aAgents.add(new InteractionAgent("i2a_" + i, "I2A Agent-" + i));
        }
        agentPools.put(AgentType.I2A, (List<SpecializedAgent>) (List<?>) i2aAgents);

        // UH1 Agent池
        List<SpecializedAgent> uh1Agents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            uh1Agents.add(new UserProcessingAgent("uh1_" + i, "UH1 Agent-" + i));
        }
        agentPools.put(AgentType.UH1, (List<SpecializedAgent>) (List<?>) uh1Agents);

        // KN5 Agent池
        List<SpecializedAgent> kn5Agents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            kn5Agents.add(new KnowledgeProcessingAgent("kn5_" + i, "KN5 Agent-" + i));
        }
        agentPools.put(AgentType.KN5, (List<SpecializedAgent>) (List<?>) kn5Agents);

        return agentPools;
    }

    // 移除了旧的模型调用方法，现在使用独立的OpenAIModelClient

    /**
     * 添加子Agent
     *
     * @param subAgent 子Agent
     */
    public void addSubAgent(SubAgent subAgent) {
        this.subAgents.add(subAgent);
    }

    /**
     * 执行任务 - 增强版实现，集成所有组件的OpenAI模式
     *
     * @param task 任务描述
     * @return 执行结果
     */
    @Override
    public CompletableFuture<String> executeTask(String task) {
        logger.debug("Executing task with full component integration: {}", task);

        // 检查是否已被中断
        if (isAborted.get()) {
            logger.warn("MainAgent aborted before execution");
            setStatus(AgentStatus.ABORTED);
            CompletableFuture<String> abortedResult = new CompletableFuture<>();
            abortedResult.complete("MainAgent aborted before execution");
            return abortedResult;
        }

        // 设置执行状态
        if (!isExecuting.compareAndSet(false, true)) {
            logger.warn("MainAgent is already executing a task");
            CompletableFuture<String> busyResult = new CompletableFuture<>();
            busyResult.complete("MainAgent is already executing a task");
            return busyResult;
        }

        // 内存管理：更新输入上下文
        if (memoryManager != null) {
            memoryManager.updateContext("INPUT", task);
        }

        try {
            // 强制要求配置OpenAI模型客户端
            if (openAIModelClient == null || continuousExecutionManager == null) {
                logger.error("OpenAI API key is required for task execution. Please set AI_API_KEY environment variable.");
                setStatus(AgentStatus.ERROR);
                CompletableFuture<String> errorResult = new CompletableFuture<>();
                errorResult.complete("Error: OpenAI API key is required. Please set AI_API_KEY environment variable.");
                return errorResult;
            }

            logger.debug("Using enhanced OpenAI mode with full component integration");

            // 使用实时steering系统进行预处理
            if (isRealtimeOrComplexTask(task)) {
                return processWithRealtimeSteeringEnhanced(task);
            }

            // 使用智能Agent分配器处理复杂任务
            if (requiresIntelligentAllocation(task)) {
                return processWithIntelligentAgentAllocation(task);
            }

            // 标准任务使用增强的连续执行管理器
            return continuousExecutionManager.executeTaskContinuously(task)
                    .whenComplete((result, throwable) -> {
                        isExecuting.set(false);
                        if (throwable != null) {
                            logger.error("Error in continuous execution: {}", throwable.getMessage(), throwable);
                            setStatus(AgentStatus.ERROR);
                            if (memoryManager != null) {
                                memoryManager.updateContext("ERROR", throwable.getMessage());
                            }
                        } else {
                            logger.debug("Continuous execution completed successfully");
                            setStatus(AgentStatus.IDLE);
                            if (memoryManager != null) {
                                memoryManager.updateContext("OUTPUT", result);
                            }
                        }
                    });
        } catch (Exception e) {
            isExecuting.set(false);
            logger.error("Error in task execution setup: {}", e.getMessage());
            setStatus(AgentStatus.ERROR);
            if (memoryManager != null) {
                memoryManager.updateContext("ERROR", e.getMessage());
            }
            CompletableFuture<String> errorResult = new CompletableFuture<>();
            errorResult.complete("Error in task execution setup: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 判断是否为实时或复杂任务
     */
    private boolean isRealtimeOrComplexTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("实时") || lowerTask.contains("streaming") ||
                lowerTask.contains("实时处理") || lowerTask.contains("流式") ||
                lowerTask.contains("复杂") || lowerTask.contains("complex") ||
                lowerTask.contains("多步骤") || lowerTask.contains("multi-step");
    }

    /**
     * 判断是否需要智能Agent分配
     */
    private boolean requiresIntelligentAllocation(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("分析") || lowerTask.contains("analysis") ||
                lowerTask.contains("设计") || lowerTask.contains("design") ||
                lowerTask.contains("开发") || lowerTask.contains("develop") ||
                lowerTask.contains("项目") || lowerTask.contains("project") ||
                lowerTask.contains("规划") || lowerTask.contains("planning");
    }

    /**
     * 使用增强的实时Steering系统处理任务 - 防止死循环
     */
    private CompletableFuture<String> processWithRealtimeSteeringEnhanced(String task) {
        // 检查是否已经在处理实时任务（防止循环）
        if (isProcessingRealtimeTask.get()) {
            logger.warn("Detected potential infinite loop in realtime steering processing");
            CompletableFuture<String> errorResult = new CompletableFuture<>();
            errorResult.complete("Error: Infinite loop detected in realtime processing");
            return errorResult;
        }

        try {
            isProcessingRealtimeTask.set(true);
            logger.debug("Processing task with enhanced RealtimeSteeringSystem: {}", task);

            // 创建用户消息
            UserMessage userMessage = new UserMessage("user", task);

            // 安全检查
            if (securityManager != null && !securityManager.validateInput(task)) {
                CompletableFuture<String> errorResult = new CompletableFuture<>();
                errorResult.complete("Error: Task validation failed");
                return errorResult;
            }

            // 内存管理检查
            if (memoryManager != null) {
                memoryManager.checkMemoryPressure();
            }


            // 重要：不调用processInput，因为这会触发MainAgentLoop中的循环调用
            // 而是直接处理，避免死循环
            String promptContent = extractPromptContentFromUserMessage(userMessage);

            // 直接使用工具引擎进行处理，避免通过RealtimeSteeringSystem内部的MainAgent
            String toolResult = toolEngine.executeTool(task);

            // 更新内存
            if (memoryManager != null) {
                memoryManager.updateContext("OUTPUT", toolResult);
            }

            logger.debug("Enhanced realtime steering processing completed successfully");
            CompletableFuture<String> result = new CompletableFuture<>();
            result.complete("[Enhanced RealtimeSteeringSystem] " + toolResult);
            return result;

        } catch (Exception e) {
            logger.error("Error in enhanced realtime steering processing: {}", e.getMessage());
            CompletableFuture<String> errorResult = new CompletableFuture<>();
            errorResult.complete("Enhanced realtime processing failed: " + e.getMessage());
            return errorResult;
        } finally {
            isProcessingRealtimeTask.set(false);
        }
    }

    /**
     * 从用户消息中提取提示内容 - 避免循环调用
     */
    private String extractPromptContentFromUserMessage(UserMessage message) {
        Object content = message.getMessage().get("content");

        if (content == null) {
            return "";
        }

        // 处理不同类型的content
        if (content instanceof String) {
            // 字符串内容
            return ((String) content).replace("\"", "");
        } else if (content instanceof Map) {
            // 对象内容，如 { "text": "message", "format": "markdown" }
            Map<?, ?> contentMap = (Map<?, ?>) content;
            if (contentMap.containsKey("text")) {
                return contentMap.get("text").toString();
            } else if (contentMap.containsKey("content")) {
                return contentMap.get("content").toString();
            } else {
                // 返回整个对象的字符串表示
                return content.toString();
            }
        } else {
            // 其他类型，直接转换为字符串
            return content.toString().replace("\"", "");
        }
    }

    /**
     * 使用智能Agent分配器处理任务
     */
    private CompletableFuture<String> processWithIntelligentAgentAllocation(String task) {
        try {
            logger.debug("Processing task with IntelligentAgentAllocator: {}", task);

            // 安全检查
            if (securityManager != null && !securityManager.validateInput(task)) {
                CompletableFuture<String> errorResult = new CompletableFuture<>();
                errorResult.complete("Error: Task validation failed");
                return errorResult;
            }

            // 内存管理检查
            if (memoryManager != null) {
                memoryManager.checkMemoryPressure();
            }

            // 使用智能Agent分配器选择最优Agent
            CompletableFuture<SpecializedAgent> allocationFuture = intelligentAgentAllocator.allocateOptimalAgent(
                    task, com.ai.infrastructure.scheduler.IntelligentTaskDecomposer.TaskPriority.HIGH);

            SpecializedAgent selectedAgent = allocationFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);

            if (selectedAgent != null) {
                logger.info("IntelligentAgentAllocator selected agent: {}", selectedAgent.getAgentId());

                // 执行任务
                CompletableFuture<String> taskResult = selectedAgent.executeTask(task);
                String result = taskResult.join();

                // 更新内存
                if (memoryManager != null) {
                    memoryManager.updateContext("OUTPUT", result);
                }

                logger.debug("Intelligent agent allocation completed successfully");
                CompletableFuture<String> finalResult = new CompletableFuture<>();
                finalResult.complete("[IntelligentAgentAllocation] Agent " + selectedAgent.getAgentId() + ": " + result);
                return finalResult;
            } else {
                CompletableFuture<String> fallbackResult = new CompletableFuture<>();
                fallbackResult.complete("[IntelligentAgentAllocation] No suitable agent found, using tool engine");
                return fallbackResult;
            }

        } catch (Exception e) {
            logger.error("Error in intelligent agent allocation: {}", e.getMessage());
            CompletableFuture<String> errorResult = new CompletableFuture<>();
            errorResult.complete("Intelligent agent allocation failed: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 判断是否为复杂任务（作为回退策略的辅助）
     *
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
        if (task.length() > configManager.getMainAgentMaxTaskLength()) {
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
     *
     * @return 子Agent列表
     */
    public List<SubAgent> getSubAgents() {
        return new ArrayList<>(subAgents);
    }

    /**
     * 中断MainAgent执行
     */
    public void abort() {
        if (isAborted.compareAndSet(false, true)) {
            setStatus(AgentStatus.ABORTED);
            logger.info("MainAgent {} aborted", getAgentId());

            // 中断所有子Agent
            for (SubAgent subAgent : subAgents) {
                subAgent.abort();
            }

            // 如果有持续执行管理器，也中断它
            if (continuousExecutionManager != null) {
                continuousExecutionManager.cancelExecution();
            }
        }
    }

    /**
     * 检查是否被中断
     *
     * @return boolean
     */
    public boolean isAborted() {
        return isAborted.get();
    }

    /**
     * 获取子Agent数量
     *
     * @return 子Agent数量
     */
    public int getSubAgentCount() {
        return subAgents.size();
    }

    /**
     * 协调和调度子Agent执行任务
     *
     * @param task 主任务
     * @return 协作执行结果
     */
    public CompletableFuture<String> coordinateSubAgentTask(String task) {
        logger.info("MainAgent coordinating sub-agent task: {}", task);

        try {
            // 任务分解
            List<String> subTasks = decomposeTask(task);

            if (subTasks.size() == 1) {
                // 简单任务，直接创建单个子Agent
                return createAndExecuteSingleSubAgent(task);
            } else {
                // 复杂任务，使用协作模式
                return createAndExecuteCollaborativeTask(subTasks);
            }
        } catch (Exception e) {
            logger.error("Error in sub-agent coordination: {}", e.getMessage(), e);
            CompletableFuture<String> errorResult = new CompletableFuture<>();
            errorResult.complete("Sub-agent coordination failed: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 任务分解
     */
    private List<String> decomposeTask(String task) {
        List<String> subTasks = new ArrayList<>();

        // 简单的任务分解逻辑
        String lowerTask = task.toLowerCase();

        if (lowerTask.contains("analyze") && lowerTask.contains("implement")) {
            subTasks.add("Analyze requirements: " + task);
            subTasks.add("Implement solution: " + task);
        } else if (lowerTask.contains("read") && lowerTask.contains("write")) {
            subTasks.add("Read input: " + task);
            subTasks.add("Process data: " + task);
            subTasks.add("Write output: " + task);
        } else {
            subTasks.add(task); // 无法分解，作为单个任务
        }

        return subTasks;
    }

    /**
     * 创建并执行单个子Agent
     */
    private CompletableFuture<String> createAndExecuteSingleSubAgent(String task) {
        SubAgent subAgent = subAgentManager.createSubAgent(task, AgentType.GENERAL, getAgentId());
        subAgents.add(subAgent);

        return subAgent.executeTask(task)
                .thenApply(result -> {
                    subAgentManager.cleanupCompletedAgents();
                    return "[SubAgent " + subAgent.getAgentId() + "]: " + result;
                });
    }

    /**
     * 创建并执行协作任务
     */
    private CompletableFuture<String> createAndExecuteCollaborativeTask(List<String> subTasks) {
        // 确定协作策略
        CoordinationStrategy strategy = determineCoordinationStrategy(subTasks);

        // 创建协作Agent组
        List<SubAgent> agentGroup = subAgentManager.createCollaborativeAgentGroup(subTasks, CollaborationType.PARALLEL);
        subAgents.addAll(agentGroup);

        // 执行协作任务
        return subAgentManager.executeCollaborativeTask(agentGroup, strategy)
                .thenApply(result -> {
                    // 清理完成的Agent
                    subAgentManager.cleanupCompletedAgents();

                    // 更新内存
                    memoryManager.updateContext("COLLABORATIVE_RESULT", result);

                    return "[Collaborative Execution]\n" + result;
                });
    }

    /**
     * 确定协调策略
     */
    private CoordinationStrategy determineCoordinationStrategy(List<String> subTasks) {
        if (subTasks.size() <= 2) {
            return CoordinationStrategy.PARALLEL;
        } else if (subTasks.stream().anyMatch(t -> t.contains("sequential"))) {
            return CoordinationStrategy.SEQUENTIAL;
        } else if (subTasks.stream().anyMatch(t -> t.contains("pipeline"))) {
            return CoordinationStrategy.PIPELINE;
        } else {
            return CoordinationStrategy.ADAPTIVE;
        }
    }

    /**
     * 获取子Agent管理器
     */
    public SubAgentManager getSubAgentManager() {
        return subAgentManager;
    }

    public MainAgent getMainAgent() {
        return this;
    }

    /**
     * 获取所有活跃子Agent的状态
     */
    public Map<String, Object> getSubAgentStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeAgents", subAgentManager.getActiveAgentIds());
        status.put("agentStatuses", subAgentManager.getAgentStatuses());
        status.put("subAgentStats", subAgentManager.getStats());
        status.put("sharedData", subAgentManager.getAllSharedData());
        return status;
    }

    /**
     * 广播消息给所有子Agent
     */
    public void broadcastToSubAgents(String message) {
        subAgentManager.broadcastMessage(getAgentId(), message);
    }

    /**
     * 获取资源使用信息
     *
     * @return String
     */
    public String getResourceUsage() {
        StringBuilder usage = new StringBuilder();
        usage.append("=== MainAgent Resource Usage ===\n");
        usage.append("MainAgent: ").append(getName()).append("\n");
        usage.append("Status: ").append(getStatus()).append("\n");

        SubAgentStats stats = subAgentManager.getStats();
        usage.append("Active SubAgents: ").append(stats.getActiveAgents()).append("\n");
        usage.append("Completed SubAgents: ").append(stats.getCompletedAgents()).append("\n");
        usage.append("Total SubAgents Created: ").append(stats.getTotalAgentsCreated()).append("\n");
        usage.append("Shared Data Items: ").append(stats.getSharedDataItems()).append("\n");

        // 组件状态信息
        usage.append("\n=== Component Status ===\n");
        usage.append("Task Scheduler: ").append(scheduler != null ? "Active" : "Inactive").append("\n");
        usage.append("Memory Manager: ").append(memoryManager != null ? "Active" : "Inactive").append("\n");
        usage.append("Security Manager: ").append(securityManager != null ? "Active" : "Inactive").append("\n");
        usage.append("Tool Engine: ").append(toolEngine != null ? "Active" : "Inactive").append("\n");
        usage.append("OpenAI Model: ").append(openAIModelClient != null ? "Configured" : "Not Configured").append("\n");
        usage.append("Continuous Execution: ").append(continuousExecutionManager != null ? "Active" : "Inactive").append("\n");
        usage.append("Intelligent Allocator: ").append(intelligentAgentAllocator != null ? "Active" : "Inactive").append("\n");
        usage.append("SubAgent Manager: ").append(subAgentManager != null ? "Active" : "Inactive").append("\n");

        // 添加内存管理信息
        if (memoryManager != null) {
            usage.append("\n=== Memory Info ===\n");
            usage.append(memoryManager.getMemoryInfo()).append("\n");
        }

        usage.append("\n=== System Architecture ===\n");
        usage.append("Architecture: RealtimeSteering + MainAgentLoop + SubAgent Collaboration\n");
        usage.append("Communication: AsyncMessageQueue + StreamingProcessor\n");
        usage.append("Coordination: MainAgent -> SubAgentManager -> SubAgents\n");
        usage.append("Data Exchange: Shared Data Space + Message Passing\n");

        return usage.toString();
    }
}