package com.ai.infrastructure.steering;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 异步消息队列集成测试类
 */
public class AsyncMessageQueueIntegrationTest {
    public static void main(String[] args) {
        System.out.println("Starting AsyncMessageQueue tests...");
        
        // 测试1: 基本入队和出队
        testBasicEnqueueDequeue();
        
        // 测试2: 异步读取
        testAsyncRead();
        
        // 测试3: 完成状态
        testCompletion();
        
        // 测试4: 错误状态
        testErrorState();
        
        // 测试5: 实时Steering系统
        testRealtimeSteering();
        
        System.out.println("All tests completed!");
    }
    
    /**
     * 测试基本入队和出队
     */
    private static void testBasicEnqueueDequeue() {
        System.out.println("\n--- Test 1: Basic Enqueue/Dequeue ---");
        
        try {
            AsyncMessageQueue<String> queue = new AsyncMessageQueue<>();
            
            // 入队消息
            queue.enqueue("Message 1");
            queue.enqueue("Message 2");
            queue.enqueue("Message 3");
            
            // 出队消息
            CompletableFuture<QueueMessage<String>> future1 = queue.read();
            QueueMessage<String> msg1 = future1.join();
            System.out.println("Dequeued: " + msg1.getValue());
            
            CompletableFuture<QueueMessage<String>> future2 = queue.read();
            QueueMessage<String> msg2 = future2.join();
            System.out.println("Dequeued: " + msg2.getValue());
            
            CompletableFuture<QueueMessage<String>> future3 = queue.read();
            QueueMessage<String> msg3 = future3.join();
            System.out.println("Dequeued: " + msg3.getValue());
            
            System.out.println("✓ Basic enqueue/dequeue test passed");
        } catch (Exception e) {
            System.out.println("✗ Basic enqueue/dequeue test failed: " + e.getMessage());
        }
    }
    
    /**
     * 测试异步读取
     */
    private static void testAsyncRead() {
        System.out.println("\n--- Test 2: Async Read ---");
        
        try {
            AsyncMessageQueue<Integer> queue = new AsyncMessageQueue<>();
            
            // 启动一个线程在稍后入队消息
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 等待1秒
                    queue.enqueue(42);
                    queue.enqueue(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            // 异步读取消息
            CompletableFuture<QueueMessage<Integer>> future1 = queue.read();
            QueueMessage<Integer> msg1 = future1.get(2, TimeUnit.SECONDS); // 2秒超时
            System.out.println("Async read: " + msg1.getValue());
            
            CompletableFuture<QueueMessage<Integer>> future2 = queue.read();
            QueueMessage<Integer> msg2 = future2.get(2, TimeUnit.SECONDS); // 2秒超时
            System.out.println("Async read: " + msg2.getValue());
            
            System.out.println("✓ Async read test passed");
        } catch (Exception e) {
            System.out.println("✗ Async read test failed: " + e.getMessage());
        }
    }
    
    /**
     * 测试完成状态
     */
    private static void testCompletion() {
        System.out.println("\n--- Test 3: Completion ---");
        
        try {
            AsyncMessageQueue<String> queue = new AsyncMessageQueue<>();
            
            // 入队一些消息
            queue.enqueue("Before completion");
            
            // 标记完成
            queue.complete();
            
            // 读取已入队的消息
            CompletableFuture<QueueMessage<String>> future1 = queue.read();
            QueueMessage<String> msg1 = future1.join();
            System.out.println("Message before completion: " + msg1.getValue());
            
            // 读取完成状态消息
            CompletableFuture<QueueMessage<String>> future2 = queue.read();
            QueueMessage<String> msg2 = future2.join();
            System.out.println("Completion message done: " + msg2.isDone());
            
            System.out.println("✓ Completion test passed");
        } catch (Exception e) {
            System.out.println("✗ Completion test failed: " + e.getMessage());
        }
    }
    
    /**
     * 测试错误状态
     */
    private static void testErrorState() {
        System.out.println("\n--- Test 4: Error State ---");
        
        try {
            AsyncMessageQueue<String> queue = new AsyncMessageQueue<>();
            
            // 设置错误状态
            queue.error(new RuntimeException("Test error"));
            
            // 尝试读取应该失败
            CompletableFuture<QueueMessage<String>> future = queue.read();
            try {
                QueueMessage<String> msg = future.join();
                System.out.println("✗ Error state test failed: Expected exception");
            } catch (Exception e) {
                System.out.println("✓ Error state test passed: " + e.getCause().getMessage());
            }
        } catch (Exception e) {
            System.out.println("✗ Error state test failed: " + e.getMessage());
        }
    }
    
    /**
     * 测试实时Steering系统
     */
    private static void testRealtimeSteering() {
        System.out.println("\n--- Test 5: Realtime Steering System ---");
        
        try (RealtimeSteeringSystem system = new RealtimeSteeringSystem()) {
            system.start();
            
            // 发送一些输入
            system.sendInput("Hello, World!");
            system.sendInput("How are you?");
            
            // 发送命令
            system.sendCommand(new Command("prompt", "Calculate 2+2"));
            
            System.out.println("✓ Realtime Steering system test passed");
        } catch (Exception e) {
            System.out.println("✗ Realtime Steering system test failed: " + e.getMessage());
        }
    }
}