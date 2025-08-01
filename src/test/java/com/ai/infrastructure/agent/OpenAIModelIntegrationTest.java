package com.ai.infrastructure.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI风格大模型集成测试
 * 注意：此测试需要有效的API密钥才能运行
 */
public class OpenAIModelIntegrationTest {
    
    private MainAgent mainAgent;
    
    @BeforeEach
    void setUp() {
        mainAgent = new MainAgent("test-main-agent", "Test Main Agent", System.getenv("AI_API_KEY"));
    }
    
    @Test
    @Disabled("需要有效的API密钥才能运行此测试")
    void testAgentTaskExecutionWithModel() {
        
        // 测试Agent任务执行
        String result = mainAgent.executeTask("请用一句话介绍人工智能").join();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.length() > 10);
        System.out.println("Task execution result: " + result);
    }
    
    @Test
    void testAgentTaskExecutionWithoutModel() {
        // 测试没有API密钥时，Agent应该回退到工具引擎
        String result = mainAgent.executeTask("计算2+2等于多少").join();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Fallback task execution result: " + result);
    }
}