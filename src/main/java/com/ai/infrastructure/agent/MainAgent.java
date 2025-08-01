package com.ai.infrastructure.agent;

import com.ai.infrastructure.agent.unified.UnifiedMainAgent;
import com.ai.infrastructure.agent.unified.UnifiedAgentContext;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;

/**
 * 新架构的MainAgent适配器
 * 保持原有MainAgent的API兼容性，内部使用统一架构
 */
public class MainAgent extends BaseAgent {
    private static final Logger logger = LoggerFactory.getLogger(MainAgent.class);
    
    private final UnifiedMainAgent unifiedMainAgent;
    private final String apiKey;
    
    // 已废弃的字段: 仅用于向后兼容
    @Deprecated
    private List<SubAgent> subAgents;
    
    public MainAgent(String agentId, String name, String apiKey) {
        super(agentId, name);
        this.apiKey = apiKey;
        
        // 使用统一架构
        UnifiedAgentContext context = UnifiedAgentContext.getInstance();
        this.unifiedMainAgent = new UnifiedMainAgent(agentId, name, context);
        
        // 初始化兼容性字段
        this.subAgents = new ArrayList<>();
        
        setStatus(AgentStatus.IDLE);
        logger.info("MainAgent initialized with unified architecture: {}", agentId);
    }
    
    @Override
    public CompletableFuture<String> executeTask(String task) {
        logger.debug("MainAgent delegating to unified architecture: {}", task);
        setStatus(AgentStatus.RUNNING);
        return unifiedMainAgent.executeMainTask(task);
    }
    
    /**
     * Get OpenAI model client (for compatibility)
     */
    public com.ai.infrastructure.model.OpenAIModelClient getOpenAIModelClient() {
        return null; // Method deprecated in new architecture
    }
    
    /**
     * Add sub agent (for compatibility)
     */
    public void addSubAgent(SubAgent subAgent) {
        logger.warn("addSubAgent() is deprecated. Use UnifiedMainAgent.createAgent() instead.");
        // 创建对应的统一Agent
        if (subAgent != null) {
            unifiedMainAgent.createAgent(
                subAgent.getAgentId(), 
                subAgent.getName(), 
                AgentType.valueOf(subAgent.getAgentType().name())
            );
            subAgents.add(subAgent);
        }
    }
    
    /**
     * Get sub agents (for compatibility)
     */
    public List<SubAgent> getSubAgents() {
        return new ArrayList<>(subAgents);
    }
    
    /**
     * Coordinate sub agent task (for compatibility)
     */
    public CompletableFuture<String> coordinateSubAgentTask(String task) {
        logger.debug("Delegating sub-agent coordination to unified architecture");
        
        // Map subAgents to unified agents
        String[] agentIds = subAgents.stream()
            .map(s -> s.getAgentId())
            .toArray(String[]::new);
            
        return unifiedMainAgent.executeCollaborativeTask(task, agentIds, "parallel");
    }
    
    /**
     * Get sub agent count (for compatibility)
     */
    public int getSubAgentCount() {
        return unifiedMainAgent.getSystemStatus().get("totalAgents") != null ? 
               (Integer) unifiedMainAgent.getSystemStatus().get("totalAgents") : 0;
    }
    
    /**
     * Get resource usage (enhanced with unified metrics)
     */
    public String getResourceUsage() {
        return unifiedMainAgent.getResourceUsage();
    }
    
    @Override
    public void abort() {
        super.abort();
        unifiedMainAgent.shutdown();
    }
    
    /**
     * Get unified MainAgent for new architecture access
     */
    public UnifiedMainAgent getUnifiedMainAgent() {
        return unifiedMainAgent;
    }
}