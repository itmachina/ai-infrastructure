package com.ai.infrastructure.steering;

import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.memory.MemoryManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 流式处理器增强功能测试类
 */
@DisplayName("Enhanced StreamingProcessor Test")
public class EnhancedStreamingProcessorTest {

    @Test
    @DisplayName("测试Prompt命令处理")
    public void testPromptCommandProcessing() throws Exception {
        System.out.println("Starting testPromptCommandProcessing...");
        
        // 创建必要的组件
        MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent");
        ToolEngine toolEngine = new ToolEngine();
        MemoryManager memoryManager = new MemoryManager();
        
        // 创建流式处理器
        StreamingProcessor processor = new StreamingProcessor(mainAgent, toolEngine, memoryManager);
        
        // 发送Prompt命令
        Command command = new Command("prompt", "Calculate 2+2");
        processor.enqueueCommand(command);
        
        // 等待处理完成
        Thread.sleep(1000);
        
        // 验证输出队列中有结果
        CompletableFuture<QueueMessage<Object>> future = processor.getOutputStream().read();
        QueueMessage<Object> message = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(message);
        assertFalse(message.isDone());
        assertTrue(message.getValue() instanceof StreamingResult);
        
        StreamingResult result = (StreamingResult) message.getValue();
        assertTrue(result.getContent().contains("Tool execution result"));
        
        // 清理
        processor.close();
        
        System.out.println("testPromptCommandProcessing completed!");
    }
    
    @Test
    @DisplayName("测试工具命令处理")
    public void testToolCommandProcessing() throws Exception {
        System.out.println("Starting testToolCommandProcessing...");
        
        // 创建必要的组件
        MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent");
        ToolEngine toolEngine = new ToolEngine();
        MemoryManager memoryManager = new MemoryManager();
        
        // 创建流式处理器
        StreamingProcessor processor = new StreamingProcessor(mainAgent, toolEngine, memoryManager);
        
        // 发送工具命令
        Command command = new Command("tool", "Calculate 10*5");
        processor.enqueueCommand(command);
        
        // 等待处理完成
        Thread.sleep(1000);
        
        // 验证输出队列中有结果
        CompletableFuture<QueueMessage<Object>> future = processor.getOutputStream().read();
        QueueMessage<Object> message = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(message);
        assertFalse(message.isDone());
        assertTrue(message.getValue() instanceof StreamingResult);
        
        StreamingResult result = (StreamingResult) message.getValue();
        assertEquals("tool_result", result.getType());
        assertTrue(result.getContent().contains("Tool execution result"));
        
        // 清理
        processor.close();
        
        System.out.println("testToolCommandProcessing completed!");
    }
    
    @Test
    @DisplayName("测试系统命令处理")
    public void testSystemCommandProcessing() throws Exception {
        System.out.println("Starting testSystemCommandProcessing...");
        
        // 创建必要的组件
        MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent");
        ToolEngine toolEngine = new ToolEngine();
        MemoryManager memoryManager = new MemoryManager();
        
        // 创建流式处理器
        StreamingProcessor processor = new StreamingProcessor(mainAgent, toolEngine, memoryManager);
        
        // 发送系统命令 - 内存统计
        Command command1 = new Command("system", "memory-stats");
        processor.enqueueCommand(command1);
        
        // 等待处理完成
        Thread.sleep(500);
        
        // 验证输出队列中有结果
        CompletableFuture<QueueMessage<Object>> future1 = processor.getOutputStream().read();
        QueueMessage<Object> message1 = future1.get(5, TimeUnit.SECONDS);
        
        assertNotNull(message1);
        assertFalse(message1.isDone());
        assertTrue(message1.getValue() instanceof StreamingResult);
        
        StreamingResult result1 = (StreamingResult) message1.getValue();
        assertEquals("system_result", result1.getType());
        assertTrue(result1.getContent().contains("Memory usage"));
        
        // 发送系统命令 - Agent状态
        Command command2 = new Command("system", "agent-status");
        processor.enqueueCommand(command2);
        
        // 等待处理完成
        Thread.sleep(500);
        
        // 验证输出队列中有结果
        CompletableFuture<QueueMessage<Object>> future2 = processor.getOutputStream().read();
        QueueMessage<Object> message2 = future2.get(5, TimeUnit.SECONDS);
        
        assertNotNull(message2);
        assertFalse(message2.isDone());
        assertTrue(message2.getValue() instanceof StreamingResult);
        
        StreamingResult result2 = (StreamingResult) message2.getValue();
        assertEquals("system_result", result2.getType());
        assertTrue(result2.getContent().contains("Main Agent status"));
        
        // 清理
        processor.close();
        
        System.out.println("testSystemCommandProcessing completed!");
    }
    
    @Test
    @DisplayName("测试不支持的命令处理")
    public void testUnsupportedCommandProcessing() throws Exception {
        System.out.println("Starting testUnsupportedCommandProcessing...");
        
        // 创建必要的组件
        MainAgent mainAgent = new MainAgent("test-main", "Test Main Agent");
        ToolEngine toolEngine = new ToolEngine();
        MemoryManager memoryManager = new MemoryManager();
        
        // 创建流式处理器
        StreamingProcessor processor = new StreamingProcessor(mainAgent, toolEngine, memoryManager);
        
        // 发送不支持的命令
        Command command = new Command("unknown", "some value");
        processor.enqueueCommand(command);
        
        // 等待处理完成
        Thread.sleep(500);
        
        // 验证输出队列中有错误结果
        CompletableFuture<QueueMessage<Object>> future = processor.getOutputStream().read();
        QueueMessage<Object> message = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(message);
        assertFalse(message.isDone());
        assertTrue(message.getValue() instanceof StreamingResult);
        
        StreamingResult result = (StreamingResult) message.getValue();
        assertEquals("error", result.getType());
        assertTrue(result.getContent().contains("Unsupported command mode"));
        
        // 清理
        processor.close();
        
        System.out.println("testUnsupportedCommandProcessing completed!");
    }
}