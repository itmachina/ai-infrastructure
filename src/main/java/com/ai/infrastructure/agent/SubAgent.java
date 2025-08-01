package com.ai.infrastructure.agent;

import com.ai.infrastructure.agent.unified.UnifiedAgent;
import com.ai.infrastructure.agent.unified.UnifiedAgentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * SubAgent适配器类
 * 保持SubAgent API兼容性，使用统一Agent实现
 * 已标记为@Deprecated，建议直接使用UnifiedAgent
 */
@Deprecated
public class SubAgent extends BaseAgent {
    private static final Logger logger = LoggerFactory.getLogger(SubAgent.class);
    
    private final UnifiedAgent unifiedAgent;
    private final String taskDescription;
    private final String parentAgentId;
    
    public SubAgent(String agentId, String name) {
        this(agentId, name, "main-agent");
    }
    
    public SubAgent(String agentId, String taskDescription, String parentAgentId) {
        super(agentId, "SubAgent-" + agentId);
        this.taskDescription = taskDescription;
        this.parentAgentId = parentAgentId;
        
        // 创建对应的统一Agent
        UnifiedAgentContext context = UnifiedAgentContext.getInstance();
        this.unifiedAgent = new UnifiedAgent(agentId, "SubAgent-" + agentId, 
                                           AgentType.GENERAL, context);
    }

    
    @Override
    public CompletableFuture<String> executeTask(String task) {
        logger.debug("SubAgent delegating to unified agent: {}", getAgentId());
        setStatus(AgentStatus.RUNNING);
        return unifiedAgent.executeTask(task);
    }
    
    /**
     * Get task description
     */
    public String getTaskDescription() {
        return taskDescription;
    }
    
    /**
     * Get parent agent ID
     */
    public String getParentAgentId() {
        return parentAgentId;
    }
    
    /**
     * Set agent type
     */
    public void setAgentType(AgentType agentType) {
        logger.warn("setAgentType() is deprecated in unified architecture");
    }
    
    /**
     * Get agent type
     */
    public AgentType getAgentType() {
        return AgentType.GENERAL;
    }
    
    /**
     * Get resource usage (compatibility)
     */
    public String getResourceUsage() {
        return "Unified agent usage: " + unifiedAgent.getAgentId();
    }
    
    /**
     * Get unified agent for new architecture access
     */
    public UnifiedAgent getUnifiedAgent() {
        return unifiedAgent;
    }
    
    @Override
    public void abort() {
        super.abort();
        unifiedAgent.abort();
    }
}