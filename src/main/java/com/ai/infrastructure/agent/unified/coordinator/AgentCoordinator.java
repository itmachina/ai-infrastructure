package com.ai.infrastructure.agent.unified.coordinator;

import com.ai.infrastructure.agent.AgentStatus;
import com.ai.infrastructure.agent.unified.BaseUnifiedAgent;
import com.ai.infrastructure.agent.unified.UnifiedAgentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 统一的Agent协调器
 * 解决原有架构中多套并行Agent管理机制的冲突
 */
public class AgentCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(AgentCoordinator.class);
    
    // 协调器环境
    private final UnifiedAgentContext context;
    private final ExecutorService coordinationExecutor;
    
    // Agent注册表
    private final Map<String, BaseUnifiedAgent> registeredAgents;
    private final Map<String, List<String>> collaborationGroups;
    private final Map<String, AgentLoadInfo> agentLoadInfo;
    
    // 协调策略
    private final Map<String, CoordinationStrategy> coordinationStrategies;
    private final com.ai.infrastructure.agent.unified.coordinator.CoordinationMetrics coordinationMetrics;
    
    // 配置
    private final int maxCollaborationSize;
    private final long coordinationTimeout;
    private final boolean enableLoadBalancing;
    private final boolean enableAutoScaling;
    
    public AgentCoordinator(UnifiedAgentContext context) {
        this.context = context;
        this.coordinationExecutor = Executors.newFixedThreadPool(10);
        
        this.registeredAgents = new ConcurrentHashMap<>();
        this.collaborationGroups = new ConcurrentHashMap<>();
        this.agentLoadInfo = new ConcurrentHashMap<>();
        
        this.coordinationStrategies = new ConcurrentHashMap<>();
        this.coordinationMetrics = new com.ai.infrastructure.agent.unified.coordinator.CoordinationMetrics();
        
        this.maxCollaborationSize = 10;
        this.coordinationTimeout = 30000L; // 30秒
        this.enableLoadBalancing = true;
        this.enableAutoScaling = true;
        
        initializeCoordinationStrategies();
        logger.info("AgentCoordinator initialized");
    }
    
    /**
     * 初始化协调策略
     */
    private void initializeCoordinationStrategies() {
        coordinationStrategies.put("parallel", new ParallelCoordinationStrategy());
        coordinationStrategies.put("sequential", new SequentialCoordinationStrategy());
        coordinationStrategies.put("adaptive", new AdaptiveCoordinationStrategy());
        coordinationStrategies.put("pipeline", new PipelineCoordinationStrategy());
        coordinationStrategies.put("load_balanced", new LoadBalancedCoordinationStrategy());
    }
    
    /**
     * 注册Agent
     */
    public boolean registerAgent(BaseUnifiedAgent agent) {
        if (agent == null || agent.getAgentId() == null) {
            logger.error("Invalid agent provided for registration");
            return false;
        }
        
        try {
            registeredAgents.put(agent.getAgentId(), agent);
            agentLoadInfo.put(agent.getAgentId(), new AgentLoadInfo(agent));
            
            logger.info("Agent registered: {} ({})", agent.getAgentId(), agent.getAgentType());
            coordinationMetrics.incrementRegistrationCount();
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to register agent {}: {}", agent.getAgentId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 注销Agent
     */
    public boolean unregisterAgent(String agentId) {
        BaseUnifiedAgent agent = registeredAgents.remove(agentId);
        if (agent != null) {
            agentLoadInfo.remove(agentId);
            
            // 清理协作组
            cleanupCollaborationGroups(agentId);
            
            logger.info("Agent unregistered: {}", agentId);
            coordinationMetrics.incrementUnregistrationCount();
            return true;
        }
        
        logger.warn("Agent not found for unregistration: {}", agentId);
        return false;
    }
    
    /**
     * 协调协作任务
     */
    public CompletableFuture<String> coordinateCollaboration(String initiatorAgentId, 
                                                             String[] partnerAgentIds, 
                                                             String task) {
        logger.info("Coordinating collaboration: {} -> {} for task: {}", 
                   initiatorAgentId, Arrays.toString(partnerAgentIds), task);
        
        // 验证参与者
        if (!validateCollaborationParticipants(initiatorAgentId, partnerAgentIds)) {
            return CompletableFuture.completedFuture("协作验证失败：无效的参与者");
        }
        
        coordinationMetrics.incrementCollaborationRequestCount();
        
        // 选择协作策略
        CoordinationStrategy strategy = selectOptimalStrategy(task, partnerAgentIds.length);
        
        // 执行协作
        return executeCollaboration(initiatorAgentId, partnerAgentIds, task, strategy)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    coordinationMetrics.incrementCollaborationFailureCount();
                    logger.error("Collaboration failed: {}", throwable.getMessage(), throwable);
                } else {
                    coordinationMetrics.incrementCollaborationSuccessCount();
                    logger.debug("Collaboration completed successfully");
                }
            });
    }
    
    /**
     * 执行协作任务
     */
    private CompletableFuture<String> executeCollaboration(String initiatorAgentId, 
                                                           String[] partnerAgentIds, 
                                                           String task, 
                                                           CoordinationStrategy strategy) {
        long startTime = System.currentTimeMillis();
        
        return strategy.execute(
            initiatorAgentId,
            partnerAgentIds,
            task,
            this,
            coordinationExecutor
        ).whenComplete((result, throwable) -> {
            long duration = System.currentTimeMillis() - startTime;
            coordinationMetrics.recordCollaborationDuration(duration);
            
            if (throwable != null) {
                logger.error("Collaboration execution failed in {}ms: {}", duration, throwable.getMessage());
            } else {
                logger.debug("Collaboration completed in {}ms", duration);
            }
        });
    }
    
    /**
     * 验证协作参与者
     */
    private boolean validateCollaborationParticipants(String initiatorAgentId, String[] partnerAgentIds) {
        // 检查发起者
        BaseUnifiedAgent initiator = registeredAgents.get(initiatorAgentId);
        if (initiator == null) {
            logger.error("Initiator agent not found: {}", initiatorAgentId);
            return false;
        }
        
        // 检查协作大小限制
        int totalParticipants = partnerAgentIds.length + 1;
        if (totalParticipants > maxCollaborationSize) {
            logger.error("Collaboration size ({}) exceeds maximum allowed ({}): {} + {} = {}",
                       totalParticipants, maxCollaborationSize, 1, partnerAgentIds.length, totalParticipants);
            return false;
        }
        
        // 检查所有参与者
        for (String partnerId : partnerAgentIds) {
            BaseUnifiedAgent partner = registeredAgents.get(partnerId);
            if (partner == null) {
                logger.error("Partner agent not found: {}", partnerId);
                return false;
            }
            
            // 检查参与者状态
            if (partner.getStatus() != AgentStatus.IDLE) {
                logger.error("Partner agent {} is not idle (status: {})", 
                           partnerId, partner.getStatus());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 选择最优协作策略
     */
    private CoordinationStrategy selectOptimalStrategy(String task, int participantCount) {
        if (enableLoadBalancing && participantCount > 3) {
            return coordinationStrategies.get("load_balanced");
        }
        
        if (task.contains("流水线") || task.contains("pipeline")) {
            return coordinationStrategies.get("pipeline");
        }
        
        if (task.contains("顺序") || task.contains("sequential")) {
            return coordinationStrategies.get("sequential");
        }
        
        if (task.contains("自适应") || task.contains("adaptive")) {
            return coordinationStrategies.get("adaptive");
        }
        
        // 默认并行策略
        return coordinationStrategies.get("parallel");
    }
    
    /**
     * 执行负载均衡的协作
     */
    public CompletableFuture<String> executeLoadBalancedCollaboration(String[] agentIds, String task) {
        if (!enableLoadBalancing) {
            return CompletableFuture.completedFuture("负载均衡未启用");
        }
        
        // 根据负载选择最佳Agent
        List<String> selectedAgents = selectAgentsByLoad(agentIds, Math.min(3, agentIds.length));
        
        if (selectedAgents.isEmpty()) {
            return CompletableFuture.completedFuture("没有可用的Agent");
        }
        
        return coordinateCollaboration(selectedAgents.get(0), 
                                      selectedAgents.subList(1, selectedAgents.toArray().length).toArray(new String[0]), 
                                      task);
    }
    
    /**
     * 根据负载选择Agent
     */
    private List<String> selectAgentsByLoad(String[] agentIds, int count) {
        return Arrays.stream(agentIds)
            .filter(registeredAgents::containsKey)
            .map(agentId -> {
                AgentLoadInfo loadInfo = agentLoadInfo.get(agentId);
                double loadScore = loadInfo != null ? loadInfo.getLoadScore() : 0.0;
                return new AgentCandidate(agentId, loadScore);
            })
            .sorted((a1, a2) -> Double.compare(a1.loadScore, a2.loadScore))
            .limit(count)
            .map(candidate -> candidate.agentId)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取Agent状态
     */
    public Map<String, AgentStatus> getAgentStatuses() {
        Map<String, AgentStatus> statuses = new ConcurrentHashMap<>();
        registeredAgents.forEach((agentId, agent) -> {
            statuses.put(agentId, agent.getStatus());
        });
        return statuses;
    }
    
    /**
     * 获取负载信息
     */
    public Map<String, Double> getAgentLoadScores() {
        Map<String, Double> loadScores = new ConcurrentHashMap<>();
        agentLoadInfo.forEach((agentId, loadInfo) -> {
            loadScores.put(agentId, loadInfo.getLoadScore());
        });
        return loadScores;
    }
    
    /**
     * 获取注册的Agent数量
     */
    public int getStatsCount() {
        return registeredAgents.size();
    }
    
    /**
     * 创建协作组
     */
    public String createCollaborationGroup(String groupId, String[] agentIds) {
        if (collaborationGroups.containsKey(groupId)) {
            logger.warn("Collaboration group already exists: {}", groupId);
            return groupId;
        }
        
        List<String> validatedAgents = new ArrayList<>();
        for (String agentId : agentIds) {
            if (registeredAgents.containsKey(agentId)) {
                validatedAgents.add(agentId);
            } else {
                logger.warn("Agent not found, skipping from group: {}", agentId);
            }
        }
        
        collaborationGroups.put(groupId, validatedAgents);
        logger.info("Collaboration group created: {} with {} agents", groupId, validatedAgents.size());
        
        return groupId;
    }
    
    /**
     * 执行协作组任务
     */
    public CompletableFuture<String> executeCollaborationGroup(String groupId, String task) {
        List<String> agentIds = collaborationGroups.get(groupId);
        if (agentIds == null || agentIds.isEmpty()) {
            return CompletableFuture.completedFuture("协作组不存在或为空");
        }
        
        String initiator = agentIds.get(0);
        String[] partners = agentIds.subList(1, agentIds.size()).toArray(new String[0]);
        
        return coordinateCollaboration(initiator, partners, task);
    }
    
    /**
     * 清理协作组
     */
    private void cleanupCollaborationGroups(String agentId) {
        collaborationGroups.values().removeIf(agentList -> agentList.remove(agentId));
        logger.debug("Cleaned up collaboration groups for agent: {}", agentId);
    }
    
    /**
     * 更新Agent负载信息
     */
    public void updateAgentLoad(String agentId, double loadScore) {
        AgentLoadInfo loadInfo = agentLoadInfo.get(agentId);
        if (loadInfo != null) {
            loadInfo.updateLoad(loadScore);
        }
    }
    
    /**
     * 获取协调指标
     */
    public com.ai.infrastructure.agent.unified.coordinator.CoordinationMetrics getCoordinationMetrics() {
        return coordinationMetrics;
    }
    
    /**
     * 获取注册的Agents
     */
    public Map<String, BaseUnifiedAgent> getRegisteredAgents() {
        return new ConcurrentHashMap<>(registeredAgents);
    }
    
    /**
     * 关闭协调器
     */
    public void shutdown() {
        logger.info("Shutting down AgentCoordinator");
        
        coordinationExecutor.shutdown();
        try {
            if (!coordinationExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                coordinationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            coordinationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        registeredAgents.clear();
        collaborationGroups.clear();
        agentLoadInfo.clear();
        
        logger.info("AgentCoordinator shutdown completed");
    }
    
    /**
     * Agent候选人
     */
    private static class AgentCandidate {
        final String agentId;
        final double loadScore;
        
        AgentCandidate(String agentId, double loadScore) {
            this.agentId = agentId;
            this.loadScore = loadScore;
        }
    }
    
    /**
     * Agent负载信息
     */
    public static class AgentLoadInfo {
        private final BaseUnifiedAgent agent;
        private double currentLoad;
        private int totalTasks;
        private int completedTasks;
        private int failedTasks;
        private long lastUpdate;
        
        public AgentLoadInfo(BaseUnifiedAgent agent) {
            this.agent = agent;
            this.currentLoad = 0.0;
            this.totalTasks = 0;
            this.completedTasks = 0;
            this.failedTasks = 0;
            this.lastUpdate = System.currentTimeMillis();
        }
        
        public double getLoadScore() {
            return currentLoad;
        }
        
        public void updateLoad(double loadScore) {
            this.currentLoad = loadScore;
            this.lastUpdate = System.currentTimeMillis();
        }
        
        public void recordTask(boolean success) {
            totalTasks++;
            if (success) {
                completedTasks++;
            } else {
                failedTasks++;
            }
            
            // 重新计算负载
            recalculateLoad();
        }
        
        private void recalculateLoad() {
            if (totalTasks == 0) {
                currentLoad = 0.0;
            } else {
                double failureRate = (double) failedTasks / totalTasks;
                currentLoad = failureRate * 0.3 + (agent.isActive() ? 0.7 : 0.0);
            }
        }
        
        public int getTotalTasks() {
            return totalTasks;
        }
        
        public int getCompletedTasks() {
            return completedTasks;
        }
        
        public int getFailedTasks() {
            return failedTasks;
        }
        
        public long getLastUpdate() {
            return lastUpdate;
        }
    }
}
