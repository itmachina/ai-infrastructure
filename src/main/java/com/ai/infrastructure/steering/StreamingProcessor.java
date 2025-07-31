package com.ai.infrastructure.steering;

import com.ai.infrastructure.agent.AgentStatus;
import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.tools.ToolEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 流式处理引擎 - 协调消息队列和Agent执行
 * 基于Claude Code的kq5函数实现，支持并发执行协调和复杂的命令处理
 */
public class StreamingProcessor implements AutoCloseable {
    private final Queue<Command> commandQueue;
    private final AtomicBoolean isExecuting;
    private final AtomicBoolean isCompleted;
    private final AsyncMessageQueue<Object> outputQueue;
    private final List<Object> messageHistory;
    private final MainAgent mainAgent;
    private final ToolEngine toolEngine;
    private final MemoryManager memoryManager;
    private volatile boolean isClosed;
    
    public StreamingProcessor(MainAgent mainAgent, ToolEngine toolEngine, MemoryManager memoryManager) {
        this.commandQueue = new ConcurrentLinkedQueue<>();
        this.isExecuting = new AtomicBoolean(false);
        this.isCompleted = new AtomicBoolean(false);
        this.outputQueue = new AsyncMessageQueue<>();
        this.messageHistory = new ArrayList<>();
        this.mainAgent = mainAgent;
        this.toolEngine = toolEngine;
        this.memoryManager = memoryManager;
        this.isClosed = false;
    }
    
    /**
     * 获取输出流
     * @return AsyncMessageQueue<Object>
     */
    public AsyncMessageQueue<Object> getOutputStream() {
        return outputQueue;
    }
    
    /**
     * 获取队列中的命令访问器
     * @return Supplier<List<Command>>
     */
    public Supplier<List<Command>> getQueuedCommandsSupplier() {
        return () -> new ArrayList<>(commandQueue);
    }
    
    /**
     * 移除队列中的命令
     * @param commandsToRemove 要移除的命令
     */
    public void removeQueuedCommands(List<Command> commandsToRemove) {
        commandQueue.removeAll(commandsToRemove);
    }
    
    /**
     * 添加命令到队列
     * @param command 命令
     */
    public void enqueueCommand(Command command) {
        if (!isCompleted.get() && !isClosed) {
            commandQueue.offer(command);
            
            // 如果未在执行，启动执行
            if (!isExecuting.get()) {
                executeCommandsAsync();
            }
        }
    }
    
    /**
     * 异步执行命令 - 支持并发控制
     */
    private void executeCommandsAsync() {
        if (isExecuting.compareAndSet(false, true)) {
            CompletableFuture.runAsync(this::executeCommands)
                .exceptionally(throwable -> {
                    System.err.println("Execution error: " + throwable.getMessage());
                    outputQueue.error(new RuntimeException(throwable));
                    isExecuting.set(false);
                    return null;
                });
        }
    }
    
    /**
     * 执行命令 - 核心调度逻辑
     * 基于Claude Code的kq5.executeCommands实现，支持复杂的命令处理
     */
    private void executeCommands() {
        try {
            // 处理队列中的所有命令
            while (!commandQueue.isEmpty() && !mainAgent.isAborted()) {
                Command command = commandQueue.poll();
                if (command == null) break;
                
                // 支持多种命令模式 - 基于Claude Code的实现
                switch (command.getMode()) {
                    case "prompt":
                        // 执行Agent主循环
                        executeAgentLoop(command.getValue());
                        break;
                    case "tool":
                        // 直接执行工具命令
                        executeToolCommand(command.getValue());
                        break;
                    case "system":
                        // 执行系统命令
                        executeSystemCommand(command.getValue());
                        break;
                    default:
                        System.err.println("Unsupported command mode: " + command.getMode());
                        outputQueue.enqueue(new StreamingResult("error", "Unsupported command mode: " + command.getMode()));
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Command execution error: " + e.getMessage());
            outputQueue.error(e);
        } finally {
            isExecuting.set(false);
            
            if (isCompleted.get() && !isClosed) {
                outputQueue.complete();
            }
        }
    }
    
    /**
     * 执行Agent主循环 - 关键调用点
     * @param prompt 提示
     */
    private void executeAgentLoop(String prompt) {
        try {
            // 调用主Agent执行循环，使用MainAgentLoop
            MainAgentLoop agentLoop = new MainAgentLoop(mainAgent, memoryManager, toolEngine);
            
            // 构建初始消息历史
            List<Object> initialMessages = new ArrayList<>(messageHistory);
            
            // 执行Agent循环
            CompletableFuture<StreamingResult> resultFuture = agentLoop.executeLoop(initialMessages, prompt);
            StreamingResult result = resultFuture.join();
            
            // 更新消息历史
            messageHistory.add(result);
            outputQueue.enqueue(result);
        } catch (Exception e) {
            System.err.println("Agent execution error: " + e.getMessage());
            outputQueue.error(e);
        }
    }
    
    /**
     * 执行工具命令 - 直接调用工具引擎
     * @param command 命令内容
     */
    private void executeToolCommand(String command) {
        try {
            // 直接调用工具引擎执行命令
            String result = toolEngine.executeTool(command);
            StreamingResult streamingResult = new StreamingResult("tool_result", result);
            
            // 更新消息历史
            messageHistory.add(streamingResult);
            outputQueue.enqueue(streamingResult);
        } catch (Exception e) {
            System.err.println("Tool execution error: " + e.getMessage());
            outputQueue.error(e);
        }
    }
    
    /**
     * 执行系统命令 - 处理系统级操作
     * @param command 命令内容
     */
    private void executeSystemCommand(String command) {
        try {
            // 处理系统级命令
            String result = handleSystemCommand(command);
            StreamingResult streamingResult = new StreamingResult("system_result", result);
            
            // 更新消息历史
            messageHistory.add(streamingResult);
            outputQueue.enqueue(streamingResult);
        } catch (Exception e) {
            System.err.println("System command error: " + e.getMessage());
            outputQueue.error(e);
        }
    }
    
    /**
     * 处理系统命令 - 支持内存管理、状态查询等操作
     * @param command 命令内容
     * @return 执行结果
     */
    private String handleSystemCommand(String command) {
        if (command.startsWith("memory-stats")) {
            // 返回内存使用统计
            return "Memory usage: " + memoryManager.getCurrentTokenUsage() + " tokens";
        } else if (command.startsWith("clear-memory")) {
            // 清理内存
            memoryManager.clear();
            return "Memory cleared";
        } else if (command.startsWith("agent-status")) {
            // 返回Agent状态
            return "Main Agent status: " + mainAgent.getStatus();
        } else {
            return "Unknown system command: " + command;
        }
    }
    
    /**
     * 中断处理
     */
    public void abort() {
        // 中断主Agent
        mainAgent.setStatus(AgentStatus.ABORTED);
        
        // 标记完成
        isCompleted.set(true);
        
        // 完成输出队列
        if (!isClosed) {
            outputQueue.complete();
        }
    }
    
    /**
     * 标记处理完成
     */
    public void complete() {
        isCompleted.set(true);
        if (!isExecuting.get() && !isClosed) {
            outputQueue.complete();
        }
    }
    
    /**
     * 关闭处理器
     */
    @Override
    public void close() {
        if (!isClosed) {
            isClosed = true;
            isCompleted.set(true);
            outputQueue.complete();
        }
    }
    
    /**
     * 检查是否已完成
     * @return boolean
     */
    public boolean isCompleted() {
        return isCompleted.get();
    }
    
    /**
     * 检查是否正在执行
     * @return boolean
     */
    public boolean isExecuting() {
        return isExecuting.get();
    }
}