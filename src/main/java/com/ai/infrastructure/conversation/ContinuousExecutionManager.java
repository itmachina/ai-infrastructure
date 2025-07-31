package com.ai.infrastructure.conversation;

import com.ai.infrastructure.agent.SubAgent;
import com.ai.infrastructure.model.OpenAIModelClient;
import com.ai.infrastructure.tools.ToolEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;

/**
 * 持续执行管理器，负责管理多轮对话和任务的持续执行
 * 基于Claude Code的持续执行机制实现
 */
public class ContinuousExecutionManager {
    private static final Logger logger = LoggerFactory.getLogger(ContinuousExecutionManager.class.getName());
    private ConversationManager conversationManager;
    private ToolEngine toolEngine;
    private OpenAIModelClient openAIModelClient;
    private AtomicInteger executionStep;
    private AtomicBoolean isExecuting;
    private Scanner scanner; // 用于从命令行读取用户输入
    private static final int MAX_EXECUTION_STEPS = 20; // 最大执行步骤数
    
        
    public ContinuousExecutionManager(ToolEngine toolEngine, OpenAIModelClient openAIModelClient) {
        this.conversationManager = new ConversationManager();
        this.toolEngine = toolEngine;
        this.openAIModelClient = openAIModelClient;
        this.executionStep = new AtomicInteger(0);
        this.isExecuting = new AtomicBoolean(false);
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * 执行任务并支持持续执行
     * @param task 任务描述
     * @return 执行结果
     */
    public CompletableFuture<String> executeTaskContinuously(String task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 检查是否已经在执行任务
                if (!isExecuting.compareAndSet(false, true)) {
                    return "Error: Task is already executing";
                }
                
                // 重置执行步骤计数器
                executionStep.set(0);
                
                // 开始执行任务
                return executeTaskStep(task);
            } catch (Exception e) {
                isExecuting.set(false);
                return "Error executing task: " + e.getMessage();
            }
        });
    }
    
    /**
     * 执行单个任务步骤
     * @param task 任务描述
     * @return 执行结果
     */
    private String executeTaskStep(String task) {
        try {
            // 检查是否超过最大执行步骤数
            if (executionStep.incrementAndGet() > MAX_EXECUTION_STEPS) {
                isExecuting.set(false);
                conversationManager.clearHistory();
                return "任务执行超时，已达到最大执行步骤数。";
            }
            
            // 构建完整的消息列表（包含历史记录）
            String systemMessage = conversationManager.getSystemMessage();
            String userMessage = task;
            
            // 如果有历史记录，将其包含在消息中
            if (conversationManager.getHistorySize() > 0) {
                userMessage = "继续执行任务: " + task + "\n\n历史对话记录:\n" + formatHistoryForPrompt();
            }
            
            // 调用模型
            String response = openAIModelClient.callModel(userMessage, systemMessage);
            
            // 处理模型响应
            String processedResponse = conversationManager.processModelResponse(response, toolEngine, openAIModelClient);
            
            // 根据处理结果决定下一步行动
            if (processedResponse.startsWith("CONTINUE:")) {
                // 需要继续执行，默认不需要用户输入，直接继续执行
                String nextStep = processedResponse.substring(9); // 移除"CONTINUE:"前缀
                logger.info("CONTINUE, nextStep:{}", nextStep);
                
                // 直接继续执行，不提示用户输入
                logger.info("Continuing execution without user input: {}", nextStep);
                return executeTaskStep("基于上下文继续执行任务: " + nextStep);
            } else if (processedResponse.startsWith("NEED_USER_INPUT:")) {
                // 模型明确要求用户输入
                String needUserInputContent = processedResponse.substring(16); // 移除"NEED_USER_INPUT:"前缀
                
                // 解析用户提示和内容
                String[] parts = needUserInputContent.split("\nCONTENT:", 2);
                String userPrompt = parts[0];
                String content = parts.length > 1 ? parts[1] : "AI需要更多信息来继续执行任务";
                
                // 提示用户输入更多信息
                System.out.println(content);
                System.out.print(userPrompt + ": ");
                
                // 从命令行读取用户输入
                String userInput = scanner.nextLine();
                
                // 将用户输入添加到历史记录中
                conversationManager.addMessageToHistory("user", userInput);
                
                // 继续执行任务
                return executeTaskStep(userInput);
            } else if (processedResponse.startsWith("TOOL_RESULT:")) {
                // 工具执行结果，继续执行
                String toolResult = processedResponse.substring(12); // 移除"TOOL_RESULT:"前缀
                logger.info("TOOL_RESULT, nextStep:{}", toolResult);
                return executeTaskStep("基于工具执行结果继续任务: " + toolResult);
            } else if (processedResponse.startsWith("SUBAGENT:")) {
                // 需要创建子Agent并执行任务
                String subagentTask = processedResponse.substring(9); // 移除"SUBAGENT:"前缀
                logger.info("SUBAGENT, nextStep:{}", subagentTask);
                // 创建子Agent并执行任务
                return executeSubAgentTask(subagentTask);
            } else if (processedResponse.startsWith("COMPLETE:")) {
                // 任务完成
                String result = processedResponse.substring(9); // 移除"COMPLETE:"前缀
                logger.info("COMPLETE, nextStep:{}", result);
                isExecuting.set(false);
                return "任务完成: " + result;
            } else {
                logger.info("ELSE, processedResponse:{}", processedResponse);
                // 默认情况，任务完成
                isExecuting.set(false);
                return "任务完成: " + processedResponse;
            }
        } catch (Exception e) {
            isExecuting.set(false);
            return "Error executing task step: " + e.getMessage();
        }
    }
    
    /**
     * 执行子Agent任务
     * @param task 任务描述
     * @return 执行结果
     */
    private String executeSubAgentTask(String task) {
        try {
            // 创建子Agent并执行任务
            SubAgent subAgent = new SubAgent("sub-" + System.currentTimeMillis(), "SubAgent for: " + task);
            String result = subAgent.executeTask(task).join();
            
            // 添加子Agent执行结果到历史记录
            conversationManager.addMessageToHistory("subagent_result", result);
            
            // 继续执行任务
            return executeTaskStep("基于子Agent执行结果继续任务: " + result);
        } catch (Exception e) {
            return "Error executing subagent task: " + e.getMessage();
        }
    }
    
    /**
     * 格式化历史记录用于提示词
     * @return 格式化的历史记录
     */
    private String formatHistoryForPrompt() {
        StringBuilder history = new StringBuilder();
        int index = 1;
        for (java.util.Map<String, String> message : conversationManager.getConversationHistory()) {
            history.append(index++).append(". ")
                   .append(message.get("role")).append(": ")
                   .append(message.get("content")).append("\n");
        }
        return history.toString();
    }
    
    /**
     * 获取对话管理器
     * @return 对话管理器
     */
    public ConversationManager getConversationManager() {
        return conversationManager;
    }
    
    /**
     * 检查是否正在执行任务
     * @return 是否正在执行
     */
    public boolean isExecuting() {
        return isExecuting.get();
    }
    
    /**
     * 获取当前执行步骤
     * @return 当前执行步骤
     */
    public int getCurrentStep() {
        return executionStep.get();
    }
    
    /**
     * 取消当前执行的任务
     */
    public void cancelExecution() {
        if (isExecuting.compareAndSet(true, false)) {
            conversationManager.clearHistory();
            executionStep.set(0);
        }
    }
    
    /**
     * 重置执行管理器
     */
    public void reset() {
        cancelExecution();
        conversationManager.clearHistory();
    }
    
    /**
     * 关闭资源
     */
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
    
                }