package com.ai.infrastructure.scheduler;

import com.ai.infrastructure.agent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LoadBalancerTest {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerTest.class);
    
    public static void main(String[] args) {
        logger.info("=== Load Balancer Test Started ===");
        
        try {
            // Test 1: Create agent pools
            testAgentPoolsCreation();
            
            // Test 2: Create load balancer
            testLoadBalancerCreation();
            
            // Test 3: Schedule a simple task
            testTaskScheduling();
            
            logger.info("=== Load Balancer Test Completed Successfully ===");
            
        } catch (Exception e) {
            logger.error("=== Load Balancer Test Failed ===", e);
            e.printStackTrace();
        }
    }
    
    private static void testAgentPoolsCreation() {
        logger.info("Testing agent pools creation...");
        
        Map<AgentType, List<SpecializedAgent>> agentPools = new HashMap<>();
        
        // I2A Agent pool
        List<SpecializedAgent> i2aAgents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            i2aAgents.add(new InteractionAgent("i2a_" + i, "I2A Interaction Agent-" + i));
        }
        agentPools.put(AgentType.I2A, i2aAgents);
        logger.info("Created I2A agents: {}", i2aAgents.size());
        
        // UH1 Agent pool
        List<SpecializedAgent> uh1Agents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            uh1Agents.add(new UserProcessingAgent("uh1_" + i, "UH1 User Processing Agent-" + i));
        }
        agentPools.put(AgentType.UH1, uh1Agents);
        logger.info("Created UH1 agents: {}", uh1Agents.size());
        
        // KN5 Agent pool
        List<SpecializedAgent> kn5Agents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            kn5Agents.add(new KnowledgeProcessingAgent("kn5_" + i, "KN5 Knowledge Processing Agent-" + i));
        }
        agentPools.put(AgentType.KN5, kn5Agents);
        logger.info("Created KN5 agents: {}", kn5Agents.size());
        
        // Test agent availability
        agentPools.forEach((type, agents) -> {
            logger.info("Agent Type: {}, Total Agents: {}", type.getDisplayName(), agents.size());
            agents.forEach(agent -> {
                boolean canAccept = agent.canAcceptTask();
                logger.info("  Agent: {}, Status: {}, CanAccept: {}", 
                           agent.getAgentId(), agent.getStatus(), canAccept);
            });
        });
    }
    
    private static void testLoadBalancerCreation() {
        logger.info("Testing load balancer creation...");
        
        Map<AgentType, List<SpecializedAgent>> agentPools = createTestAgentPools();
        
        // Create load balancer with short timeout for testing and API key
        String testApiKey = System.getenv("AI_API_KEY");
        IntelligentLoadBalancer loadBalancer = new IntelligentLoadBalancer(
            agentPools, 
            5,  // maxConcurrency
            5000,  // 5 second timeout
            2,   // maxRetryCount
            testApiKey
        );
        
        logger.info("Load balancer created successfully");
        logger.info("Load balancer status:\n{}", loadBalancer.getSchedulerStatus());
        
        // Schedule a simple task
        CompletableFuture<String> future = loadBalancer.scheduleTask(
            "处理简单的用户输入", 
            IntelligentTaskDecomposer.TaskPriority.MEDIUM
        );
        
        try {
            String result = future.get(10, TimeUnit.SECONDS);
            logger.info("Task completed successfully!");
            logger.info("Result: {}", result);
        } catch (Exception e) {
            logger.error("Task execution failed: {}", e.getMessage());
        }
        
        // Shutdown load balancer
        loadBalancer.shutdown();
        logger.info("Load balancer shutdown completed");
    }
    
    private static void testTaskScheduling() {
        logger.info("Testing task scheduling...");
        
        Map<AgentType, List<SpecializedAgent>> agentPools = createTestAgentPools();
        String testApiKey = System.getenv("AI_API_KEY");
        IntelligentLoadBalancer loadBalancer = new IntelligentLoadBalancer(
            agentPools, 5, 5000, 2, testApiKey
        );
        
        // Test multiple tasks
        String[] testTasks = {
            "计算2+2的结果",
            "解析用户输入数据", 
            "生成简单的文本响应"
        };
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        for (String task : testTasks) {
            logger.info("Scheduling task: {}", task);
            CompletableFuture<String> future = loadBalancer.scheduleTask(
                task, 
                IntelligentTaskDecomposer.TaskPriority.MEDIUM
            );
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(15, TimeUnit.SECONDS);
            logger.info("All tasks completed successfully!");
            
            // Print results
            for (int i = 0; i < futures.size(); i++) {
                String result = futures.get(i).join();
                logger.info("Task {} result: {}", i + 1, result.substring(0, Math.min(result.length(), 100)) + "...");
            }
            
        } catch (Exception e) {
            logger.error("Task scheduling failed: {}", e.getMessage());
        }
        
        loadBalancer.shutdown();
    }
    
    private static Map<AgentType, List<SpecializedAgent>> createTestAgentPools() {
        Map<AgentType, List<SpecializedAgent>> agentPools = new HashMap<>();
        
        // I2A Agent pool
        List<SpecializedAgent> i2aAgents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            i2aAgents.add(new InteractionAgent("i2a_" + i, "I2A Agent-" + i));
        }
        agentPools.put(AgentType.I2A, i2aAgents);
        
        // UH1 Agent pool
        List<SpecializedAgent> uh1Agents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            uh1Agents.add(new UserProcessingAgent("uh1_" + i, "UH1 Agent-" + i));
        }
        agentPools.put(AgentType.UH1, uh1Agents);
        
        // KN5 Agent pool
        List<SpecializedAgent> kn5Agents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            kn5Agents.add(new KnowledgeProcessingAgent("kn5_" + i, "KN5 Agent-" + i));
        }
        agentPools.put(AgentType.KN5, kn5Agents);
        
        return agentPools;
    }
}