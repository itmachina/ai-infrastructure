package com.ai.infrastructure.steering;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异步消息队列测试类
 */
@DisplayName("AsyncMessageQueue Test")
public class AsyncMessageQueueTest {

    @Test
    @DisplayName("测试基本入队和出队")
    public void testBasicEnqueueDequeue() throws Exception {
        AsyncMessageQueue<String> queue = new AsyncMessageQueue<>();
        
        // 入队消息
        queue.enqueue("Message 1");
        queue.enqueue("Message 2");
        
        // 出队消息
        CompletableFuture<QueueMessage<String>> future1 = queue.read();
        QueueMessage<String> msg1 = future1.get(1, TimeUnit.SECONDS);
        assertEquals("Message 1", msg1.getValue());
        assertFalse(msg1.isDone());
        
        CompletableFuture<QueueMessage<String>> future2 = queue.read();
        QueueMessage<String> msg2 = future2.get(1, TimeUnit.SECONDS);
        assertEquals("Message 2", msg2.getValue());
        assertFalse(msg2.isDone());
    }

    @Test
    @DisplayName("测试异步读取")
    public void testAsyncRead() throws Exception {
        AsyncMessageQueue<String> queue = new AsyncMessageQueue<>();
        
        // 先启动异步读取
        CompletableFuture<QueueMessage<String>> future = queue.read();
        
        // 然后入队消息
        queue.enqueue("Async Message");
        
        // 验证消息被正确接收
        QueueMessage<String> msg = future.get(1, TimeUnit.SECONDS);
        assertEquals("Async Message", msg.getValue());
        assertFalse(msg.isDone());
    }

    @Test
    @DisplayName("测试完成状态")
    public void testCompletion() throws Exception {
        AsyncMessageQueue<String> queue = new AsyncMessageQueue<>();
        
        // 入队消息然后标记完成
        queue.enqueue("Before completion");
        queue.complete();
        
        // 读取已入队的消息
        CompletableFuture<QueueMessage<String>> future1 = queue.read();
        QueueMessage<String> msg1 = future1.get(1, TimeUnit.SECONDS);
        assertEquals("Before completion", msg1.getValue());
        assertFalse(msg1.isDone());
        
        // 读取完成状态消息
        CompletableFuture<QueueMessage<String>> future2 = queue.read();
        QueueMessage<String> msg2 = future2.get(1, TimeUnit.SECONDS);
        assertNull(msg2.getValue());
        assertTrue(msg2.isDone());
    }

    @Test
    @DisplayName("测试错误状态")
    public void testErrorState() throws Exception {
        AsyncMessageQueue<String> queue = new AsyncMessageQueue<>();
        
        // 设置错误状态
        RuntimeException testError = new RuntimeException("Test error");
        queue.error(testError);
        
        // 尝试读取应该失败
        CompletableFuture<QueueMessage<String>> future = queue.read();
        Exception thrown = assertThrows(Exception.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        });
        assertTrue(thrown.getCause() instanceof RuntimeException);
        assertEquals("Test error", thrown.getCause().getMessage());
    }
}