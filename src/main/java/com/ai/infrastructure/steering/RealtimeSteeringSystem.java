package com.ai.infrastructure.steering;

import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.memory.MemoryManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 实时Steering系统的完整集成
 * 基于Claude Code的完整实现，支持消息流处理管道
 */
public class RealtimeSteeringSystem implements AutoCloseable {
    private final AsyncMessageQueue<String> inputQueue;
    private final StreamingMessageParser messageParser;
    private final StreamingProcessor processor;
    private final MainAgentLoop agentLoop;
    private final MainAgent mainAgent;
    private final MemoryManager memoryManager;
    private final ToolEngine toolEngine;
    private final AtomicBoolean isClosed;
    private final AtomicBoolean isProcessing;
    
    public RealtimeSteeringSystem() {
        this.inputQueue = new AsyncMessageQueue<>();
        this.mainAgent = new MainAgent("steering-main", "Realtime Steering Main Agent");
        this.memoryManager = new MemoryManager();
        this.toolEngine = new ToolEngine();
        
        this.messageParser = new StreamingMessageParser(inputQueue);
        this.processor = new StreamingProcessor(mainAgent, toolEngine, memoryManager);
        this.agentLoop = new MainAgentLoop(mainAgent, memoryManager, toolEngine);
        this.isClosed = new AtomicBoolean(false);
        this.isProcessing = new AtomicBoolean(false);
        
        // 启动消息解析器
        this.messageParser.startProcessing();
    }
    
    /**
     * 获取输入队列
     * @return AsyncMessageQueue<String>
     */
    public AsyncMessageQueue<String> getInputQueue() {
        return inputQueue;
    }
    
    /**
     * 获取输出队列
     * @return AsyncMessageQueue<Object>
     */
    public AsyncMessageQueue<Object> getOutputQueue() {
        return processor.getOutputStream();
    }
    
    /**
     * 发送输入消息 - 支持实时Steering
     * @param message 消息
     */
    public void sendInput(String message) {
        if (!isClosed.get()) {
            inputQueue.enqueue(message);
        }
    }
    
    /**
     * 发送命令 - 支持并发执行
     * @param command 命令
     */
    public void sendCommand(Command command) {
        if (!isClosed.get()) {
            processor.enqueueCommand(command);
        }
    }
    
    /**
     * 处理输入数据 - 输入处理协程实现
     * @param message 用户消息
     */
    public void processInput(UserMessage message) {
        if (isClosed.get()) return;
        
        try {
            String promptContent = extractPromptContent(message);
            
            // 新消息入队
            Command command = new Command("prompt", promptContent);
            processor.enqueueCommand(command);
        } catch (Exception e) {
            System.err.println("Error processing input: " + e.getMessage());
        }
    }
    
    /**
     * 提取消息内容 - 支持多种格式
     * @param message 用户消息
     * @return String
     */
    private String extractPromptContent(UserMessage message) {
        Object content = message.getMessage().get("content");
        
        if (content == null) {
            return "";
        }
        
        // 处理不同类型的content
        if (content instanceof String) {
            // 字符串内容
            return ((String) content).replace("\"", "");
        } else if (content instanceof Map) {
            // 对象内容，如 { "text": "message", "format": "markdown" }
            Map<?, ?> contentMap = (Map<?, ?>) content;
            if (contentMap.containsKey("text")) {
                return contentMap.get("text").toString();
            } else if (contentMap.containsKey("content")) {
                return contentMap.get("content").toString();
            } else {
                // 返回整个对象的字符串表示
                return content.toString();
            }
        } else if (content instanceof List) {
            // 数组内容，如多个文本段
            List<?> contentList = (List<?>) content;
            StringBuilder result = new StringBuilder();
            for (Object item : contentList) {
                if (item instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) item;
                    if (itemMap.containsKey("text")) {
                        result.append(itemMap.get("text").toString()).append("\n");
                    } else {
                        result.append(item.toString()).append("\n");
                    }
                } else {
                    result.append(item.toString()).append("\n");
                }
            }
            return result.toString().trim();
        } else {
            // 其他类型，直接转换为字符串
            return content.toString().replace("\"", "");
        }
    }
    
    /**
     * 中断处理 - 支持多源中断
     * @param reason 中断原因
     */
    public void abort(String reason) {
        if (isClosed.compareAndSet(false, true)) {
            agentLoop.abort();
            processor.complete();
            messageParser.close();
            inputQueue.complete();
            
            System.out.println("Realtime Steering System aborted: " + reason);
        }
    }
    
    /**
     * 启动系统
     */
    public void start() {
        if (!isClosed.get()) {
            System.out.println("Realtime Steering System started");
            System.out.println("Ready for real-time interaction...");
        }
    }
    
    /**
     * 关闭系统 - 确保资源清理
     */
    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                messageParser.close();
                processor.close();
                inputQueue.cleanup();
                
                System.out.println("Realtime Steering System closed");
            } catch (Exception e) {
                System.err.println("Error closing system: " + e.getMessage());
            }
        }
    }
    
    /**
     * 检查系统是否已关闭
     * @return boolean
     */
    public boolean isClosed() {
        return isClosed.get();
    }
    
    /**
     * 检查系统是否正在处理
     * @return boolean
     */
    public boolean isProcessing() {
        return isProcessing.get();
    }
    
    /**
     * 获取系统状态信息
     * @return String
     */
    public String getStatusInfo() {
        return String.format("RealtimeSteeringSystem{closed=%b, processing=%b}", 
            isClosed.get(), isProcessing.get());
    }
    
    /**
     * 获取队列命令访问器
     * @return Supplier<List<Command>>
     */
    public Supplier<List<Command>> getQueuedCommandsSupplier() {
        return processor.getQueuedCommandsSupplier();
    }
    
    /**
     * 移除队列中的命令
     * @param commandsToRemove 要移除的命令
     */
    public void removeQueuedCommands(List<Command> commandsToRemove) {
        processor.removeQueuedCommands(commandsToRemove);
    }
}