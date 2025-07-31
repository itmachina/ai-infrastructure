package com.ai.infrastructure.steering;

import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.tools.ToolEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强的MainAgentLoop测试类
 */
@DisplayName("Enhanced MainAgentLoop Test")
public class EnhancedMainAgentLoopTest {

    @Test
    @DisplayName("测试基本执行循环")
    public void testBasicExecuteLoop() throws Exception {
        System.out.println("Starting testBasicExecuteLoop...");
        
        // 创建必要的组件
        MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent", System.getenv("AI_API_KEY"));
        ToolEngine toolEngine = new ToolEngine();
        MemoryManager memoryManager = new MemoryManager();
        
        // 创建MainAgentLoop
        MainAgentLoop agentLoop = new MainAgentLoop(mainAgent, memoryManager, toolEngine);
        
        // 创建空的消息历史
        List<Object> messages = new ArrayList<>();
        
        // 执行循环
        String prompt = "Calculate 2+2";
        var resultFuture = agentLoop.executeLoop(messages, prompt);
        StreamingResult result = resultFuture.join();
        
        assertNotNull(result);
        assertEquals("assistant", result.getType());
        assertTrue(result.getContent().contains("Tool execution result"));
        
        System.out.println("testBasicExecuteLoop completed!");
    }
    
    @Test
    @DisplayName("测试流式执行循环")
    public void testStreamingExecuteLoop() throws Exception {
        System.out.println("Starting testStreamingExecuteLoop...");
        
        // 创建必要的组件
        MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent", System.getenv("AI_API_KEY"));
        ToolEngine toolEngine = new ToolEngine();
        MemoryManager memoryManager = new MemoryManager();
        
        // 创建MainAgentLoop
        MainAgentLoop agentLoop = new MainAgentLoop(mainAgent, memoryManager, toolEngine);
        
        // 创建空的消息历史
        List<Object> messages = new ArrayList<>();
        
        // 执行流式循环
        String prompt = "Hello, World!";
        Iterator<StreamingResult> results = agentLoop.executeStreamingLoop(messages, prompt);
        
        // 收集所有结果
        List<StreamingResult> resultList = new ArrayList<>();
        while (results.hasNext()) {
            resultList.add(results.next());
        }
        
        // 验证结果
        assertTrue(resultList.size() >= 2); // 至少应该有开始标记和结果
        assertEquals("stream_start", resultList.get(0).getType());
        assertEquals("Stream request started", resultList.get(0).getContent());
        
        StreamingResult lastResult = resultList.get(resultList.size() - 1);
        assertEquals("assistant", lastResult.getType());
        assertTrue(lastResult.getContent().contains("Tool execution result") || 
                   lastResult.getContent().contains("Unknown tool"));
        
        System.out.println("testStreamingExecuteLoop completed!");
    }
    
    @Test
    @DisplayName("测试包含压缩的消息处理")
    public void testMessageCompression() throws Exception {
        System.out.println("Starting testMessageCompression...");
        
        // 创建必要的组件
        MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent", System.getenv("AI_API_KEY"));
        ToolEngine toolEngine = new ToolEngine();
        MemoryManager memoryManager = new MemoryManager();
        
        // 创建MainAgentLoop
        MainAgentLoop agentLoop = new MainAgentLoop(mainAgent, memoryManager, toolEngine);
        
        // 创建大量消息历史以触发压缩
        List<Object> messages = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            messages.add(new StreamingResult("user", "Message " + i + " with some content to increase token count"));
        }
        
        // 执行流式循环
        String prompt = "Summarize the conversation";
        Iterator<StreamingResult> results = agentLoop.executeStreamingLoop(messages, prompt);
        
        // 收集所有结果
        List<StreamingResult> resultList = new ArrayList<>();
        while (results.hasNext()) {
            resultList.add(results.next());
        }
        
        // 验证结果中包含压缩信息
        boolean foundCompaction = false;
        for (StreamingResult result : resultList) {
            if ("compaction".equals(result.getType())) {
                foundCompaction = true;
                assertTrue(result.getContent().contains("Compacted"));
                break;
            }
        }
        
        assertTrue(foundCompaction, "Should find compaction event in results");
        
        System.out.println("testMessageCompression completed!");
    }
    
    @Test
    @DisplayName("测试中断处理")
    public void testAbortHandling() throws Exception {
        System.out.println("Starting testAbortHandling...");
        
        // 创建必要的组件
        MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent", System.getenv("AI_API_KEY"));
        ToolEngine toolEngine = new ToolEngine();
        MemoryManager memoryManager = new MemoryManager();
        
        // 创建MainAgentLoop
        MainAgentLoop agentLoop = new MainAgentLoop(mainAgent, memoryManager, toolEngine);
        
        // 立即中断
        agentLoop.abort();
        
        // 创建空的消息历史
        List<Object> messages = new ArrayList<>();
        
        // 执行循环
        String prompt = "This should be aborted";
        var resultFuture = agentLoop.executeLoop(messages, prompt);
        StreamingResult result = resultFuture.join();
        
        assertNotNull(result);
        assertTrue("error".equals(result.getType()) || result.getContent().contains("aborted"));
        
        System.out.println("testAbortHandling completed!");
    }
}