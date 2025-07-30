package com.ai.infrastructure.scheduler;

import com.ai.infrastructure.agent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AgentTest {
    private static final Logger logger = LoggerFactory.getLogger(AgentTest.class);
    
    public static void main(String[] args) {
        logger.info("=== Agent Test Started ===");
        
        try {
            // Test agent creation
            testAgentCreation();
            
            // Test agent pool initialization
            testAgentPoolInitialization();
            
            // Test agent capabilities
            testAgentCapabilities();
            
            logger.info("=== Agent Test Completed Successfully ===");
            
        } catch (Exception e) {
            logger.error("=== Agent Test Failed ===", e);
            e.printStackTrace();
        }
    }
    
    private static void testAgentCreation() {
        logger.info("Testing agent creation...");
        
        // Create test agents
        InteractionAgent i2aAgent = new InteractionAgent("test_i2a", "Test I2A Agent");
        UserProcessingAgent uh1Agent = new UserProcessingAgent("test_uh1", "Test UH1 Agent");
        KnowledgeProcessingAgent kn5Agent = new KnowledgeProcessingAgent("test_kn5", "Test KN5 Agent");
        
        // Verify agent properties
        logger.info("Created I2A Agent: {}, Status: {}, CanAcceptTask: {}", 
                   i2aAgent.getAgentId(), i2aAgent.getStatus(), i2aAgent.canAcceptTask());
        
        logger.info("Created UH1 Agent: {}, Status: {}, CanAcceptTask: {}", 
                   uh1Agent.getAgentId(), uh1Agent.getStatus(), uh1Agent.canAcceptTask());
        
        logger.info("Created KN5 Agent: {}, Status: {}, CanAcceptTask: {}", 
                   kn5Agent.getAgentId(), kn5Agent.getStatus(), kn5Agent.canAcceptTask());
        
        // Test task execution
        testTaskExecution(i2aAgent, "处理用户交互界面");
        testTaskExecution(uh1Agent, "解析用户输入数据");
        testTaskExecution(kn5Agent, "分析知识推理任务");
    }
    
    private static void testAgentPoolInitialization() {
        logger.info("Testing agent pool initialization...");
        
        Map<AgentType, List<SpecializedAgent>> agentPools = new HashMap<>();
        
        // I2A Agent pool
        List<SpecializedAgent> i2aAgents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            i2aAgents.add(new InteractionAgent("i2a_" + i, "I2A Interaction Agent-" + i));
        }
        agentPools.put(AgentType.I2A, i2aAgents);
        
        // UH1 Agent pool
        List<SpecializedAgent> uh1Agents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            uh1Agents.add(new UserProcessingAgent("uh1_" + i, "UH1 User Processing Agent-" + i));
        }
        agentPools.put(AgentType.UH1, uh1Agents);
        
        // KN5 Agent pool
        List<SpecializedAgent> kn5Agents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            kn5Agents.add(new KnowledgeProcessingAgent("kn5_" + i, "KN5 Knowledge Processing Agent-" + i));
        }
        agentPools.put(AgentType.KN5, kn5Agents);
        
        // Log agent pool status
        agentPools.forEach((type, agents) -> {
            logger.info("Agent Type: {}, Agents: {}", type.getDisplayName(), agents.size());
            agents.forEach(agent -> {
                logger.info("  Agent: {}, Status: {}, CanAcceptTask: {}", 
                           agent.getAgentId(), agent.getStatus(), agent.canAcceptTask());
            });
        });
    }
    
    private static void testAgentCapabilities() {
        logger.info("Testing agent capabilities...");
        
        InteractionAgent i2aAgent = new InteractionAgent("test_i2a", "Test I2A Agent");
        UserProcessingAgent uh1Agent = new UserProcessingAgent("test_uh1", "Test UH1 Agent");
        KnowledgeProcessingAgent kn5Agent = new KnowledgeProcessingAgent("test_kn5", "Test KN5 Agent");
        
        // Test support task types
        String[] testTasks = {
            "用户交互界面",
            "解析用户输入",
            "知识推理分析",
            "数据处理",
            "可视化展示"
        };
        
        Arrays.stream(testTasks).forEach(task -> {
            logger.info("Task: '{}'", task);
            logger.info("  I2A supports: {}", i2aAgent.supportsTaskType(task));
            logger.info("  UH1 supports: {}", uh1Agent.supportsTaskType(task));
            logger.info("  KN5 supports: {}", kn5Agent.supportsTaskType(task));
        });
    }
    
    private static void testTaskExecution(SpecializedAgent agent, String task) {
        logger.info("Testing task execution for {} with task: {}", agent.getAgentId(), task);
        
        try {
            CompletableFuture<String> future = agent.executeTask(task, IntelligentTaskDecomposer.TaskPriority.MEDIUM);
            String result = future.get(10, java.util.concurrent.TimeUnit.SECONDS);
            logger.info("Task execution result: {}", result.substring(0, Math.min(result.length(), 100)) + "...");
        } catch (Exception e) {
            logger.error("Task execution failed: {}", e.getMessage());
        }
    }
}