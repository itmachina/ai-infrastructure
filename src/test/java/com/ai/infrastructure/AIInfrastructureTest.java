package com.ai.infrastructure;

import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.scheduler.TaskScheduler;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AI基础设施组件测试类
 */
@DisplayName("AI Infrastructure Components Test")
public class AIInfrastructureTest {

    @Test
    @DisplayName("测试组件初始化")
    public void testComponentInitialization() {
        assertDoesNotThrow(() -> {
            MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent");
            MemoryManager memoryManager = new MemoryManager();
            TaskScheduler taskScheduler = new TaskScheduler(5);
            SecurityManager securityManager = new SecurityManager();
            ToolEngine toolEngine = new ToolEngine();
        }, "所有组件应该正常初始化");
    }

    @Test
    @DisplayName("测试内存管理基本功能")
    public void testMemoryManagementBasic() {
        MemoryManager memoryManager = new MemoryManager();
        
        // 测试初始状态
        assertEquals(0, memoryManager.getCurrentTokenUsage(), "初始Token使用量应该为0");
        assertNotNull(memoryManager.getShortTermMemory(), "短期记忆不应该为null");
        assertNotNull(memoryManager.getMediumTermMemory(), "中期记忆不应该为null");
        
        // 测试更新上下文
        assertDoesNotThrow(() -> {
            memoryManager.updateContext("test input", "test output");
        }, "更新上下文不应该抛出异常");
    }

    @Test
    @DisplayName("测试任务调度器")
    public void testTaskScheduler() {
        TaskScheduler scheduler = new TaskScheduler(3);
        
        assertEquals(3, scheduler.getMaxConcurrency(), "最大并发数应该为3");
        
        // 测试任务调度
        String result = scheduler.scheduleTask("test task", task -> "Result: " + task);
        assertEquals("Result: test task", result, "任务执行结果应该正确");
    }

    @Test
    @DisplayName("测试安全管理器")
    public void testSecurityManager() {
        SecurityManager securityManager = new SecurityManager();
        
        // 测试输入验证
        assertTrue(securityManager.validateInput("Calculate 2+2"), "正常输入应该通过验证");
        assertTrue(securityManager.validateInput("Read configuration file"), "正常输入应该通过验证");
        
        // 测试权限检查
        assertTrue(securityManager.checkPermissions("read", "read"), "读取权限应该被允许");
    }

    @Test
    @DisplayName("测试工具引擎")
    public void testToolEngine() {
        ToolEngine toolEngine = new ToolEngine();
        
        // 测试工具执行
        String result1 = toolEngine.executeTool("Calculate 10*5");
        assertTrue(result1.contains("Tool execution result"), "计算工具应该返回结果");
        
        String result2 = toolEngine.executeTool("Read configuration file");
        assertTrue(result2.contains("Tool execution result"), "读取工具应该返回结果");
        
        String result3 = toolEngine.executeTool("Search for documentation");
        assertTrue(result3.contains("Tool execution result"), "搜索工具应该返回结果");
    }
}