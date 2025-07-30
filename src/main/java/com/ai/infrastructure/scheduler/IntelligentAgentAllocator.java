package com.ai.infrastructure.scheduler;

import com.ai.infrastructure.agent.AgentType;
import com.ai.infrastructure.agent.SpecializedAgent;
import com.ai.infrastructure.model.OpenAIModelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 智能Agent分配器
 * 使用大模型分析任务需求并选择最适合的Agent
 */
public class IntelligentAgentAllocator {
    private static final Logger logger = LoggerFactory.getLogger(IntelligentAgentAllocator.class);
    
    private OpenAIModelClient openAIModelClient;
    private final Map<AgentType, List<SpecializedAgent>> agentPools;
    private final Map<String, SpecializedAgent> busyAgents;
    
    // Agent能力描述
    private static final Map<AgentType, String> agentDescriptions = Map.of(
        AgentType.I2A,
        "I2A交互Agent：专门负责用户交互、界面设计、展示演示、用户反馈处理等任务。擅长创建交互原型、用户界面、生成演示报告和优化用户体验。",
        
        AgentType.UH1,
        "UH1用户处理Agent：专门负责用户请求解析、数据处理、格式转换、验证计算等任务。擅长处理用户输入、数据格式化、计算验证和结构化数据处理。",
        
        AgentType.KN5,
        "KN5知识处理Agent：专门负责知识推理、分析决策、学习优化等任务。擅长分析问题、推理逻辑、学习新知识、提供决策建议和优化方案。"
    );
    
    // Agent限制和约束
    private static final Map<AgentType, Integer> agentCapacityLimits = Map.of(
        AgentType.I2A, 3,
        AgentType.UH1, 5,
        AgentType.KN5, 2
    );
    
    public IntelligentAgentAllocator(Map<AgentType, List<SpecializedAgent>> agentPools, 
                                   Map<String, SpecializedAgent> busyAgents) {
        this(agentPools, busyAgents, null);
    }
    
    public IntelligentAgentAllocator(Map<AgentType, List<SpecializedAgent>> agentPools, 
                                   Map<String, SpecializedAgent> busyAgents,
                                   String apiKey) {
        this.agentPools = agentPools;
        this.busyAgents = busyAgents;
        
        // 初始化OpenAI客户端，如果没有提供API密钥则使用模拟模式
        if (apiKey != null && !apiKey.isEmpty()) {
            this.openAIModelClient = new OpenAIModelClient(apiKey);
            logger.info("Intelligent Agent Allocator initialized with OpenAI API");
        } else {
            this.openAIModelClient = null; // 模拟模式
            logger.info("Intelligent Agent Allocator initialized in simulation mode");
        }
        
        logger.info("Intelligent Agent Allocator initialized with {} agent types", 
                   agentPools.size());
    }
    
    /**
     * 使用大模型分析任务需求并选择最优Agent
     */
    public CompletableFuture<SpecializedAgent> allocateOptimalAgent(String taskDescription, 
                                                                  IntelligentTaskDecomposer.TaskPriority priority) {
        logger.info("Analyzing task for optimal agent allocation: {}", taskDescription);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 构建Agent候选列表（过滤掉忙碌的Agent）
                List<AgentCandidate> candidates = buildAgentCandidates();
                
                if (candidates.isEmpty()) {
                    logger.warn("No available agents found for task: {}", taskDescription);
                    return null;
                }
                
                // 2. 构建大模型提示
                String prompt = buildAllocationPrompt(taskDescription, priority, candidates);
                
                // 3. 调用大模型进行决策
                String llmResponse = callModelWithFallback(prompt, 0.7);
                
                // 4. 解析大模型响应
                SpecializedAgent selectedAgent = parseLLMResponse(llmResponse, candidates, taskDescription);
                
                if (selectedAgent != null) {
                    logger.info("LLM selected agent {} for task: {}", 
                               selectedAgent.getAgentId(), taskDescription);
                } else {
                    // 回退到基于规则的分配
                    selectedAgent = fallbackAllocation(candidates, taskDescription);
                    logger.info("Using fallback allocation for agent selection: {}", 
                               selectedAgent.getAgentId());
                }
                
                return selectedAgent;
                
            } catch (Exception e) {
                logger.warn("Error in intelligent agent allocation: {}", e.getMessage(), e);
                // 回退到基于规则的分配
                List<AgentCandidate> candidates = buildAgentCandidates();
                return candidates.isEmpty() ? null : fallbackAllocation(candidates, taskDescription);
            }
        });
    }
    
    /**
     * 调用大模型（带回退机制）
     */
    private String callModelWithFallback(String prompt, double temperature) {
        if (openAIModelClient != null) {
            try {
                String systemMessage = "你是一个智能任务分配专家，请仔细分析任务需求并选择最适合的执行Agent。" +
                                     "请严格按照指定的JSON格式返回结果，确保选择的Agent是最匹配任务需求的。";
                
                String response = openAIModelClient.callModel(prompt, systemMessage);
                logger.debug("LLM response received: {}", response);
                return response;
                
            } catch (Exception e) {
                logger.warn("LLM call failed, using fallback: {}", e.getMessage());
            }
        }
        
        // 模拟大模型响应
        return generateSimulatedLLMResponse(prompt);
    }
    
    /**
     * 生成模拟的大模型响应
     */
    private String generateSimulatedLLMResponse(String prompt) {
        // 根据提示内容生成模拟响应
        if (prompt.contains("交互") || prompt.contains("界面") || prompt.contains("展示")) {
            return "{\"selected_agent_index\": 0, \"confidence\": 0.85, \"reasoning\": \"任务涉及交互界面设计，选择I2A交互Agent最合适\", \"matching_score\": 0.90, \"expected_duration_ms\": 8000}";
        } else if (prompt.contains("处理") || prompt.contains("解析") || prompt.contains("数据")) {
            return "{\"selected_agent_index\": 1, \"confidence\": 0.90, \"reasoning\": \"任务涉及数据处理和解析，选择UH1用户处理Agent最合适\", \"matching_score\": 0.95, \"expected_duration_ms\": 5000}";
        } else if (prompt.contains("分析") || prompt.contains("推理") || prompt.contains("知识")) {
            return "{\"selected_agent_index\": 2, \"confidence\": 0.88, \"reasoning\": \"任务涉及知识推理和分析，选择KN5知识处理Agent最合适\", \"matching_score\": 0.92, \"expected_duration_ms\": 10000}";
        } else {
            // 默认选择
            return "{\"selected_agent_index\": 1, \"confidence\": 0.75, \"reasoning\": \"任务无明显特征，选择UH1作为通用处理器\", \"matching_score\": 0.70, \"expected_duration_ms\": 6000}";
        }
    }

    
    /**
     * 构建Agent候选列表
     */
    private List<AgentCandidate> buildAgentCandidates() {
        List<AgentCandidate> candidates = new ArrayList<>();
        
        for (Map.Entry<AgentType, List<SpecializedAgent>> entry : agentPools.entrySet()) {
            AgentType agentType = entry.getKey();
            List<SpecializedAgent> agents = entry.getValue();
            
            for (SpecializedAgent agent : agents) {
                if (agent.canAcceptTask() && !busyAgents.containsKey(agent.getAgentId())) {
                    AgentCandidate candidate = new AgentCandidate(agent, agentType);
                    candidates.add(candidate);
                }
            }
        }
        
        logger.debug("Built {} agent candidates", candidates.size());
        return candidates;
    }
    
    /**
     * 构建大模型提示
     */
    private String buildAllocationPrompt(String taskDescription, 
                                       IntelligentTaskDecomposer.TaskPriority priority,
                                       List<AgentCandidate> candidates) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个智能任务分配专家，需要根据任务描述选择最适合的执行Agent。\n\n");
        
        prompt.append("=== 任务信息 ===\n");
        prompt.append("任务描述: ").append(taskDescription).append("\n");
        prompt.append("任务优先级: ").append(priority).append("\n\n");
        
        prompt.append("=== 可用Agent列表 ===\n");
        
        for (int i = 0; i < candidates.size(); i++) {
            AgentCandidate candidate = candidates.get(i);
            prompt.append(String.format("%d. %s (%s)\n", 
                i + 1,
                candidate.agent.getAgentId(),
                candidate.agentType.getDisplayName()));
            prompt.append("   能力描述: ").append(agentDescriptions.get(candidate.agentType)).append("\n");
            prompt.append("   当前状态: ").append(candidate.agent.getStatus()).append("\n");
            prompt.append("   负载分数: ").append(String.format("%.2f", candidate.agent.getLoadScore())).append("\n");
            prompt.append("   完成率: ").append(String.format("%.1f%%", candidate.agent.getCompletionRate() * 100)).append("\n");
            prompt.append("   活跃任务数: ").append(candidate.agent.getActiveTasks()).append("\n\n");
        }
        
        prompt.append("=== 分配要求 ===\n");
        prompt.append("请基于以下标准选择最优Agent：\n");
        prompt.append("1. Agent的专业能力与任务需求的匹配度（最重要）\n");
        prompt.append("2. Agent的当前负载和工作负载\n");
        prompt.append("3. Agent的历史完成率和可靠性\n");
        prompt.append("4. Agent的优先级适配\n\n");
        
        prompt.append("=== 输出格式 ===\n");
        prompt.append("请按照以下JSON格式输出选择结果：\n");
        prompt.append("{\n");
        prompt.append("  \"selected_agent_index\": 1,\n");
        prompt.append("  \"confidence\": 0.95,\n");
        prompt.append("  \"reasoning\": \"选择理由详细说明\",\n");
        prompt.append("  \"matching_score\": 0.92,\n");
        prompt.append("  \"expected_duration_ms\": 5000\n");
        prompt.append("}\n\n");
        
        prompt.append("请仔细分析任务需求，选择最合适的Agent。");
        
        return prompt.toString();
    }
    
    /**
     * 解析大模型响应
     */
    private SpecializedAgent parseLLMResponse(String llmResponse, 
                                            List<AgentCandidate> candidates,
                                            String taskDescription) {
        try {
            // 简单的JSON解析（实际项目中应该使用JSON库）
            String jsonPattern = "\\{[^}]*\\}";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsonPattern, 
                java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(llmResponse);
            
            if (matcher.find()) {
                String jsonStr = matcher.group();
                
                // 提取selected_agent_index
                int selectedIndex = extractIntValue(jsonStr, "selected_agent_index", -1);
                
                if (selectedIndex >= 0 && selectedIndex < candidates.size()) {
                    logger.debug("LLM selected agent index: {}, reason: {}", 
                               selectedIndex, extractStringValue(jsonStr, "reasoning", ""));
                    return candidates.get(selectedIndex).agent;
                }
            }
            
            logger.warn("Failed to parse LLM response, using fallback");
            return null;
            
        } catch (Exception e) {
            logger.error("Error parsing LLM response: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从JSON字符串中提取整数值
     */
    private int extractIntValue(String jsonStr, String key, int defaultValue) {
        String pattern = String.format("\"%s\"\\s*:\\s*(\\d+)", key);
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(jsonStr);
        
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return defaultValue;
    }
    
    /**
     * 从JSON字符串中提取字符串值
     */
    private String extractStringValue(String jsonStr, String key, String defaultValue) {
        String pattern = String.format("\"%s\"\\s*:\\s*\"([^\"]*)\"", key);
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(jsonStr);
        
        if (m.find()) {
            return m.group(1);
        }
        return defaultValue;
    }
    
    /**
     * 回退分配算法
     */
    private SpecializedAgent fallbackAllocation(List<AgentCandidate> candidates, String taskDescription) {
        logger.debug("Using fallback allocation for task: {}", taskDescription);
        
        // 计算每个Agent的综合得分
        double bestScore = -1.0;
        SpecializedAgent bestAgent = null;
        
        for (AgentCandidate candidate : candidates) {
            double score = calculateCompositeScore(candidate, taskDescription);
            
            if (score > bestScore) {
                bestScore = score;
                bestAgent = candidate.agent;
            }
        }
        
        logger.debug("Fallback allocation selected agent {} with score {}", 
                   bestAgent.getAgentId(), bestScore);
        
        return bestAgent;
    }
    
    /**
     * 计算综合得分
     */
    private double calculateCompositeScore(AgentCandidate candidate, String taskDescription) {
        // 负载分数（负载越低分数越高）
        double loadScore = 1.0 - candidate.agent.getLoadScore();
        
        // 完成率分数
        double completionScore = candidate.agent.getCompletionRate();
        
        // 能力匹配分数
        double capabilityScore = calculateCapabilityMatch(candidate.agentType, taskDescription);
        
        // 综合分数
        return loadScore * 0.3 + completionScore * 0.2 + capabilityScore * 0.5;
    }
    
    /**
     * 计算能力匹配分数
     */
    private double calculateCapabilityMatch(AgentType agentType, String taskDescription) {
        String lowerTask = taskDescription.toLowerCase();
        
        switch (agentType) {
            case I2A:
                return calculateI2AMatchScore(lowerTask);
            case UH1:
                return calculateUH1MatchScore(lowerTask);
            case KN5:
                return calculateKN5MatchScore(lowerTask);
            default:
                return 0.0;
        }
    }
    
    private double calculateI2AMatchScore(String task) {
        double score = 0.0;
        
        // 交互相关关键词
        if (task.contains("交互") || task.contains("界面") || task.contains("展示")) score += 0.8;
        if (task.contains("用户") || task.contains("反馈")) score += 0.6;
        if (task.contains("报告") || task.contains("演示")) score += 0.5;
        if (task.contains("可视化") || task.contains("图表")) score += 0.7;
        
        return Math.min(score, 1.0);
    }
    
    private double calculateUH1MatchScore(String task) {
        double score = 0.0;
        
        // 处理相关关键词
        if (task.contains("处理") || task.contains("解析") || task.contains("转换")) score += 0.8;
        if (task.contains("验证") || task.contains("计算") || task.contains("格式化")) score += 0.7;
        if (task.contains("数据") || task.contains("输入") || task.contains("输出")) score += 0.6;
        if (task.contains("请求") || task.contains("响应")) score += 0.5;
        
        return Math.min(score, 1.0);
    }
    
    private double calculateKN5MatchScore(String task) {
        double score = 0.0;
        
        // 知识相关关键词
        if (task.contains("分析") || task.contains("推理") || task.contains("学习")) score += 0.8;
        if (task.contains("知识") || task.contains("决策") || task.contains("优化")) score += 0.7;
        if (task.contains("研究") || task.contains("评估") || task.contains("建议")) score += 0.6;
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 获取分配器统计信息
     */
    public String getAllocationStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== 智能Agent分配器统计 ===\n");
        
        // 统计各类型Agent数量
        int totalAvailable = 0;
        for (Map.Entry<AgentType, List<SpecializedAgent>> entry : agentPools.entrySet()) {
            AgentType type = entry.getKey();
            List<SpecializedAgent> agents = entry.getValue();
            int availableCount = (int) agents.stream()
                .filter(agent -> agent.canAcceptTask() && !busyAgents.containsKey(agent.getAgentId()))
                .count();
            
            stats.append(type.getDisplayName()).append(": ")
                 .append(availableCount).append("/").append(agents.size()).append(" 可用\n");
            
            totalAvailable += availableCount;
        }
        
        stats.append("总计: ").append(totalAvailable).append(" 个可用Agent\n");
        
        return stats.toString();
    }
    
    /**
     * Agent候选类
     */
    private static class AgentCandidate {
        final SpecializedAgent agent;
        final AgentType agentType;
        
        AgentCandidate(SpecializedAgent agent, AgentType agentType) {
            this.agent = agent;
            this.agentType = agentType;
        }
    }
}