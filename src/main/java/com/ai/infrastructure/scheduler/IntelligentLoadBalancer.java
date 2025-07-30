package com.ai.infrastructure.scheduler;

import com.ai.infrastructure.agent.AgentType;
import com.ai.infrastructure.agent.SpecializedAgent;
import com.ai.infrastructure.scheduler.IntelligentTaskDecomposer.TaskPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 智能负载均衡调度器
 * 基于Claude Code分层多Agent架构实现智能任务调度和负载均衡
 */
public class IntelligentLoadBalancer {
    private static final Logger logger = LoggerFactory.getLogger(IntelligentLoadBalancer.class);
    
    // 任务队列
    private final PriorityBlockingQueue<ScheduledTask> taskQueue;
    private final Map<String, ScheduledTask> activeTasks = new ConcurrentHashMap<>();
    
    // Agent池
    private final Map<AgentType, List<SpecializedAgent>> agentPools;
    private final Map<String, SpecializedAgent> busyAgents = new ConcurrentHashMap<>();
    
    // 智能Agent分配器
    private final IntelligentAgentAllocator intelligentAllocator;
    
    // 调度器
    private final ScheduledExecutorService scheduler;
    private final ExecutorService executorService;
    
    // 负载均衡策略
    private final LoadBalancingStrategy strategy;
    
    // 性能指标
    private final AtomicInteger totalScheduledTasks = new AtomicInteger(0);
    private final AtomicInteger totalCompletedTasks = new AtomicInteger(0);
    private final AtomicInteger totalFailedTasks = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    // 调度配置
    private final int maxConcurrency;
    private final long taskTimeout;
    private final int maxRetryCount;
    
    public IntelligentLoadBalancer(Map<AgentType, List<SpecializedAgent>> agentPools, 
                                 int maxConcurrency, long taskTimeout, int maxRetryCount) {
        this(agentPools, maxConcurrency, taskTimeout, maxRetryCount, null);
    }
    
    public IntelligentLoadBalancer(Map<AgentType, List<SpecializedAgent>> agentPools, 
                                 int maxConcurrency, long taskTimeout, int maxRetryCount,
                                 String apiKey) {
        this.agentPools = agentPools;
        this.maxConcurrency = maxConcurrency;
        this.taskTimeout = taskTimeout;
        this.maxRetryCount = maxRetryCount;
        this.strategy = LoadBalancingStrategy.ADAPTIVE;
        
        // 初始化智能Agent分配器
        this.intelligentAllocator = new IntelligentAgentAllocator(agentPools, busyAgents, apiKey);
        
        this.taskQueue = new PriorityBlockingQueue<>(100, this::compareTasks);
        this.scheduler = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        
        startSchedulingLoop();
        startMonitoringLoop();
        
        logger.info("Intelligent Load Balancer initialized with {} max concurrency", maxConcurrency);
    }
    
    /**
     * 调度任务
     */
    public CompletableFuture<String> scheduleTask(String task, IntelligentTaskDecomposer.TaskPriority priority) {
        logger.debug("Scheduling task with priority: {}", priority);
        
        String taskId = "task_" + System.currentTimeMillis() + "_" + task.hashCode();
        ScheduledTask scheduledTask = new ScheduledTask(taskId, task, priority);
        
        CompletableFuture<String> future = new CompletableFuture<>();
        scheduledTask.setFuture(future);
        
        taskQueue.offer(scheduledTask);
        totalScheduledTasks.incrementAndGet();
        
        logger.debug("Task scheduled: {}", taskId);
        return future;
    }
    
    /**
     * 任务比较器
     */
    private int compareTasks(ScheduledTask task1, ScheduledTask task2) {
        // 综合优先级计算
        int priority1 = calculateTaskPriority(task1);
        int priority2 = calculateTaskPriority(task2);
        
        // 优先级高的先执行
        return Integer.compare(priority2, priority1);
    }
    
    /**
     * 计算任务优先级
     */
    private int calculateTaskPriority(ScheduledTask task) {
        int basePriority = task.getPriority().getValue();
        
        // 考虑等待时间
        long waitTime = System.currentTimeMillis() - task.getCreationTime();
        int waitTimeBonus = (int) (waitTime / 1000); // 每秒增加1分
        
        // 考虑重试次数
        int retryPenalty = task.getRetryCount() * 100; // 每次重试减100分
        
        return basePriority + waitTimeBonus - retryPenalty;
    }
    
    /**
     * 开始调度循环
     */
    private void startSchedulingLoop() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                processTaskQueue();
            } catch (Exception e) {
                logger.error("Error in scheduling loop", e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS); // 每100ms检查一次
    }
    
    /**
     * 处理任务队列
     */
    private void processTaskQueue() {
        while (activeTasks.size() < maxConcurrency && !taskQueue.isEmpty()) {
            ScheduledTask task = taskQueue.poll();
            if (task != null) {
                executeTask(task);
            }
        }
    }
    
    /**
     * 执行任务
     */
    private void executeTask(ScheduledTask task) {
        logger.debug("Executing task: {}", task.getTaskId());
        
        task.setStatus(TaskStatus.EXECUTING);
        task.setStartTime(System.currentTimeMillis());
        activeTasks.put(task.getTaskId(), task);
        
        executorService.submit(() -> {
            try {
                // 分配Agent
                SpecializedAgent agent = allocateAgent(task);
                if (agent == null) {
                    handleTaskFailure(task, new Exception("No available agent"));
                    return;
                }
                
                task.setAssignedAgent(agent);
                busyAgents.put(agent.getAgentId(), agent);
                
                // 执行任务
                CompletableFuture<String> resultFuture = agent.executeTask(task.getDescription(), task.getPriority());
                String result = resultFuture.get(taskTimeout, TimeUnit.MILLISECONDS);
                
                handleTaskSuccess(task, result);
                
            } catch (Exception e) {
                handleTaskFailure(task, e);
            } finally {
                // 释放Agent
                if (task.getAssignedAgent() != null) {
                    busyAgents.remove(task.getAssignedAgent().getAgentId());
                }
                activeTasks.remove(task.getTaskId());
            }
        });
    }
    
    /**
     * 分配Agent（使用智能大模型决策）
     */
    private SpecializedAgent allocateAgent(ScheduledTask task) {
        logger.info("Using intelligent allocator for agent assignment");
        
        try {
            // 使用智能Agent分配器（基于大模型）
            CompletableFuture<SpecializedAgent> allocationFuture = 
                intelligentAllocator.allocateOptimalAgent(task.getDescription(), task.getPriority());
            
            SpecializedAgent selectedAgent = allocationFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
            
            if (selectedAgent != null) {
                logger.info("Intelligent allocator selected agent: {}", selectedAgent.getAgentId());
                return selectedAgent;
            } else {
                logger.warn("Intelligent allocator failed, using fallback allocation");
                return fallbackAllocateAgent(task);
            }
            
        } catch (Exception e) {
            logger.warn("Intelligent allocation failed, using fallback: {}", e.getMessage());
            return fallbackAllocateAgent(task);
        }
    }
    
    /**
     * 回退Agent分配方法
     */
    private SpecializedAgent fallbackAllocateAgent(ScheduledTask task) {
        // 使用原来的基于规则的分配方法作为回退
        Map<AgentType, Double> requirements = analyzeTaskRequirements(task.getDescription());
        return selectOptimalAgent(requirements);
    }
    
    /**
     * 分析任务需求
     */
    private Map<AgentType, Double> analyzeTaskRequirements(String taskDescription) {
        Map<AgentType, Double> requirements = new HashMap<>();
        
        // 简化的需求分析
        String lowerTask = taskDescription.toLowerCase();
        
        // I2A需求
        double i2aRequirement = 0.0;
        if (lowerTask.contains("交互") || lowerTask.contains("界面") || lowerTask.contains("展示")) {
            i2aRequirement += 0.8;
        }
        if (lowerTask.contains("用户") || lowerTask.contains("反馈")) {
            i2aRequirement += 0.6;
        }
        
        // UH1需求
        double uh1Requirement = 0.0;
        if (lowerTask.contains("处理") || lowerTask.contains("解析") || lowerTask.contains("计算")) {
            uh1Requirement += 0.8;
        }
        if (lowerTask.contains("验证") || lowerTask.contains("转换") || lowerTask.contains("格式化")) {
            uh1Requirement += 0.6;
        }
        
        // KN5需求
        double kn5Requirement = 0.0;
        if (lowerTask.contains("分析") || lowerTask.contains("推理") || lowerTask.contains("学习")) {
            kn5Requirement += 0.8;
        }
        if (lowerTask.contains("知识") || lowerTask.contains("决策") || lowerTask.contains("优化")) {
            kn5Requirement += 0.6;
        }
        
        requirements.put(AgentType.I2A, i2aRequirement);
        requirements.put(AgentType.UH1, uh1Requirement);
        requirements.put(AgentType.KN5, kn5Requirement);
        
        // 确保至少有一种Agent类型有正需求分数，如果没有，给UH1一个默认分数
        if (i2aRequirement == 0.0 && uh1Requirement == 0.0 && kn5Requirement == 0.0) {
            requirements.put(AgentType.UH1, 0.5); // 默认使用UH1作为通用处理器
            logger.debug("No specific agent requirements found, using UH1 as default");
        }
        
        logger.debug("Task requirements: I2A={}, UH1={}, KN5={}", i2aRequirement, uh1Requirement, kn5Requirement);
        
        return requirements;
    }
    
    /**
     * 选择最优Agent
     */
    private SpecializedAgent selectOptimalAgent(Map<AgentType, Double> requirements) {
        SpecializedAgent bestAgent = null;
        double bestScore = -1.0;
        
        logger.debug("Selecting optimal agent from {} agent types", requirements.size());
        
        for (Map.Entry<AgentType, Double> entry : requirements.entrySet()) {
            AgentType agentType = entry.getKey();
            double requirement = entry.getValue();
            
            if (requirement > 0) {
                List<SpecializedAgent> agents = agentPools.get(agentType);
                logger.debug("Agent type {} has {} agents, requirement: {}", agentType, 
                           agents != null ? agents.size() : 0, requirement);
                
                if (agents != null) {
                    for (SpecializedAgent agent : agents) {
                        boolean canAccept = agent.canAcceptTask();
                        boolean isBusy = busyAgents.containsKey(agent.getAgentId());
                        boolean available = canAccept && !isBusy;
                        
                        logger.debug("Agent {}: canAccept={}, isBusy={}, available={}", 
                                   agent.getAgentId(), canAccept, isBusy, available);
                        
                        if (available) {
                            double score = calculateAgentScore(agent, requirement);
                            logger.debug("Agent {} score: {}", agent.getAgentId(), score);
                            
                            if (score > bestScore) {
                                bestScore = score;
                                bestAgent = agent;
                                logger.debug("New best agent: {} with score {}", agent.getAgentId(), score);
                            }
                        }
                    }
                }
            }
        }
        
        if (bestAgent == null) {
            logger.warn("No available agent found for requirements: {}", requirements);
        } else {
            logger.info("Selected agent {} with score {}", bestAgent.getAgentId(), bestScore);
        }
        
        return bestAgent;
    }
    
    /**
     * 计算Agent得分
     */
    private double calculateAgentScore(SpecializedAgent agent, double requirement) {
        // 负载分数（负载越低分数越高）
        double loadScore = 1.0 - agent.getLoadScore();
        
        // 能力匹配分数
        double capabilityScore = requirement;
        
        // 性能分数（完成率越高分数越高）
        double performanceScore = agent.getCompletionRate();
        
        // 综合分数
        return loadScore * 0.4 + capabilityScore * 0.4 + performanceScore * 0.2;
    }
    
    /**
     * 处理任务成功
     */
    private void handleTaskSuccess(ScheduledTask task, String result) {
        task.setStatus(TaskStatus.COMPLETED);
        task.setEndTime(System.currentTimeMillis());
        task.setResult(result);
        
        totalCompletedTasks.incrementAndGet();
        totalProcessingTime.addAndGet(task.getEndTime() - task.getStartTime());
        
        task.getFuture().complete(result);
        
        logger.debug("Task completed: {}", task.getTaskId());
    }
    
    /**
     * 处理任务失败
     */
    private void handleTaskFailure(ScheduledTask task, Exception error) {
        task.setStatus(TaskStatus.FAILED);
        task.setEndTime(System.currentTimeMillis());
        task.setError(error.getMessage());
        
        // 检查是否需要重试
        if (task.getRetryCount() < maxRetryCount) {
            task.incrementRetryCount();
            task.setStatus(TaskStatus.QUEUED);
            taskQueue.offer(task);
            logger.debug("Task retry scheduled: {} (attempt {})", task.getTaskId(), task.getRetryCount());
        } else {
            totalFailedTasks.incrementAndGet();
            task.getFuture().completeExceptionally(error);
            logger.error("Task failed permanently: {}", task.getTaskId(), error);
        }
    }
    
    /**
     * 开始监控循环
     */
    private void startMonitoringLoop() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitorAndBalance();
            } catch (Exception e) {
                logger.error("Error in monitoring loop", e);
            }
        }, 0, 1, TimeUnit.SECONDS); // 每秒监控一次
    }
    
    /**
     * 监控和负载均衡
     */
    private void monitorAndBalance() {
        // 监控任务执行情况
        monitorTaskExecution();
        
        // 监控Agent负载
        monitorAgentLoad();
        
        // 动态调整调度策略
        adjustSchedulingStrategy();
    }
    
    /**
     * 监控任务执行
     */
    private void monitorTaskExecution() {
        long currentTime = System.currentTimeMillis();
        
        // 检查超时任务
        for (ScheduledTask task : activeTasks.values()) {
            if (currentTime - task.getStartTime() > taskTimeout) {
                logger.warn("Task timeout detected: {}", task.getTaskId());
                // 可以在这里实现超时处理逻辑
            }
        }
    }
    
    /**
     * 监控Agent负载
     */
    private void monitorAgentLoad() {
        for (List<SpecializedAgent> agents : agentPools.values()) {
            for (SpecializedAgent agent : agents) {
                double load = agent.getLoadScore();
                if (load > 0.8) {
                    logger.debug("High load detected for agent: {} - {}", agent.getAgentId(), load);
                    // 可以在这里实现负载均衡逻辑
                }
            }
        }
    }
    
    /**
     * 调整调度策略
     */
    private void adjustSchedulingStrategy() {
        // 基于系统负载动态调整策略
        double systemLoad = calculateSystemLoad();
        
        if (systemLoad > 0.8) {
            // 高负载时采用保守策略
            logger.debug("High system load detected, using conservative strategy");
        } else if (systemLoad < 0.3) {
            // 低负载时采用激进策略
            logger.debug("Low system load detected, using aggressive strategy");
        }
    }
    
    /**
     * 计算系统负载
     */
    private double calculateSystemLoad() {
        double totalLoad = 0.0;
        int totalAgents = 0;
        
        for (List<SpecializedAgent> agents : agentPools.values()) {
            totalAgents += agents.size();
            for (SpecializedAgent agent : agents) {
                totalLoad += agent.getLoadScore();
            }
        }
        
        return totalAgents > 0 ? totalLoad / totalAgents : 0.0;
    }
    
    /**
     * 获取调度器状态
     */
    public String getSchedulerStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== 智能负载均衡调度器状态 ===\n");
        status.append("活跃任务: ").append(activeTasks.size()).append("/").append(maxConcurrency).append("\n");
        status.append("队列任务: ").append(taskQueue.size()).append("\n");
        status.append("总调度任务: ").append(totalScheduledTasks.get()).append("\n");
        status.append("完成任务: ").append(totalCompletedTasks.get()).append("\n");
        status.append("失败任务: ").append(totalFailedTasks.get()).append("\n");
        
        double successRate = totalScheduledTasks.get() > 0 ? 
            (double) totalCompletedTasks.get() / totalScheduledTasks.get() : 0.0;
        status.append("成功率: ").append(String.format("%.2f%%", successRate * 100)).append("\n");
        
        long avgProcessingTime = totalCompletedTasks.get() > 0 ? 
            totalProcessingTime.get() / totalCompletedTasks.get() : 0;
        status.append("平均处理时间: ").append(avgProcessingTime).append("ms\n");
        
        status.append("系统负载: ").append(String.format("%.2f", calculateSystemLoad())).append("\n");
        status.append("繁忙Agent: ").append(busyAgents.size()).append("\n");
        
        // 添加智能分配器状态
        status.append("\n").append(intelligentAllocator.getAllocationStatistics()).append("\n");
        
        return status.toString();
    }
    
    /**
     * 获取Agent负载详情
     */
    public String getAgentLoadDetails() {
        StringBuilder details = new StringBuilder();
        details.append("=== Agent负载详情 ===\n");
        
        for (Map.Entry<AgentType, List<SpecializedAgent>> entry : agentPools.entrySet()) {
            AgentType type = entry.getKey();
            List<SpecializedAgent> agents = entry.getValue();
            
            details.append(type.getDisplayName()).append(":\n");
            for (SpecializedAgent agent : agents) {
                details.append("  ").append(agent.getAgentStatusInfo()).append("\n");
            }
            details.append("\n");
        }
        
        return details.toString();
    }
    
    /**
     * 关闭调度器
     */
    public void shutdown() {
        logger.info("Shutting down Intelligent Load Balancer");
        
        // 取消所有活跃任务
        for (ScheduledTask task : activeTasks.values()) {
            task.getFuture().cancel(true);
        }
        
        // 关闭执行器
        scheduler.shutdown();
        executorService.shutdown();
        
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Intelligent Load Balancer shutdown completed");
    }
    
    /**
     * 任务状态枚举
     */
    private enum TaskStatus {
        QUEUED, EXECUTING, COMPLETED, FAILED
    }
    
    /**
     * 负载均衡策略枚举
     */
    private enum LoadBalancingStrategy {
        ROUND_ROBIN,    // 轮询
        LEAST_LOADED,   // 最少负载
        PRIORITY_BASED, // 基于优先级
        ADAPTIVE        // 自适应
    }
    
    /**
     * 调度任务类
     */
    private static class ScheduledTask {
        private final String taskId;
        private final String description;
        private final IntelligentTaskDecomposer.TaskPriority priority;
        private final long creationTime;
        private final CompletableFuture<String> future;
        
        private TaskStatus status;
        private SpecializedAgent assignedAgent;
        private long startTime;
        private long endTime;
        private String result;
        private String error;
        private int retryCount;
        
        public ScheduledTask(String taskId, String description, IntelligentTaskDecomposer.TaskPriority priority) {
            this.taskId = taskId;
            this.description = description;
            this.priority = priority;
            this.creationTime = System.currentTimeMillis();
            this.future = new CompletableFuture<>();
            this.status = TaskStatus.QUEUED;
            this.retryCount = 0;
        }
        
        public String getTaskId() { return taskId; }
        public String getDescription() { return description; }
        public IntelligentTaskDecomposer.TaskPriority getPriority() { return priority; }
        public long getCreationTime() { return creationTime; }
        public CompletableFuture<String> getFuture() { return future; }
        public TaskStatus getStatus() { return status; }
        public SpecializedAgent getAssignedAgent() { return assignedAgent; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public String getResult() { return result; }
        public String getError() { return error; }
        public int getRetryCount() { return retryCount; }
        
        public void setStatus(TaskStatus status) { this.status = status; }
        public void setAssignedAgent(SpecializedAgent assignedAgent) { this.assignedAgent = assignedAgent; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public void setResult(String result) { this.result = result; }
        public void setError(String error) { this.error = error; }
        public void setFuture(CompletableFuture<String> future) { /* Future已设置 */ }
        
        public void incrementRetryCount() { this.retryCount++; }
    }
}