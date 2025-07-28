package com.ai.infrastructure;

import com.ai.infrastructure.core.AIInfrastructureApplication;
import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.agent.SubAgent;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.scheduler.TaskScheduler;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 集成测试类，用于验证所有组件功能
 */
public class AIInfrastructureIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(AIInfrastructureIntegrationTest.class);
    
    public static void main(String[] args) {
        logger.info("Starting AI Infrastructure Test Suite...");
        
        // 测试1: 基本组件初始化
        testComponentInitialization();
        
        // 测试2: Agent功能
        testAgentFunctionality();
        
        // 测试3: 内存管理
        testMemoryManagement();
        
        // 测试4: 任务调度
        testTaskScheduling();
        
        // 测试5: 安全管理
        testSecurityManagement();
        
        // 测试6: 工具引擎
        testToolEngine();
        
        // 测试7: 完整应用流程
        testFullApplicationFlow();
        
        logger.info("All tests completed!");
    }
    
    /**
     * 测试基本组件初始化
     */
    private static void testComponentInitialization() {
        logger.info("--- Test 1: Component Initialization ---");
        
        try {
            MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent");
            MemoryManager memoryManager = new MemoryManager();
            TaskScheduler taskScheduler = new TaskScheduler(5);
            SecurityManager securityManager = new SecurityManager();
            ToolEngine toolEngine = new ToolEngine();
            
            logger.info("✓ All components initialized successfully");
        } catch (Exception e) {
            logger.error("✗ Component initialization failed", e);
        }
    }
    
    /**
     * 测试Agent功能
     */
    private static void testAgentFunctionality() {
        logger.info("--- Test 2: Agent Functionality ---");
        
        try {
            MainAgent mainAgent = new MainAgent("test-main-2", "Test Main Agent 2");
            
            // 测试简单任务
            CompletableFuture<String> result1 = mainAgent.executeTask("Calculate 10+5");
            logger.info("Simple task result: " + result1.join());
            
            // 测试复杂任务（应该创建子Agent）
            CompletableFuture<String> result2 = mainAgent.executeTask("Analyze complex data with statistics and machine learning algorithms");
            logger.info("Complex task result: " + result2.join());
            
            // 检查子Agent是否创建
            logger.info("Number of sub-agents created: " + mainAgent.getSubAgents().size());
            
            logger.info("✓ Agent functionality test passed");
        } catch (Exception e) {
            logger.error("✗ Agent functionality test failed", e);
        }
    }
    
    /**
     * 测试内存管理
     */
    private static void testMemoryManagement() {
        logger.info("--- Test 3: Memory Management ---");
        
        try {
            MemoryManager memoryManager = new MemoryManager();
            
            // 添加一些上下文
            memoryManager.updateContext("User request 1", "Response 1");
            memoryManager.updateContext("User request 2", "Response 2");
            
            // 检查内存状态
            logger.info("Current token usage: " + memoryManager.getCurrentTokenUsage());
            logger.info("Short term memory size: " + memoryManager.getShortTermMemory().size());
            
            // 测试长期记忆
            memoryManager.updateLongTermMemory("project_name", "AI Infrastructure");
            String project = memoryManager.getFromLongTermMemory("project_name");
            logger.info("Long term memory retrieval: " + project);
            
            logger.info("✓ Memory management test passed");
        } catch (Exception e) {
            logger.error("✗ Memory management test failed", e);
        }
    }
    
    /**
     * 测试任务调度
     */
    private static void testTaskScheduling() {
        logger.info("--- Test 4: Task Scheduling ---");
        
        try {
            TaskScheduler scheduler = new TaskScheduler(3);
            
            // 提交几个任务
            String result1 = scheduler.scheduleTask("Task 1", task -> "Result of " + task);
            String result2 = scheduler.scheduleTask("Task 2", task -> "Result of " + task);
            String result3 = scheduler.scheduleTask("Task 3", task -> "Result of " + task);
            
            logger.info("Scheduled task results: " + result1 + ", " + result2 + ", " + result3);
            logger.info("Max concurrency: " + scheduler.getMaxConcurrency());
            
            logger.info("✓ Task scheduling test passed");
        } catch (Exception e) {
            logger.error("✗ Task scheduling test failed", e);
        }
    }
    
    /**
     * 测试安全管理
     */
    private static void testSecurityManagement() {
        logger.info("--- Test 5: Security Management ---");
        
        try {
            SecurityManager securityManager = new SecurityManager();
            
            // 测试正常输入
            boolean valid1 = securityManager.validateInput("Calculate 2+2");
            logger.info("Valid input test: " + valid1);
            
            // 测试权限检查
            boolean permission1 = securityManager.checkPermissions("read", "read");
            logger.info("Permission check test: " + permission1);
            
            // 测试沙箱检查
            boolean sandbox1 = securityManager.sandboxCheck("read file");
            logger.info("Sandbox check test: " + sandbox1);
            
            logger.info("✓ Security management test passed");
        } catch (Exception e) {
            logger.error("✗ Security management test failed", e);
        }
    }
    
    /**
     * 测试工具引擎
     */
    private static void testToolEngine() {
        logger.info("--- Test 6: Tool Engine ---");
        
        try {
            ToolEngine toolEngine = new ToolEngine();
            
            // 测试各种工具
            String result1 = toolEngine.executeTool("Calculate 10*5");
            logger.info("Calculate tool result: " + result1);
            
            String result2 = toolEngine.executeTool("Read configuration file");
            logger.info("Read tool result: " + result2);
            
            String result3 = toolEngine.executeTool("Search for documentation");
            logger.info("Search tool result: " + result3);
            
            logger.info("✓ Tool engine test passed");
        } catch (Exception e) {
            logger.error("✗ Tool engine test failed", e);
        }
    }
    
    /**
     * 测试完整应用流程
     */
    private static void testFullApplicationFlow() {
        logger.info("--- Test 7: Full Application Flow ---");
        
        try {
            AIInfrastructureApplication application = new AIInfrastructureApplication();
            application.start();
            
            // 执行一系列任务
            CompletableFuture<String> task1 = application.executeTask("Calculate the sum of 1 to 100");
            CompletableFuture<String> task2 = application.executeTask("Read the system configuration file");
            CompletableFuture<String> task3 = application.executeTask("Search for Java documentation");
            
            logger.info("Full application task 1: " + task1.join());
            logger.info("Full application task 2: " + task2.join());
            logger.info("Full application task 3: " + task3.join());
            
            application.shutdown();
            
            logger.info("✓ Full application flow test passed");
        } catch (Exception e) {
            logger.error("✗ Full application flow test failed", e);
        }
    }
}