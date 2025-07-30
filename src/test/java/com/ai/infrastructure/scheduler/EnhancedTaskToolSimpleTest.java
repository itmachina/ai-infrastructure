package com.ai.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ai.infrastructure.agent.*;

/**
 * Simple test to verify EnhancedTaskTool functionality
 */
public class EnhancedTaskToolSimpleTest {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedTaskToolSimpleTest.class);
    
    @Test
    public void testEnhancedTaskToolCreation() {
        logger.info("Testing EnhancedTaskTool creation...");
        
        // Test that EnhancedTaskTool can be created without errors
        assertDoesNotThrow(() -> {
            EnhancedTaskTool taskTool = new EnhancedTaskTool();
            assertNotNull(taskTool);
            logger.info("EnhancedTaskTool created successfully");
        });
    }
    
    @Test
    public void testSimpleTaskExecution() {
        logger.info("Testing simple task execution...");
        
        EnhancedTaskTool taskTool = new EnhancedTaskTool();
        
        // Test synchronous task execution
        String result = taskTool.executeTaskSync("Calculate 2+2");
        assertNotNull(result);
        assertTrue(result.length() > 0);
        logger.info("Task execution result: {}", result.substring(0, Math.min(result.length(), 100)));
        
        // Clean up
        taskTool.shutdown();
    }
    
    @Test
    public void testTaskComplexityAnalysis() {
        logger.info("Testing task complexity analysis...");
        
        EnhancedTaskTool taskTool = new EnhancedTaskTool();
        
        // Test complexity analysis
        double complexity = taskTool.analyzeTaskComplexity("Analyze complex system architecture");
        assertTrue(complexity >= 0.0);
        assertTrue(complexity <= 1.0);
        logger.info("Task complexity: {}", complexity);
        
        // Test duration estimation
        long duration = taskTool.estimateTaskDuration("Analyze complex system architecture");
        assertTrue(duration > 0);
        logger.info("Estimated duration: {}ms", duration);
        
        // Clean up
        taskTool.shutdown();
    }
    
    @Test
    public void testSystemStatus() {
        logger.info("Testing system status retrieval...");
        
        EnhancedTaskTool taskTool = new EnhancedTaskTool();
        
        // Test system status
        String status = taskTool.getSystemStatus();
        assertNotNull(status);
        assertTrue(status.contains("Enhanced Task Tool System Status"));
        logger.info("System status retrieved successfully");
        
        // Test performance metrics
        var metrics = taskTool.getPerformanceMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("totalAgents"));
        logger.info("Performance metrics retrieved successfully");
        
        // Clean up
        taskTool.shutdown();
    }
    
    @Test
    public void testAgentFunctionality() {
        logger.info("Testing Agent functionality...");
        
        // Test that agents can be created and perform basic operations
        assertDoesNotThrow(() -> {
            var i2aAgent = new InteractionAgent("test_i2a", "Test I2A Agent");
            var uh1Agent = new UserProcessingAgent("test_uh1", "Test UH1 Agent");
            var kn5Agent = new KnowledgeProcessingAgent("test_kn5", "Test KN5 Agent");
            
            // Test basic agent properties
            assertNotNull(i2aAgent.getAgentType());
            assertNotNull(uh1Agent.getAgentType());
            assertNotNull(kn5Agent.getAgentType());
            
            // Test agent status
            assertNotNull(i2aAgent.getStatus());
            assertNotNull(uh1Agent.getStatus());
            assertNotNull(kn5Agent.getStatus());
            
            logger.info("All agents created and basic properties tested successfully");
        });
    }
    
    @Test
    public void testTaskDecomposer() {
        logger.info("Testing IntelligentTaskDecomposer...");
        
        IntelligentTaskDecomposer decomposer = new IntelligentTaskDecomposer();
        
        // Test task decomposition
        var request = new IntelligentTaskDecomposer.TaskDecompositionRequest(
            "test_task",
            "Analyze system architecture and propose optimization suggestions",
            IntelligentTaskDecomposer.TaskPriority.MEDIUM,
            java.util.Optional.empty()
        );
        
        var result = decomposer.decomposeTask(request);
        assertNotNull(result);
        assertNotNull(result.getTaskId());
        assertNotNull(result.getSteps());
        assertTrue(result.getSteps().size() > 0);
        
        logger.info("Task decomposition successful - {} steps created", result.getSteps().size());
    }
    
    @Test
    public void testLoadBalancer() {
        logger.info("Testing IntelligentLoadBalancer...");
        
        // Create agent pool
        var agentPools = new java.util.HashMap<com.ai.infrastructure.agent.AgentType, java.util.List<com.ai.infrastructure.agent.SpecializedAgent>>();
        
        // Add I2A agents
        var i2aAgents = new java.util.ArrayList<com.ai.infrastructure.agent.SpecializedAgent>();
        i2aAgents.add(new InteractionAgent("i2a_1", "Test I2A Agent"));
        agentPools.put(com.ai.infrastructure.agent.AgentType.I2A, i2aAgents);
        
        // Add UH1 agents
        var uh1Agents = new java.util.ArrayList<com.ai.infrastructure.agent.SpecializedAgent>();
        uh1Agents.add(new UserProcessingAgent("uh1_1", "Test UH1 Agent"));
        agentPools.put(com.ai.infrastructure.agent.AgentType.UH1, uh1Agents);
        
        // Create load balancer
        assertDoesNotThrow(() -> {
            String testApiKey = "test-api-key-for-enhanced-tool";
            var loadBalancer = new IntelligentLoadBalancer(agentPools, 5, 30000, 3, testApiKey);
            assertNotNull(loadBalancer);
            logger.info("IntelligentLoadBalancer created successfully");
            
            // Clean up
            loadBalancer.shutdown();
        });
    }
}