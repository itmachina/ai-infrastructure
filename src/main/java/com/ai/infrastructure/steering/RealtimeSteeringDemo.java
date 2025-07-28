package com.ai.infrastructure.steering;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 实时Steering系统演示类
 */
public class RealtimeSteeringDemo {
    public static void main(String[] args) {
        System.out.println("=== 实时Steering系统演示 ===");
        System.out.println("输入命令与AI交互，输入 'quit' 退出程序");
        System.out.println("支持的命令:");
        System.out.println("  - 直接输入文本: 发送消息给AI");
        System.out.println("  - /calc <表达式>: 计算数学表达式");
        System.out.println("  - /read <文件名>: 读取文件内容");
        System.out.println("  - /search <关键词>: 搜索内容");
        System.out.println("  - quit: 退出程序");
        System.out.println();
        
        // 创建实时Steering系统
        try (RealtimeSteeringSystem system = new RealtimeSteeringSystem()) {
            system.start();
            
            // 创建输入处理线程
            ExecutorService executor = Executors.newSingleThreadExecutor();
            
            // 启动输出处理
            CompletableFuture<Void> outputFuture = CompletableFuture.runAsync(() -> {
                try {
                    AsyncMessageQueue<Object> outputQueue = system.getOutputQueue();
                    while (!system.isClosed()) {
                        CompletableFuture<QueueMessage<Object>> readFuture = outputQueue.read();
                        QueueMessage<Object> message = readFuture.join();
                        
                        if (message.isDone()) {
                            break;
                        }
                        
                        Object value = message.getValue();
                        if (value != null) {
                            System.out.println("[AI] " + value);
                        }
                    }
                } catch (Exception e) {
                    if (!system.isClosed()) {
                        System.err.println("输出处理错误: " + e.getMessage());
                    }
                }
            });
            
            // 处理用户输入
            Scanner scanner = new Scanner(System.in);
            String input;
            
            while (true) {
                System.out.print("> ");
                input = scanner.nextLine();
                
                if ("quit".equalsIgnoreCase(input)) {
                    break;
                }
                
                if (input.startsWith("/")) {
                    // 处理命令
                    handleCommand(system, input);
                } else if (!input.trim().isEmpty()) {
                    // 发送普通消息
                    system.sendInput(input);
                }
            }
            
            System.out.println("正在关闭系统...");
            executor.shutdown();
            
        } catch (Exception e) {
            System.err.println("系统错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("演示程序已结束");
    }
    
    /**
     * 处理命令
     * @param system 实时Steering系统
     * @param input 用户输入
     */
    private static void handleCommand(RealtimeSteeringSystem system, String input) {
        if (input.startsWith("/calc ")) {
            String expression = input.substring(6).trim();
            system.sendCommand(new Command("prompt", "Calculate " + expression));
        } else if (input.startsWith("/read ")) {
            String filename = input.substring(6).trim();
            system.sendCommand(new Command("prompt", "Read file " + filename));
        } else if (input.startsWith("/search ")) {
            String query = input.substring(8).trim();
            system.sendCommand(new Command("prompt", "Search for " + query));
        } else {
            System.out.println("未知命令: " + input);
            System.out.println("支持的命令: /calc, /read, /search");
        }
    }
}