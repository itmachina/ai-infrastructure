package com.ai.infrastructure.steering;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 流式消息解析器测试类
 */
@DisplayName("StreamingMessageParser Test")
public class StreamingMessageParserTest {

    @Test
    @DisplayName("测试简单文本消息解析")
    public void testSimpleTextMessage() throws Exception {
        System.out.println("Starting testSimpleTextMessage...");
        
        AsyncMessageQueue<String> inputQueue = new AsyncMessageQueue<>();
        StreamingMessageParser parser = new StreamingMessageParser(inputQueue);
        
        // 启动解析器
        parser.startProcessing();
        Thread.sleep(100); // 等待解析器启动
        
        // 发送简单文本消息
        inputQueue.enqueue("Hello, World!\n");
        System.out.println("Sent text message");
        
        // 读取解析后的消息
        CompletableFuture<QueueMessage<UserMessage>> future = parser.getOutputStream().read();
        QueueMessage<UserMessage> message = future.get(5, TimeUnit.SECONDS); // 增加超时时间
        
        assertNotNull(message);
        assertFalse(message.isDone());
        assertEquals("Hello, World!", message.getValue().getContent());
        assertEquals("user", message.getValue().getRole());
        
        // 完成队列
        inputQueue.complete();
        parser.close();
        
        System.out.println("testSimpleTextMessage completed!");
    }
    
    @Test
    @DisplayName("测试基本JSON消息解析")
    public void testBasicJsonMessage() throws Exception {
        System.out.println("Starting testBasicJsonMessage...");
        
        AsyncMessageQueue<String> inputQueue = new AsyncMessageQueue<>();
        StreamingMessageParser parser = new StreamingMessageParser(inputQueue);
        
        // 启动解析器
        parser.startProcessing();
        Thread.sleep(100); // 等待解析器启动
        
        // 发送JSON格式消息
        String jsonMessage = "{\"type\":\"user\",\"message\":{\"role\":\"user\",\"content\":\"Hello from JSON\"}}\n";
        inputQueue.enqueue(jsonMessage);
        System.out.println("Sent JSON message: " + jsonMessage);
        
        // 读取解析后的消息
        CompletableFuture<QueueMessage<UserMessage>> future = parser.getOutputStream().read();
        QueueMessage<UserMessage> message = future.get(5, TimeUnit.SECONDS); // 增加超时时间
        
        assertNotNull(message);
        assertFalse(message.isDone());
        assertEquals("Hello from JSON", message.getValue().getContent());
        assertEquals("user", message.getValue().getRole());
        assertEquals("user", message.getValue().getType());
        
        // 完成队列
        inputQueue.complete();
        parser.close();
        
        System.out.println("testBasicJsonMessage completed!");
    }
    
    @Test
    @DisplayName("测试复杂JSON消息解析")
    public void testComplexJsonMessage() throws Exception {
        System.out.println("Starting testComplexJsonMessage...");
        
        AsyncMessageQueue<String> inputQueue = new AsyncMessageQueue<>();
        StreamingMessageParser parser = new StreamingMessageParser(inputQueue);
        
        // 启动解析器
        parser.startProcessing();
        Thread.sleep(100); // 等待解析器启动
        
        // 发送复杂JSON格式消息
        String jsonMessage = "{\"type\":\"user\",\"message\":{\"role\":\"user\",\"content\":{\"text\":\"Complex message content\",\"format\":\"markdown\"}}}\n";
        inputQueue.enqueue(jsonMessage);
        System.out.println("Sent complex JSON message: " + jsonMessage);
        
        // 读取解析后的消息
        CompletableFuture<QueueMessage<UserMessage>> future = parser.getOutputStream().read();
        QueueMessage<UserMessage> message = future.get(5, TimeUnit.SECONDS); // 增加超时时间
        
        assertNotNull(message);
        assertFalse(message.isDone());
        assertTrue(message.getValue().getContent().contains("Complex message content"));
        assertEquals("user", message.getValue().getRole());
        assertEquals("user", message.getValue().getType());
        
        // 完成队列
        inputQueue.complete();
        parser.close();
        
        System.out.println("testComplexJsonMessage completed!");
    }
    
    @Test
    @DisplayName("测试无效JSON消息处理")
    public void testInvalidJsonMessage() throws Exception {
        System.out.println("Starting testInvalidJsonMessage...");
        
        AsyncMessageQueue<String> inputQueue = new AsyncMessageQueue<>();
        StreamingMessageParser parser = new StreamingMessageParser(inputQueue);
        
        // 启动解析器
        parser.startProcessing();
        Thread.sleep(100); // 等待解析器启动
        
        // 发送无效JSON消息
        String invalidJson = "{\"type\":\"user\",\"message\":{\"role\":\"user\"}}\n"; // 缺少content字段
        inputQueue.enqueue(invalidJson);
        System.out.println("Sent invalid JSON message: " + invalidJson);
        
        // 完成队列
        inputQueue.complete();
        parser.close();
        
        // 验证没有产生输出消息，或者产生完成消息
        CompletableFuture<QueueMessage<UserMessage>> future = parser.getOutputStream().read();
        QueueMessage<UserMessage> message = future.get(5, TimeUnit.SECONDS); // 增加超时时间
        assertTrue(message.isDone());
        
        System.out.println("testInvalidJsonMessage completed!");
    }
    
    @Test
    @DisplayName("测试错误消息类型处理")
    public void testWrongMessageType() throws Exception {
        System.out.println("Starting testWrongMessageType...");
        
        AsyncMessageQueue<String> inputQueue = new AsyncMessageQueue<>();
        StreamingMessageParser parser = new StreamingMessageParser(inputQueue);
        
        // 启动解析器
        parser.startProcessing();
        Thread.sleep(100); // 等待解析器启动
        
        // 发送错误类型的消息
        String wrongTypeMessage = "{\"type\":\"assistant\",\"message\":{\"role\":\"user\",\"content\":\"Wrong type\"}}\n";
        inputQueue.enqueue(wrongTypeMessage);
        System.out.println("Sent wrong type message: " + wrongTypeMessage);
        
        // 完成队列
        inputQueue.complete();
        parser.close();
        
        // 验证没有产生输出消息，或者产生完成消息
        CompletableFuture<QueueMessage<UserMessage>> future = parser.getOutputStream().read();
        QueueMessage<UserMessage> message = future.get(5, TimeUnit.SECONDS); // 增加超时时间
        assertTrue(message.isDone());
        
        System.out.println("testWrongMessageType completed!");
    }
}