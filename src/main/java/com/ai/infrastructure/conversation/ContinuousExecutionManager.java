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
    private static final int MAX_EXECUTION_STEPS = 10; // 最大执行步骤数
    
    // 需要用户输入的关键词列表（中英文）
    private static final String[] USER_INPUT_KEYWORDS = {
        "请提供", "需要您", "请输入", "请告诉我", "请说明", "请上传", "请明确", "请确认",
        "具体信息", "详细信息", "更多信息", "补充信息",
        "项目需求", "具体要求", "详细要求", "需求说明",
        "文件内容", "代码内容", "文本内容",
        "please provide", "need you to", "please input", "please tell me", "please specify", "please upload",
        "specific information", "detailed information", "more information", "additional information",
        "project requirements", "specific requirements", "detailed requirements", "requirements specification",
        "file content", "code content", "text content"
    };
    
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
                // 需要继续执行，智能判断是否需要用户输入
                String nextStep = processedResponse.substring(9); // 移除"CONTINUE:"前缀
                logger.info("CONTINUE, nextStep:{}", nextStep);
                
                // 检查是否真的需要用户输入新信息
                // 如果nextStep包含特定关键词，才提示用户输入
                if (needsUserInput(nextStep)) {
                    // 提示用户输入更多信息
                    System.out.println("AI需要更多信息来继续执行任务: " + nextStep);
                    System.out.print("请输入您的回复: ");
                    
                    // 从命令行读取用户输入
                    String userInput = scanner.nextLine();
                    
                    // 将用户输入添加到历史记录中
                    conversationManager.addMessageToHistory("user", userInput);
                    
                    // 继续执行任务
                    return executeTaskStep(userInput);
                } else {
                    // 不需要用户输入，直接继续执行
                    logger.info("Continuing execution without user input: {}", nextStep);
                    return executeTaskStep("基于上下文继续执行任务: " + nextStep);
                }
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
    
    /**
     * 智能判断是否需要用户输入 - 改进版
     * 基于上下文分析和模型意图识别，更准确地判断是否需要用户输入
     * @param nextStep 下一步操作描述
     * @return 是否需要用户输入
     */
    private boolean needsUserInput(String nextStep) {
        // 转换为小写以便进行不区分大小写的匹配
        String lowerNextStep = nextStep.toLowerCase();
        
        // 首先检查是否明确要求用户输入
        for (String keyword : USER_INPUT_KEYWORDS) {
            // 对于英文关键词，使用小写进行匹配
            if (keyword.matches(".*[a-zA-Z].*")) {
                if (lowerNextStep.contains(keyword.toLowerCase())) {
                    // 进一步检查上下文，避免重复询问
                    if (!isInformationAlreadyProvided(keyword)) {
                        return true;
                    }
                }
            } else {
                // 对于中文关键词，使用原始大小写进行匹配
                if (nextStep.contains(keyword)) {
                    // 进一步检查上下文，避免重复询问
                    if (!isInformationAlreadyProvided(keyword)) {
                        return true;
                    }
                }
            }
        }
        
        // 检查是否是开放式问题（以问号结尾）
        if (nextStep.trim().endsWith("?")) {
            // 检查问题是否已经在历史记录中被回答过
            if (!isQuestionAlreadyAnswered(nextStep)) {
                return true;
            }
        }
        
        // 检查是否是确认类请求
        if (isConfirmationRequest(nextStep)) {
            return true;
        }
        
        // 如果没有明确要求用户提供信息，则认为不需要用户输入
        return false;
    }
    
    /**
     * 检查所需信息是否已经在对话历史中提供过
     * @param keyword 关键词
     * @return 是否已提供
     */
    private boolean isInformationAlreadyProvided(String keyword) {
        // 检查对话历史中是否已经包含了相关信息
        for (Map<String, String> message : conversationManager.getConversationHistory()) {
            if ("user".equals(message.get("role"))) {
                String content = message.get("content").toLowerCase();
                if (content.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 检查问题是否已经在历史记录中被回答过
     * @param question 问题
     * @return 是否已回答
     */
    private boolean isQuestionAlreadyAnswered(String question) {
        String lowerQuestion = question.toLowerCase().trim();
        // 移除问号以便比较
        if (lowerQuestion.endsWith("?")) {
            lowerQuestion = lowerQuestion.substring(0, lowerQuestion.length() - 1);
        }
        
        // 检查对话历史中是否已经有相关的问答
        List<Map<String, String>> history = conversationManager.getConversationHistory();
        for (int i = 0; i < history.size() - 1; i++) {
            Map<String, String> message = history.get(i);
            if ("user".equals(message.get("role"))) {
                String content = message.get("content").toLowerCase().trim();
                // 移除问号以便比较
                if (content.endsWith("?")) {
                    content = content.substring(0, content.length() - 1);
                }
                
                // 检查问题是否相似
                if (content.contains(lowerQuestion) || lowerQuestion.contains(content)) {
                    // 检查下一个消息是否是助手的回答
                    if (i + 1 < history.size()) {
                        Map<String, String> nextMessage = history.get(i + 1);
                        if ("assistant".equals(nextMessage.get("role")) || "tool_result".equals(nextMessage.get("role"))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 判断是否是确认类请求
     * @param nextStep 下一步操作描述
     * @return 是否是确认类请求
     */
    private boolean isConfirmationRequest(String nextStep) {
        String lowerNextStep = nextStep.toLowerCase();
        return lowerNextStep.contains("确认") || lowerNextStep.contains("confirm") || 
               lowerNextStep.contains("是否") || lowerNextStep.contains("是否同意") ||
               lowerNextStep.contains("你觉得") || lowerNextStep.contains("你认为") ||
               lowerNextStep.contains("do you think") || lowerNextStep.contains("what do you think");
    }
}