package com.ai.infrastructure.steering;

import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.steering.StreamingProcessor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 实时Steering系统的完整集成
 * 基于Claude Code的完整实现，支持消息流处理管道
 */
public class RealtimeSteeringSystem implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RealtimeSteeringSystem.class);
    private final AsyncMessageQueue<String> inputQueue;
    private final StreamingMessageParser messageParser;
    private final StreamingProcessor processor;
    private final MainAgentLoop agentLoop;
    private final MainAgent mainAgent;
    private final MemoryManager memoryManager;
    private final ToolEngine toolEngine;
    private final AtomicBoolean isClosed;
    private final AtomicBoolean isProcessing;
    private final AtomicBoolean isAgentLoopActive;
    
    public RealtimeSteeringSystem(String apiKey) {
        this.inputQueue = new AsyncMessageQueue<>();
        this.mainAgent = new MainAgent("steering-main", "Realtime Steering Main Agent", apiKey);
        this.memoryManager = new MemoryManager();
        this.toolEngine = new ToolEngine();
        
        this.messageParser = new StreamingMessageParser(inputQueue);
        this.processor = new StreamingProcessor(mainAgent, toolEngine, memoryManager);
        this.agentLoop = new MainAgentLoop(mainAgent, memoryManager, toolEngine);
        this.isClosed = new AtomicBoolean(false);
        this.isProcessing = new AtomicBoolean(false);
        this.isAgentLoopActive = new AtomicBoolean(false);
        
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
            logger.error("Error processing input: {}", e.getMessage(), e);
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
            
            logger.warn("Realtime Steering System aborted: {}", reason);
        }
    }
    
    /**
     * 启动系统
     */
    public void start() {
        if (!isClosed.get()) {
            logger.info("Realtime Steering System started");
            logger.info("Ready for real-time interaction...");
            
            // 启动mainAgentLoop
            startMainAgentLoop();
        }
    }
    
    /**
     * 关闭系统 - 确保资源清理
     */
    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                // 停止mainAgentLoop
                stopMainAgentLoop();
                
                messageParser.close();
                processor.close();
                inputQueue.cleanup();
                
                logger.info("Realtime Steering System closed");
            } catch (Exception e) {
                logger.error("Error closing system: {}", e.getMessage(), e);
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
        return String.format("RealtimeSteeringSystem{closed=%b, processing=%b, agentLoopActive=%b}", 
            isClosed.get(), isProcessing.get(), isAgentLoopActive.get());
    }
    
    /**
     * 启动主Agent循环
     */
    private void startMainAgentLoop() {
        logger.info("Starting MainAgentLoop");
        
        // 在新的线程中启动主Agent循环
        Thread agentLoopThread = new Thread(() -> {
            try {
                // 创建初始消息列表
                List<Object> initialMessages = new ArrayList<>();
                
                // 启动主Agent循环，等待输入
                while (!isClosed.get()) {
                    // 这里可以添加循环逻辑，定期检查输入队列
                    // 目前简化处理，只是保持线程运行
                    Thread.sleep(1000);
                    
                    // 检查是否有待处理的命令
                    List<Command> queuedCommands = processor.getQueuedCommandsSupplier().get();
                    if (!queuedCommands.isEmpty() && !isProcessing.get()) {
                        isProcessing.set(true);
                        logger.debug("Processing {} queued commands", queuedCommands.size());
                        
                        // 处理队列中的命令
                        for (Command command : queuedCommands) {
                            if (isClosed.get()) break;
                            
                            try {
                                // 使用MainAgentLoop处理命令
                                List<Object> messages = new ArrayList<>();
                                messages.add("Command: " + command.getMode() + " - " + command.getValue());
                                
                                CompletableFuture<StreamingResult> resultFuture = agentLoop.executeLoop(
                                    messages, command.getValue());
                                
                                StreamingResult result = resultFuture.get(30, java.util.concurrent.TimeUnit.SECONDS);
                                logger.debug("Command processed: {}", result.getType());
                            } catch (Exception e) {
                                logger.error("Error processing command: {}", e.getMessage(), e);
                            }
                        }
                        
                        // 清理已处理的命令
                        processor.removeQueuedCommands(queuedCommands);
                        isProcessing.set(false);
                    }
                }
            } catch (Exception e) {
                logger.error("Error in MainAgentLoop: {}", e.getMessage(), e);
            } finally {
                isAgentLoopActive.set(false);
                logger.info("MainAgentLoop stopped");
            }
        });
        
        agentLoopThread.setDaemon(true);
        agentLoopThread.start();
        isAgentLoopActive.set(true);
        logger.info("MainAgentLoop started in background thread");
    }
    
    /**
     * 停止主Agent循环
     */
    private void stopMainAgentLoop() {
        logger.info("Stopping MainAgentLoop");
        if (agentLoop != null) {
            agentLoop.abort();
        }
        isAgentLoopActive.set(false);
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