package com.ai.infrastructure.scheduler;

import com.ai.infrastructure.agent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 智能Agent分配测试
 * 测试基于大模型的智能Agent分配决策
 */
public class IntelligentAllocationTest {
    private static final Logger logger = LoggerFactory.getLogger(IntelligentAllocationTest.class);

    public static void main(String[] args) {
        logger.info("=== 智能Agent分配测试开始 ===");

        try {
            // 测试1: 创建智能分配器
            testIntelligentAllocatorCreation();

            // 测试2: 测试不同类型的任务分配
            testDifferentTaskTypes();

            // 测试3: 测试智能分配与手动分配的对比
            testIntelligentVsManualAllocation();

            // 测试4: 测试错误处理和回退机制
            testFallbackAllocation();

            logger.info("=== 智能Agent分配测试完成 ===");

        } catch (Exception e) {
            logger.error("=== 智能Agent分配测试失败 ===", e);
            e.printStackTrace();
        }
    }

    private static void testIntelligentAllocatorCreation() {
        logger.info("测试1: 创建智能分配器");

        // 创建Agent池
        Map<AgentType, List<SpecializedAgent>> agentPools = createTestAgentPools();
        Map<String, SpecializedAgent> busyAgents = new HashMap<>();

        // 创建智能分配器（传入API key用于测试）
        String testApiKey = System.getenv("AI_API_KEY");
        IntelligentAgentAllocator allocator = new IntelligentAgentAllocator(agentPools, busyAgents, testApiKey);

        logger.info("智能分配器创建成功");
        logger.info("分配器统计信息:\n{}", allocator.getAllocationStatistics());
    }

    private static void testDifferentTaskTypes() {
        logger.info("\n测试2: 测试不同类型的任务分配");

        Map<AgentType, List<SpecializedAgent>> agentPools = createTestAgentPools();
        Map<String, SpecializedAgent> busyAgents = new HashMap<>();
        String testApiKey = System.getenv("AI_API_KEY");
        IntelligentAgentAllocator allocator = new IntelligentAgentAllocator(agentPools, busyAgents, testApiKey);

        // 测试不同类型的任务
        String[] testTasks = {
                "设计用户友好的交互界面原型，包含数据可视化组件",
                "解析用户输入数据并验证其完整性，转换为标准格式",
                "分析大型数据集并生成知识推理报告，提供决策建议",
                "处理用户请求并生成响应，确保数据格式正确",
                "学习新技术知识并推理其在项目中的应用价值"
        };

        for (String task : testTasks) {
            logger.info("\n测试任务: {}", task);

            try {
                CompletableFuture<SpecializedAgent> future = allocator.allocateOptimalAgent(
                        task, IntelligentTaskDecomposer.TaskPriority.MEDIUM
                );

                SpecializedAgent selectedAgent = future.get(10, TimeUnit.SECONDS);

                if (selectedAgent != null) {
                    logger.info("✅ 智能分配选择Agent: {}", selectedAgent.getAgentId());
                    logger.info("   Agent类型: {}", selectedAgent.getAgentType().getDisplayName());
                    logger.info("   Agent状态: {}", selectedAgent.getStatus());
                    logger.info("   完成率: {}", String.format("%.1f%%", selectedAgent.getCompletionRate() * 100));
                } else {
                    logger.warn("❌ 智能分配失败");
                }

            } catch (Exception e) {
                logger.error("❌ 智能分配异常: {}", e.getMessage());
            }
        }
    }

    private static void testIntelligentVsManualAllocation() {
        logger.info("\n测试3: 智能分配 vs 手动分配对比");

        Map<AgentType, List<SpecializedAgent>> agentPools = createTestAgentPools();
        Map<String, SpecializedAgent> busyAgents = new HashMap<>();
        String testApiKey = System.getenv("AI_API_KEY");
        IntelligentAgentAllocator allocator = new IntelligentAgentAllocator(agentPools, busyAgents, testApiKey);

        // 创建智能负载均衡器
        IntelligentLoadBalancer loadBalancer = new IntelligentLoadBalancer(
                agentPools, 5, 10000, 3
        );

        // 测试任务
        String testTask = "分析用户行为数据并生成可视化报告，优化用户体验";

        logger.info("测试任务: {}", testTask);
        SpecializedAgent intelligentAgent = null;
        // 测试智能分配
        long startTime = System.currentTimeMillis();
        try {
            CompletableFuture<SpecializedAgent> intelligentFuture = allocator.allocateOptimalAgent(
                    testTask, IntelligentTaskDecomposer.TaskPriority.HIGH
            );

            intelligentAgent = intelligentFuture.get(10, TimeUnit.SECONDS);
            long intelligentTime = System.currentTimeMillis() - startTime;

            logger.info("🤖 智能分配结果:");
            logger.info("   选择Agent: {}", intelligentAgent != null ? intelligentAgent.getAgentId() : "无");
            logger.info("   耗时: {}ms", intelligentTime);

        } catch (Exception e) {
            logger.error("🤖 智能分配失败: {}", e.getMessage());
        }

        // 测试手动分配（基于规则的分配）
        startTime = System.currentTimeMillis();
        try {
            Map<AgentType, Double> requirements = analyzeTaskRequirements(testTask);
            SpecializedAgent manualAgent = selectOptimalAgentManually(agentPools, busyAgents, requirements);
            long manualTime = System.currentTimeMillis() - startTime;

            logger.info("📋 手动分配结果:");
            logger.info("   选择Agent: {}", manualAgent != null ? manualAgent.getAgentId() : "无");
            logger.info("   耗时: {}ms", manualTime);

            // 对比分析
            if (intelligentAgent != null && manualAgent != null) {
                boolean sameAgent = intelligentAgent.getAgentId().equals(manualAgent.getAgentId());
                logger.info("📊 分配对比: {}", sameAgent ? "一致" : "不一致");
                if (!sameAgent) {
                    logger.info("   智能选择更适合的任务类型");
                }
            }

        } catch (Exception e) {
            logger.error("📋 手动分配失败: {}", e.getMessage());
        }
    }

    private static void testFallbackAllocation() {
        logger.info("\n测试4: 测试错误处理和回退机制");

        Map<AgentType, List<SpecializedAgent>> agentPools = createTestAgentPools();
        Map<String, SpecializedAgent> busyAgents = new HashMap<>();

        // 模拟所有Agent都忙碌的情况
        for (List<SpecializedAgent> agents : agentPools.values()) {
            for (SpecializedAgent agent : agents) {
                busyAgents.put(agent.getAgentId(), agent);
            }
        }

        String testApiKey = System.getenv("AI_API_KEY");
        IntelligentAgentAllocator allocator = new IntelligentAgentAllocator(agentPools, busyAgents, testApiKey);

        // 测试任务
        String testTask = "处理紧急的用户请求";

        logger.info("测试任务: {}", testTask);
        logger.info("模拟所有Agent忙碌状态...");

        try {
            CompletableFuture<SpecializedAgent> future = allocator.allocateOptimalAgent(
                    testTask, IntelligentTaskDecomposer.TaskPriority.HIGH
            );

            SpecializedAgent selectedAgent = future.get(5, TimeUnit.SECONDS);

            if (selectedAgent != null) {
                logger.info("✅ 回退机制成功，选择Agent: {}", selectedAgent.getAgentId());
            } else {
                logger.warn("❌ 回退机制失败，无可用Agent");
            }

        } catch (Exception e) {
            logger.error("❌ 回退机制测试异常: {}", e.getMessage());
        }
    }

    /**
     * 手动分配Agent方法（用于对比）
     */
    private static SpecializedAgent selectOptimalAgentManually(
            Map<AgentType, List<SpecializedAgent>> agentPools,
            Map<String, SpecializedAgent> busyAgents,
            Map<AgentType, Double> requirements
    ) {
        SpecializedAgent bestAgent = null;
        double bestScore = -1.0;

        for (Map.Entry<AgentType, Double> entry : requirements.entrySet()) {
            AgentType agentType = entry.getKey();
            double requirement = entry.getValue();

            if (requirement > 0) {
                List<SpecializedAgent> agents = agentPools.get(agentType);

                if (agents != null) {
                    for (SpecializedAgent agent : agents) {
                        boolean canAccept = agent.canAcceptTask();
                        boolean isBusy = busyAgents.containsKey(agent.getAgentId());
                        boolean available = canAccept && !isBusy;

                        if (available) {
                            double score = calculateAgentScoreManually(agent, requirement);

                            if (score > bestScore) {
                                bestScore = score;
                                bestAgent = agent;
                            }
                        }
                    }
                }
            }
        }

        return bestAgent;
    }

    /**
     * 手动计算Agent得分
     */
    private static double calculateAgentScoreManually(SpecializedAgent agent, double requirement) {
        double loadScore = 1.0 - agent.getLoadScore();
        double capabilityScore = requirement;
        double performanceScore = agent.getCompletionRate();

        return loadScore * 0.4 + capabilityScore * 0.4 + performanceScore * 0.2;
    }

    /**
     * 分析任务需求
     */
    private static Map<AgentType, Double> analyzeTaskRequirements(String taskDescription) {
        Map<AgentType, Double> requirements = new HashMap<>();

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

        // 默认处理
        if (i2aRequirement == 0.0 && uh1Requirement == 0.0 && kn5Requirement == 0.0) {
            requirements.put(AgentType.UH1, 0.5);
        }

        return requirements;
    }

    /**
     * 创建测试Agent池
     */
    private static Map<AgentType, List<SpecializedAgent>> createTestAgentPools() {
        Map<AgentType, List<SpecializedAgent>> agentPools = new HashMap<>();

        // I2A Agent池
        List<SpecializedAgent> i2aAgents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            i2aAgents.add(new InteractionAgent("i2a_" + i, "I2A Agent-" + i));
        }
        agentPools.put(AgentType.I2A, i2aAgents);

        // UH1 Agent池
        List<SpecializedAgent> uh1Agents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            uh1Agents.add(new UserProcessingAgent("uh1_" + i, "UH1 Agent-" + i));
        }
        agentPools.put(AgentType.UH1, uh1Agents);

        // KN5 Agent池
        List<SpecializedAgent> kn5Agents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            kn5Agents.add(new KnowledgeProcessingAgent("kn5_" + i, "KN5 Agent-" + i));
        }
        agentPools.put(AgentType.KN5, kn5Agents);

        return agentPools;
    }
}