package com.ai.infrastructure.agent;

import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * SubAgent管理器，负责子Agent的创建、协作和数据交换
 * 支持子Agent之间的实时通信和数据共享
 */
public class SubAgentManager {
    private static final Logger logger = LoggerFactory.getLogger(SubAgentManager.class);

    private final Map<String, SubAgent> activeSubAgents;
    private final Map<String, SubAgent> completedSubAgents;
    private final Map<String, Object> sharedDataSpace;
    private final MemoryManager memoryManager;
    private final ToolEngine toolEngine;
    private final SecurityManager securityManager;
    private final AtomicLong agentIdCounter;
    private final MainAgent mainAgent;

    public SubAgentManager(MainAgent mainAgent, MemoryManager memoryManager, ToolEngine toolEngine, SecurityManager securityManager) {
        this.mainAgent = mainAgent;
        this.memoryManager = memoryManager;
        this.toolEngine = toolEngine;
        this.securityManager = securityManager;
        this.activeSubAgents = new ConcurrentHashMap<>();
        this.completedSubAgents = new ConcurrentHashMap<>();
        this.sharedDataSpace = new ConcurrentHashMap<>();
        this.agentIdCounter = new AtomicLong(0);
    }

    /**
     * 创建新的子Agent
     *
     * @param taskDescription 任务描述
     * @param agentType       代理类型
     * @param parentAgentId   父Agent ID
     * @return 创建的子Agent
     */
    public SubAgent createSubAgent(String taskDescription, AgentType agentType, String parentAgentId) {
        String agentId = generateAgentId(agentType);

        SubAgent subAgent = new SubAgent(agentId, taskDescription, parentAgentId);
        subAgent.setAgentType(agentType);

        activeSubAgents.put(agentId, subAgent);
        logger.info("Created new SubAgent: {} for task: {}", agentId, taskDescription);

        return subAgent;
    }

    /**
     * 创建协作子Agent组
     *
     * @param tasks             任务列表
     * @param collaborationType 协作类型
     * @return 子Agent组
     */
    public List<SubAgent> createCollaborativeAgentGroup(List<String> tasks, CollaborationType collaborationType) {
        List<SubAgent> agentGroup = new ArrayList<>();
        String groupId = "group_" + System.currentTimeMillis();

        for (int i = 0; i < tasks.size(); i++) {
            String task = tasks.get(i);
            AgentType agentType = determineAgentTypeForTask(task);

            SubAgent subAgent = createSubAgent(task, agentType, groupId);
            subAgent.setCollaborationType(collaborationType);
            subAgent.setGroupId(groupId);

            agentGroup.add(subAgent);
        }

        // 建立协作关系
        establishCollaborationLinks(agentGroup, collaborationType);

        logger.info("Created collaborative agent group: {} with {} agents", groupId, agentGroup.size());
        return agentGroup;
    }

    /**
     * 执行子Agent协作任务
     *
     * @param agents               子Agent列表
     * @param coordinationStrategy 协调策略
     * @return 协作结果
     */
    public CompletableFuture<String> executeCollaborativeTask(List<SubAgent> agents, CoordinationStrategy coordinationStrategy) {
        logger.info("Starting collaborative execution for {} agents with strategy: {}",
                agents.size(), coordinationStrategy);

        switch (coordinationStrategy) {
            case PARALLEL:
                return executeParallel(agents);
            case SEQUENTIAL:
                return executeSequential(agents);
            case PIPELINE:
                return executePipeline(agents);
            case REDUNDANT:
                return executeRedundant(agents);
            case ADAPTIVE:
                return executeAdaptive(agents);
            default:
                return executeParallel(agents);
        }
    }

    /**
     * 并行执行所有子Agent
     */
    private CompletableFuture<String> executeParallel(List<SubAgent> agents) {
        List<CompletableFuture<String>> futures = agents.stream()
                .map(agent -> agent.executeTask(agent.getTaskDescription()))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    StringBuilder result = new StringBuilder();
                    result.append("=== Parallel Execution Results ===\n");

                    for (int i = 0; i < agents.size(); i++) {
                        try {
                            String agentResult = futures.get(i).get();
                            result.append(String.format("Agent %s: %s\n",
                                    agents.get(i).getAgentId(), agentResult));
                        } catch (Exception e) {
                            result.append(String.format("Agent %s: ERROR - %s\n",
                                    agents.get(i).getAgentId(), e.getMessage()));
                        }
                    }

                    return result.toString();
                });
    }

    /**
     * 顺序执行子Agent
     */
    private CompletableFuture<String> executeSequential(List<SubAgent> agents) {
        CompletableFuture<String> chain = CompletableFuture.completedFuture("");

        for (SubAgent agent : agents) {
            chain = chain.thenCompose(previousResult -> {
                // 将前一个结果传递给下一个Agent
                String enrichedTask = agent.getTaskDescription() + " [Previous: " + previousResult + "]";
                return agent.executeTask(enrichedTask);
            });
        }

        return chain.thenApply(finalResult -> {
            StringBuilder result = new StringBuilder();
            result.append("=== Sequential Execution Results ===\n");
            result.append("Final Result: ").append(finalResult);
            return result.toString();
        });
    }

    /**
     * 流水线执行子Agent
     */
    private CompletableFuture<String> executePipeline(List<SubAgent> agents) {
        // 创建数据流
        Map<String, Object> pipelineData = new HashMap<>();

        CompletableFuture<String> chain = CompletableFuture.completedFuture("");

        for (int i = 0; i < agents.size(); i++) {
            final int index = i;
            chain = chain.thenCompose(previousOutput -> {
                SubAgent agent = agents.get(index);

                String enrichedTask = agent.getTaskDescription();
                if (!previousOutput.isEmpty()) {
                    enrichedTask = enrichedTask + " [Pipeline input: " + previousOutput + "]";
                }

                return agent.executeTask(enrichedTask);
            });
        }

        return chain.thenApply(finalOutput -> {
            StringBuilder result = new StringBuilder();
            result.append("=== Pipeline Execution Results ===\n");
            result.append("Final Pipeline Output: ").append(finalOutput);
            result.append("\nPipeline Data: ").append(pipelineData);
            return result.toString();
        });
    }

    /**
     * 冗余执行子Agent（容错机制）
     */
    private CompletableFuture<String> executeRedundant(List<SubAgent> agents) {
        // 执行所有Agent并选择最佳结果
        List<CompletableFuture<String>> futures = agents.stream()
                .map(agent -> agent.executeTask(agent.getTaskDescription()))
                .collect(Collectors.toList());

        return CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(result -> {
                    StringBuilder output = new StringBuilder();
                    output.append("=== Redundant Execution Results ===\n");
                    output.append("Best Result Selected: ").append(result);
                    return output.toString();
                });
    }

    /**
     * 自适应执行子Agent
     */
    private CompletableFuture<String> executeAdaptive(List<SubAgent> agents) {
        // 根据任务复杂度动态选择执行策略
        return CompletableFuture.supplyAsync(() -> {
            int complexity = calculateTaskComplexity(agents);

            if (complexity <= 3) {
                return executeParallel(agents).join();
            } else if (complexity <= 6) {
                return executeSequential(agents).join();
            } else {
                return executePipeline(agents).join();
            }
        });
    }

    /**
     * 共享数据空间操作
     */
    public void putSharedData(String key, Object data) {
        sharedDataSpace.put(key, data);
        logger.debug("Shared data updated: {} = {}", key, data);
    }

    public Object getSharedData(String key) {
        return sharedDataSpace.get(key);
    }

    public Map<String, Object> getAllSharedData() {
        return new HashMap<>(sharedDataSpace);
    }

    public void removeSharedData(String key) {
        sharedDataSpace.remove(key);
    }

    /**
     * 子Agent间通信
     */
    public void sendMessage(String fromAgentId, String toAgentId, Object message) {
        SubAgent fromAgent = activeSubAgents.get(fromAgentId);
        SubAgent toAgent = activeSubAgents.get(toAgentId);

        if (fromAgent != null && toAgent != null) {
            logger.debug("Message sent from {} to {}: {}", fromAgentId, toAgentId, message);
        }
    }

    public void broadcastMessage(String fromAgentId, Object message) {
        for (Map.Entry<String, SubAgent> entry : activeSubAgents.entrySet()) {
            if (!entry.getKey().equals(fromAgentId)) {
                logger.debug("Broadcast message from {} to agent {}: {}", fromAgentId, entry.getKey(), message);
            }
        }
        logger.debug("Broadcast message from {} to all agents: {}", fromAgentId, message);
    }

    /**
     * 获取Agent状态
     */
    public Map<String, AgentStatus> getAgentStatuses() {
        Map<String, AgentStatus> statuses = new HashMap<>();
        for (Map.Entry<String, SubAgent> entry : activeSubAgents.entrySet()) {
            statuses.put(entry.getKey(), entry.getValue().getStatus());
        }
        return statuses;
    }

    public List<String> getActiveAgentIds() {
        List<String> agentIds = new ArrayList<>();
        for (SubAgent agent : activeSubAgents.values()) {
            agentIds.add(agent.getAgentId());
        }
        return agentIds;
    }

    /**
     * 清理完成的Agent
     */
    public void cleanupCompletedAgents() {
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, SubAgent> entry : activeSubAgents.entrySet()) {
            if (entry.getValue().getStatus() == com.ai.infrastructure.agent.AgentStatus.COMPLETED ||
                    entry.getValue().getStatus() == com.ai.infrastructure.agent.AgentStatus.ERROR) {
                toRemove.add(entry.getKey());
                completedSubAgents.put(entry.getKey(), entry.getValue());
            }
        }

        for (String agentId : toRemove) {
            activeSubAgents.remove(agentId);
            logger.info("Cleaned up completed agent: {}", agentId);
        }
    }

    /**
     * 获取系统统计信息
     */
    public SubAgentStats getStats() {
        return new SubAgentStats(
                activeSubAgents.size(),
                completedSubAgents.size(),
                sharedDataSpace.size(),
                agentIdCounter.get()
        );
    }

    /**
     * 私有辅助方法
     */
    private String generateAgentId(AgentType agentType) {
        return String.format("%s_%d", agentType.name().toLowerCase(), agentIdCounter.incrementAndGet());
    }

    private AgentType determineAgentTypeForTask(String task) {
        String lowerTask = task.toLowerCase();

        if (lowerTask.contains("read") || lowerTask.contains("write") || lowerTask.contains("file")) {
            return AgentType.I2A;
        } else if (lowerTask.contains("user") || lowerTask.contains("input") || lowerTask.contains("process")) {
            return AgentType.UH1;
        } else if (lowerTask.contains("search") || lowerTask.contains("knowledge") || lowerTask.contains("research")) {
            return AgentType.KN5;
        } else {
            return AgentType.GENERAL;
        }
    }

    private void establishCollaborationLinks(List<SubAgent> agents, CollaborationType collaborationType) {
        for (int i = 0; i < agents.size(); i++) {
            SubAgent current = agents.get(i);

            // 设置协作类型
            current.setCollaborationType(collaborationType);
            current.setGroupId("collaboration-group-" + System.currentTimeMillis());
        }
    }

    private int calculateTaskComplexity(List<SubAgent> agents) {
        return agents.size(); // 简化计算
    }
}