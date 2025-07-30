package com.ai.infrastructure.agent;

import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.scheduler.IntelligentTaskDecomposer.TaskPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 专业Agent基础类
 * 基于Claude Code分层多Agent架构实现专业化Agent
 */
public abstract class SpecializedAgent extends BaseAgent {
    private static final Logger logger = LoggerFactory.getLogger(SpecializedAgent.class);
    
    protected final AgentType agentType;
    protected final MemoryManager memoryManager;
    protected final SecurityManager securityManager;
    protected final ToolEngine toolEngine;
    
    // Agent性能指标
    protected final AtomicInteger completedTasks = new AtomicInteger(0);
    protected final AtomicInteger failedTasks = new AtomicInteger(0);
    protected final AtomicInteger activeTasks = new AtomicInteger(0);
    
    // Agent负载指标
    protected volatile double currentLoad = 0.0;
    protected volatile double maxLoad = 1.0;
    protected volatile long lastActivityTime = System.currentTimeMillis();
    
    // Agent能力配置
    protected final Map<String, Object> capabilities = new ConcurrentHashMap<>();
    
    public SpecializedAgent(String agentId, String name, AgentType agentType) {
        super(agentId, name);
        this.agentType = agentType;
        this.memoryManager = new MemoryManager();
        this.securityManager = new SecurityManager();
        this.toolEngine = new ToolEngine();
        initializeCapabilities();
    }
    
    /**
     * 初始化Agent能力
     */
    protected void initializeCapabilities() {
        // 添加基础能力
        capabilities.put("maxConcurrency", 5);
        capabilities.put("maxTaskComplexity", 0.8);
        capabilities.put("timeout", 30000); // 30秒超时
        capabilities.put("retryCount", 3);
        
        // 添加特定能力
        String[] agentCapabilities = agentType.getCapabilities();
        for (String capability : agentCapabilities) {
            capabilities.put(capability, true);
        }
    }
    
    /**
     * 执行专业化任务
     */
    @Override
    public CompletableFuture<String> executeTask(String task) {
        logger.debug("{} executing task: {}", agentType.getDisplayName(), task);
        
        // 检查Agent状态
        if (getStatus() == AgentStatus.ABORTED) {
            return CompletableFuture.completedFuture("Agent aborted");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 设置状态为运行中
                setStatus(AgentStatus.RUNNING);
                activeTasks.incrementAndGet();
                lastActivityTime = System.currentTimeMillis();
                updateLoad();
                
                // 安全检查
                if (!securityManager.validateInput(task)) {
                    throw new SecurityException("Task validation failed");
                }
                
                // 内存检查
                memoryManager.checkMemoryPressure();
                
                // 执行专业化任务处理
                String result = processSpecializedTask(task);
                
                // 更新性能指标
                completedTasks.incrementAndGet();
                
                return result;
            } catch (Exception e) {
                logger.error("{} task execution failed: {}", agentType.getDisplayName(), e.getMessage(), e);
                failedTasks.incrementAndGet();
                setStatus(AgentStatus.ERROR);
                return "Task execution failed: " + e.getMessage();
            } finally {
                // 确保状态重置
                activeTasks.decrementAndGet();
                if (getStatus() != AgentStatus.ABORTED && getStatus() != AgentStatus.ERROR) {
                    setStatus(AgentStatus.IDLE);
                }
                lastActivityTime = System.currentTimeMillis();
                updateLoad();
            }
        });
    }
    
    /**
     * 处理专业化任务（子类实现）
     */
    protected abstract String processSpecializedTask(String task);
    
    /**
     * 检查是否支持指定任务类型
     */
    public abstract boolean supportsTaskType(String taskType);
    
    /**
     * 获取Agent负载分数
     */
    public double getLoadScore() {
        double activeLoad = activeTasks.get() / (double) getMaxConcurrency();
        double failureRate = getTotalTasks() > 0 ? 
            (double) failedTasks.get() / getTotalTasks() : 0.0;
        
        // 综合负载计算
        return activeLoad * 0.7 + failureRate * 0.3;
    }
    
    /**
     * 更新负载状态
     */
    protected void updateLoad() {
        this.currentLoad = getLoadScore();
    }
    
    /**
     * 检查是否可以接受新任务
     */
    public boolean canAcceptTask() {
        int maxConcurrency = getMaxConcurrency();
        int currentActiveTasks = activeTasks.get();
        
        // 如果没有活跃任务，确保状态重置为IDLE
        if (currentActiveTasks == 0 && getStatus() != AgentStatus.IDLE && getStatus() != AgentStatus.ABORTED) {
            logger.debug("Agent {} has no active tasks but status is {}, resetting to IDLE", getAgentId(), getStatus());
            setStatus(AgentStatus.IDLE);
        }
        
        // 初始化时确保状态为IDLE
        if (getStatus() == null) {
            logger.debug("Agent {} has null status, setting to IDLE", getAgentId());
            setStatus(AgentStatus.IDLE);
        }
        
        // 检查负载和并发条件
        boolean canAccept = currentActiveTasks < maxConcurrency && 
                           currentLoad < maxLoad && 
                           getStatus() == AgentStatus.IDLE;
        
        logger.debug("Agent {} canAcceptTask: activeTasks={}/{}, currentLoad={}/{}, status={}, canAccept={}", 
                   getAgentId(), currentActiveTasks, maxConcurrency, currentLoad, maxLoad, getStatus(), canAccept);
        
        return canAccept;
    }
    
    /**
     * 获取最大并发数
     */
    public int getMaxConcurrency() {
        return (int) capabilities.getOrDefault("maxConcurrency", 5);
    }
    
    /**
     * 获取总任务数
     */
    public int getTotalTasks() {
        return completedTasks.get() + failedTasks.get();
    }
    
    /**
     * 获取任务完成率
     */
    public double getCompletionRate() {
        int total = getTotalTasks();
        return total > 0 ? (double) completedTasks.get() / total : 1.0;
    }
    
    /**
     * 获取Agent状态信息
     */
    public String getAgentStatusInfo() {
        return String.format(
            "%s - 状态: %s, 负载: %.2f, 活跃任务: %d, 完成: %d, 失败: %d, 完成率: %.2f%%",
            agentType.getDisplayName(),
            getStatus(),
            currentLoad,
            activeTasks.get(),
            completedTasks.get(),
            failedTasks.get(),
            getCompletionRate() * 100
        );
    }
    
    /**
     * 获取Agent能力
     */
    public Map<String, Object> getCapabilities() {
        return new ConcurrentHashMap<>(capabilities);
    }
    
    /**
     * 重置Agent状态
     */
    public void reset() {
        completedTasks.set(0);
        failedTasks.set(0);
        activeTasks.set(0);
        currentLoad = 0.0;
        lastActivityTime = System.currentTimeMillis();
        setStatus(AgentStatus.IDLE);
    }
    
    /**
     * 获取最后活动时间
     */
    public long getLastActivityTime() {
        return lastActivityTime;
    }
    
    /**
     * 执行任务（核心方法）
     */
    public CompletableFuture<String> executeTask(String task, TaskPriority priority) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 设置状态为运行中
                setStatus(AgentStatus.RUNNING);
                activeTasks.incrementAndGet();
                lastActivityTime = System.currentTimeMillis();
                updateLoad();
                
                // 记录任务开始
                logger.debug("{} starting execution of task: {}", getAgentType().getDisplayName(), task);
                
                // 根据Agent类型执行不同的任务处理逻辑
                String result = executeTaskByAgentType(task, priority);
                
                // 更新任务统计
                completedTasks.incrementAndGet();
                
                // 记录任务完成
                logger.debug("{} completed task: {}", getAgentType().getDisplayName(), task);
                
                return result;
                
            } catch (Exception e) {
                logger.error("{} failed to execute task: {}", getAgentType().getDisplayName(), task, e);
                
                // 更新失败统计
                failedTasks.incrementAndGet();
                setStatus(AgentStatus.ERROR);
                
                throw new RuntimeException("Task execution failed", e);
            } finally {
                // 确保状态重置
                activeTasks.decrementAndGet();
                if (getStatus() != AgentStatus.ABORTED && getStatus() != AgentStatus.ERROR) {
                    setStatus(AgentStatus.IDLE);
                }
                lastActivityTime = System.currentTimeMillis();
                updateLoad();
            }
        });
    }
    
    /**
     * 根据Agent类型执行任务
     */
    private String executeTaskByAgentType(String task, TaskPriority priority) {
        switch (agentType) {
            case I2A:
                return executeI2ATask(task, priority);
            case UH1:
                return executeUH1Task(task, priority);
            case KN5:
                return executeKN5Task(task, priority);
            default:
                return "Unknown agent type: " + agentType;
        }
    }
    
    /**
     * I2A Agent任务执行逻辑
     */
    private String executeI2ATask(String task, TaskPriority priority) {
        // 模拟I2A Agent的交互界面处理
        if (task.contains("交互") || task.contains("界面") || task.contains("展示")) {
            return String.format("[I2A-交互Agent] 生成交互界面原型: %s\n" +
                    "  - 设计响应式布局\n" +
                    "  - 创建用户交互组件\n" +
                    "  - 实现数据可视化\n" +
                    "  - 生成用户测试报告", 
                    task);
        } else if (task.contains("沟通") || task.contains("汇报") || task.contains("演示")) {
            return String.format("[I2A-交互Agent] 创建沟通演示: %s\n" +
                    "  - 准备演示材料\n" +
                    "  - 设计沟通流程\n" +
                    "  - 制作汇报PPT\n" +
                    "  - 安排演示时间", 
                    task);
        } else {
            return String.format("[I2A-交互Agent] 处理交互任务: %s\n" +
                    "  - 分析交互需求\n" +
                    "  - 设计交互方案\n" +
                    "  - 实现交互功能\n" +
                    "  - 测试用户体验", 
                    task);
        }
    }
    
    /**
     * UH1 Agent任务执行逻辑
     */
    private String executeUH1Task(String task, TaskPriority priority) {
        // 模拟UH1 Agent的用户请求处理
        if (task.contains("解析") || task.contains("处理") || task.contains("转换")) {
            return String.format("[UH1-用户处理Agent] 处理用户请求: %s\n" +
                    "  - 解析请求参数\n" +
                    "  - 验证数据格式\n" +
                    "  - 转换数据结构\n" +
                    "  - 生成处理结果\n" +
                    "  - 返回响应数据", 
                    task);
        } else if (task.contains("格式化") || task.contains("验证") || task.contains("计算")) {
            return String.format("[UH1-用户处理Agent] 格式化验证任务: %s\n" +
                    "  - 输入数据验证\n" +
                    "  - 数据格式化处理\n" +
                    "  - 计算处理结果\n" +
                    "  - 生成验证报告\n" +
                    "  - 输出格式化数据", 
                    task);
        } else {
            return String.format("[UH1-用户处理Agent] 用户请求处理: %s\n" +
                    "  - 接收用户输入\n" +
                    "  - 分析请求内容\n" +
                    "  - 处理核心逻辑\n" +
                    "  - 生成响应结果\n" +
                    "  - 返回给用户", 
                    task);
        }
    }
    
    /**
     * KN5 Agent任务执行逻辑
     */
    private String executeKN5Task(String task, TaskPriority priority) {
        // 模拟KN5 Agent的知识处理
        if (task.contains("知识") || task.contains("学习") || task.contains("推理")) {
            return String.format("[KN5-知识处理Agent] 知识推理任务: %s\n" +
                    "  - 查询知识库\n" +
                    "  - 分析知识关联\n" +
                    "  - 推理逻辑关系\n" +
                    "  - 学习新知识\n" +
                    "  - 生成推理结论", 
                    task);
        } else if (task.contains("分析") || task.contains("决策") || task.contains("评估")) {
            return String.format("[KN5-知识处理Agent] 分析决策任务: %s\n" +
                    "  - 收集分析数据\n" +
                    "  - 建立决策模型\n" +
                    "  - 进行多维度分析\n" +
                    "  - 生成评估报告\n" +
                    "  - 提供决策建议", 
                    task);
        } else if (task.contains("优化") || task.contains("改进") || task.contains("建议")) {
            return String.format("[KN5-知识处理Agent] 优化改进任务: %s\n" +
                    "  - 分析现状问题\n" +
                    "  - 研究优化方案\n" +
                    "  - 评估改进效果\n" +
                    "  - 提供改进建议\n" +
                    "  - 实施优化措施", 
                    task);
        } else {
            return String.format("[KN5-知识处理Agent] 知识处理任务: %s\n" +
                    "  - 处理知识请求\n" +
                    "  - 分析问题类型\n" +
                    "  - 应用相关知识\n" +
                    "  - 生成处理结果\n" +
                    "  - 记录处理过程", 
                    task);
        }
    }
    
    /**
     * 获取Agent类型
     */
    public AgentType getAgentType() {
        return agentType;
    }
    
    /**
     * 获取活跃任务数量
     */
    public int getActiveTasks() {
        return activeTasks.get();
    }
    
    /**
     * 获取已完成任务数量
     */
    public int getCompletedTasks() {
        return completedTasks.get();
    }
    
    /**
     * 获取失败任务数量
     */
    public int getFailedTasks() {
        return failedTasks.get();
    }
}