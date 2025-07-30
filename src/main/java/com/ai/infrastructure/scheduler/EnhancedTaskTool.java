package com.ai.infrastructure.scheduler;

import com.ai.infrastructure.agent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

/**
 * Enhanced Task Tool Main Controller
 * Based on Claude Code layered multi-Agent architecture for intelligent task scheduling and execution
 */
public class EnhancedTaskTool {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedTaskTool.class);
    
    // Core components
    private final IntelligentTaskDecomposer taskDecomposer;
    private final MultiAgentCoordinator multiAgentCoordinator;
    private final IntelligentLoadBalancer loadBalancer;
    
    // Agent pool
    private final Map<com.ai.infrastructure.agent.AgentType, List<SpecializedAgent>> agentPools;
    
    // Configuration parameters
    private final int maxConcurrency;
    private final long taskTimeout;
    private final int maxRetryCount;
    
    public EnhancedTaskTool() {
        this(10, 30000, 3, null);
    }
    
    public EnhancedTaskTool(int maxConcurrency, long taskTimeout, int maxRetryCount) {
        this(maxConcurrency, taskTimeout, maxRetryCount, null);
    }
    
    public EnhancedTaskTool(int maxConcurrency, long taskTimeout, int maxRetryCount, String apiKey) {
        this.maxConcurrency = maxConcurrency;
        this.taskTimeout = taskTimeout;
        this.maxRetryCount = maxRetryCount;
        
        // Initialize core components
        this.taskDecomposer = new IntelligentTaskDecomposer();
        this.multiAgentCoordinator = new MultiAgentCoordinator();
        
        // Initialize Agent pool
        this.agentPools = initializeAgentPools();
        
        // Initialize load balancer with API key
        this.loadBalancer = new IntelligentLoadBalancer(agentPools, maxConcurrency, taskTimeout, maxRetryCount, apiKey);
        
        logger.info("Enhanced Task Tool initialized with maxConcurrency: {}, timeout: {}ms, retries: {}", 
                   maxConcurrency, taskTimeout, maxRetryCount);
    }
    
    /**
     * Initialize Agent pool
     */
    private Map<com.ai.infrastructure.agent.AgentType, List<SpecializedAgent>> initializeAgentPools() {
        Map<com.ai.infrastructure.agent.AgentType, List<SpecializedAgent>> pools = new HashMap<>();
        
        // I2A Agent pool
        List<SpecializedAgent> i2aAgents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            i2aAgents.add(new InteractionAgent("i2a_" + i, "I2A Interaction Agent-" + i));
        }
        pools.put(com.ai.infrastructure.agent.AgentType.I2A, i2aAgents);
        
        // UH1 Agent pool
        List<SpecializedAgent> uh1Agents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            uh1Agents.add(new UserProcessingAgent("uh1_" + i, "UH1 User Processing Agent-" + i));
        }
        pools.put(com.ai.infrastructure.agent.AgentType.UH1, uh1Agents);
        
        // KN5 Agent pool
        List<SpecializedAgent> kn5Agents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            kn5Agents.add(new KnowledgeProcessingAgent("kn5_" + i, "KN5 Knowledge Processing Agent-" + i));
        }
        pools.put(com.ai.infrastructure.agent.AgentType.KN5, kn5Agents);
        
        return pools;
    }
    
    /**
     * Execute task (main interface)
     */
    public CompletableFuture<String> executeTask(String task, IntelligentTaskDecomposer.TaskPriority priority) {
        logger.info("Executing task with priority {}: {}", priority, task);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Task decomposition
                IntelligentTaskDecomposer.TaskDecompositionRequest request = 
                    new IntelligentTaskDecomposer.TaskDecompositionRequest(
                        "task_" + System.currentTimeMillis(),
                        task,
                        priority,
                        Optional.empty()
                    );
                
                IntelligentTaskDecomposer.TaskDecompositionResult decomposition = 
                    taskDecomposer.decomposeTask(request);
                
                logger.debug("Task decomposition completed - complexity: {}, steps: {}", 
                           decomposition.getComplexity(), decomposition.getSteps().size());
                
                // 2. Select execution strategy based on complexity
                if (decomposition.getComplexity() > 0.6) {
                    // Complex task uses multi-Agent coordination
                    return executeComplexTask(decomposition);
                } else {
                    // Simple task uses load balancing
                    return executeSimpleTask(task, priority);
                }
                
            } catch (Exception e) {
                logger.error("Task execution failed", e);
                return "Task execution failed: " + e.getMessage();
            }
        });
    }
    
    /**
     * Execute complex task (multi-Agent coordination)
     */
    private String executeComplexTask(IntelligentTaskDecomposer.TaskDecompositionResult decomposition) {
        try {
            // Use multi-Agent coordinator to execute
            CompletableFuture<String> coordinationFuture = 
                multiAgentCoordinator.coordinateTaskExecution(decomposition.getTaskId(), 
                    IntelligentTaskDecomposer.TaskPriority.HIGH);
            
            return coordinationFuture.get(60, java.util.concurrent.TimeUnit.SECONDS);
            
        } catch (Exception e) {
            logger.error("Complex task execution failed", e);
            return "Complex task execution failed: " + e.getMessage();
        }
    }
    
    /**
     * Execute simple task (load balancing)
     */
    private String executeSimpleTask(String task, IntelligentTaskDecomposer.TaskPriority priority) {
        try {
            CompletableFuture<String> loadBalanceFuture = 
                loadBalancer.scheduleTask(task, priority);
            
            return loadBalanceFuture.get(taskTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            logger.error("Simple task execution failed", e);
            return "Simple task execution failed: " + e.getMessage();
        }
    }
    
    /**
     * Execute task (simplified interface)
     */
    public String executeTaskSync(String task) {
        return executeTaskSync(task, IntelligentTaskDecomposer.TaskPriority.MEDIUM);
    }
    
    /**
     * Synchronous task execution
     */
    public String executeTaskSync(String task, IntelligentTaskDecomposer.TaskPriority priority) {
        try {
            CompletableFuture<String> future = executeTask(task, priority);
            return future.get(taskTimeout + 10000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Synchronous task execution failed", e);
            return "Synchronous task execution failed: " + e.getMessage();
        }
    }
    
    /**
     * Batch execute tasks
     */
    public CompletableFuture<List<String>> executeBatchTasks(List<String> tasks, IntelligentTaskDecomposer.TaskPriority priority) {
        logger.info("Executing batch tasks: {} tasks", tasks.size());
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (String task : tasks) {
            futures.add(executeTask(task, priority));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()));
    }
    
    /**
     * Get system status
     */
    public String getSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== Enhanced Task Tool System Status ===\n\n");
        
        // Load balancer status
        status.append(loadBalancer.getSchedulerStatus()).append("\n");
        
        // Agent pool status
        status.append(multiAgentCoordinator.getAgentPoolStatus()).append("\n");
        
        // Agent load details
        status.append(loadBalancer.getAgentLoadDetails()).append("\n");
        
        // Coordination metrics
        status.append(multiAgentCoordinator.getCoordinationMetrics());
        
        return status.toString();
    }
    
    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Agent pool statistics
        metrics.put("agentPoolStatistics", multiAgentCoordinator.getAgentPoolStatistics());
        
        // System load
        metrics.put("systemLoad", loadBalancer.getSchedulerStatus());
        
        // Task statistics
        metrics.put("totalAgents", agentPools.values().stream().mapToInt(List::size).sum());
        metrics.put("maxConcurrency", maxConcurrency);
        metrics.put("taskTimeout", taskTimeout);
        metrics.put("maxRetryCount", maxRetryCount);
        
        return metrics;
    }
    
    /**
     * Analyze task complexity
     */
    public double analyzeTaskComplexity(String task) {
        IntelligentTaskDecomposer.TaskDecompositionRequest request = 
            new IntelligentTaskDecomposer.TaskDecompositionRequest(
                "analysis_" + System.currentTimeMillis(),
                task,
                IntelligentTaskDecomposer.TaskPriority.MEDIUM,
                Optional.empty()
            );
        
        IntelligentTaskDecomposer.TaskDecompositionResult result = 
            taskDecomposer.decomposeTask(request);
        
        return result.getComplexity();
    }
    
    /**
     * Estimate task duration
     */
    public long estimateTaskDuration(String task) {
        IntelligentTaskDecomposer.TaskDecompositionRequest request = 
            new IntelligentTaskDecomposer.TaskDecompositionRequest(
                "estimate_" + System.currentTimeMillis(),
                task,
                IntelligentTaskDecomposer.TaskPriority.MEDIUM,
                Optional.empty()
            );
        
        IntelligentTaskDecomposer.TaskDecompositionResult result = 
            taskDecomposer.decomposeTask(request);
        
        return result.getEstimatedDuration();
    }
    
    /**
     * Reset system
     */
    public void reset() {
        logger.info("Resetting Enhanced Task Tool");
        
        // Reset all agents
        multiAgentCoordinator.resetAllAgents();
        
        // Clean up resources
        agentPools.values().forEach(agents -> agents.forEach(SpecializedAgent::reset));
        
        logger.info("Enhanced Task Tool reset completed");
    }
    
    /**
     * Shutdown system
     */
    public void shutdown() {
        logger.info("Shutting down Enhanced Task Tool");
        
        // Shutdown load balancer
        loadBalancer.shutdown();
        
        // Reset all agents
        reset();
        
        logger.info("Enhanced Task Tool shutdown completed");
    }
    
    /**
     * Demonstrate functionality
     */
    public void demonstrateCapabilities() {
        logger.info("=== Enhanced Task Tool Functionality Demonstration ===");
        
        // Demo different types of tasks
        List<String> demoTasks = Arrays.asList(
            "Analyze user requirements and generate interactive interface prototype",
            "Calculate data analysis results and verify data integrity",
            "Learn new programming language and reason about best practices",
            "Process user input and generate formatted response",
            "Optimize system performance and generate improvement report"
        );
        
        System.out.println("Executing demo tasks...");
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (String task : demoTasks) {
            IntelligentTaskDecomposer.TaskPriority priority = 
                IntelligentTaskDecomposer.TaskPriority.MEDIUM;
            
            CompletableFuture<String> future = executeTask(task, priority)
                .thenApply(result -> {
                    System.out.println("Task completed: " + task.substring(0, Math.min(task.length(), 30)) + "...");
                    return result;
                });
            
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        
        try {
            allFutures.get(60, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("All demo tasks completed!");
            
            // Display system status
            System.out.println("\n" + getSystemStatus());
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
            System.out.println("Demo execution failed: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        // Create Enhanced Task Tool instance
        EnhancedTaskTool taskTool = new EnhancedTaskTool();
        
        try {
            // Run demonstration
            taskTool.demonstrateCapabilities();
            
            // Test single task
            System.out.println("\n=== Single Task Execution Test ===");
            String result = taskTool.executeTaskSync("Analyze system architecture and propose optimization suggestions");
            System.out.println("Task execution result: " + result.substring(0, Math.min(result.length(), 200)) + "...");
            
        } catch (Exception e) {
            logger.error("Main execution failed", e);
        } finally {
            // Shutdown system
            taskTool.shutdown();
        }
    }
}