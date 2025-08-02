package com.ai.infrastructure.agent;

import com.ai.infrastructure.agent.unified.UnifiedMainAgent;
import com.ai.infrastructure.agent.unified.UnifiedAgentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 新架构的MainAgent适配器
 * 保持原有MainAgent的API兼容性，内部使用统一架构
 */
public class MainAgent extends BaseAgent {
    private static final Logger logger = LoggerFactory.getLogger(MainAgent.class);
    
    private final UnifiedMainAgent unifiedMainAgent;
    
    public MainAgent(String agentId, String name, String apiKey) {
        super(agentId, name);
        
        // 使用统一架构
        UnifiedAgentContext context = UnifiedAgentContext.getInstance();
        this.unifiedMainAgent = new UnifiedMainAgent(agentId, name, context);
        
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
