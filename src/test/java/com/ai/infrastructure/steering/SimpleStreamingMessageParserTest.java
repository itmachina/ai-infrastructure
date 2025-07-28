package com.ai.infrastructure.steering;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单测试StreamingMessageParser功能
 */
public class SimpleStreamingMessageParserTest {

    @Test
    public void testSimpleTextMessage() {
        System.out.println("Testing simple text message parsing...");
        
        // 创建一个UserMessage对象进行测试
        UserMessage message = new UserMessage("user", "Hello, World!");
        
        assertEquals("user", message.getType());
        assertEquals("user", message.getRole());
        assertEquals("Hello, World!", message.getContent());
        assertTrue(message.isValid());
        
        System.out.println("Simple text message test passed!");
    }
    
    @Test
    public void testJsonMessage() {
        System.out.println("Testing UserMessage with JSON-like content...");
        
        // 创建一个UserMessage对象进行测试
        UserMessage message = new UserMessage("user", "{\"text\":\"Hello from JSON\"}");
        
        assertEquals("user", message.getType());
        assertEquals("user", message.getRole());
        assertEquals("{\"text\":\"Hello from JSON\"}", message.getContent());
        assertTrue(message.isValid());
        
        System.out.println("JSON message test passed!");
    }
}