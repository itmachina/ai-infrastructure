package com.ai.infrastructure.conversation;

import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.model.OpenAIModelClient;
import com.ai.infrastructure.agent.SubAgent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

/**
 * 对话管理器，负责管理对话历史和持续执行逻辑
 * 基于Claude Code的对话管理机制实现
 */
public class ConversationManager {
    private Queue<Map<String, String>> conversationHistory;
    private static final int MAX_HISTORY_SIZE = 10; // 最大历史记录数
    private final Gson gson;
    
    public ConversationManager() {
        this.conversationHistory = new LinkedList<>();
        this.gson = new Gson();
    }
    
    /**
     * 添加消息到对话历史
     * @param role 角色 (system, user, assistant)
     * @param content 消息内容
     */
    public void addMessageToHistory(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        
        conversationHistory.offer(message);
        
        // 保持历史记录在最大数量限制内
        if (conversationHistory.size() > MAX_HISTORY_SIZE) {
            conversationHistory.poll();
        }
    }
    
    /**
     * 获取完整的对话历史
     * @return 对话历史列表
     */
    public List<Map<String, String>> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
    
    /**
     * 清除对话历史
     */
    public void clearHistory() {
        conversationHistory.clear();
    }
    
    /**
     * 获取历史记录数量
     * @return 历史记录数量
     */
    public int getHistorySize() {
        return conversationHistory.size();
    }
    
    /**
     * 构建包含历史记录的完整消息列表
     * @param systemMessage 系统消息
     * @param userMessage 用户消息
     * @return 完整的消息列表
     */
    public List<Map<String, String>> buildCompleteMessages(String systemMessage, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加系统消息
        if (systemMessage != null && !systemMessage.isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemMessage);
            messages.add(systemMsg);
        }
        
        // 添加历史记录
        messages.addAll(getConversationHistory());
        
        // 添加当前用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        
        return messages;
    }
    
    /**
     * 处理模型的持续执行响应
     * @param response 模型响应
     * @param toolEngine 工具引擎
     * @param openAIModelClient OpenAI模型客户端
     * @return 处理结果
     */
    public String processModelResponse(String response, ToolEngine toolEngine, OpenAIModelClient openAIModelClient) {
        try {
            // 添加模型响应到历史记录
            addMessageToHistory("assistant", response);
            
            // 解析模型响应，检查是否需要继续执行
            JsonObject responseJson = parseResponseForContinuation(response);
            
            if (responseJson.has("action")) {
                String action = responseJson.get("action").getAsString();
                
                switch (action) {
                    case "continue":
                        // 需要继续执行，返回继续信号
                        return "CONTINUE:" + responseJson.get("next_step").getAsString();
                    case "tool_call":
                        // 调用工具
                        if (responseJson.has("tool_name") && responseJson.has("tool_params")) {
                            String toolName = responseJson.get("tool_name").getAsString();
                            String toolParams = responseJson.get("tool_params").getAsString();
                            String toolResult = toolEngine.executeTool(toolName + " " + toolParams);
                            
                            // 添加工具结果到历史记录
                            addMessageToHistory("tool_result", toolResult);
                            
                            return "TOOL_RESULT:" + toolResult;
                        }
                        break;
                    case "subagent":
                        // 创建子Agent（这个逻辑需要在调用方处理）
                        if (responseJson.has("task")) {
                            String subagentTask = responseJson.get("task").getAsString();
                            return "SUBAGENT:" + subagentTask;
                        }
                        break;
                    case "complete":
                        // 任务完成
                        return "COMPLETE:" + response;
                    default:
                        // 默认情况，直接返回响应
                        return response;
                }
            }
            
            // 如果没有特定的动作指令，直接返回响应
            return response;
        } catch (Exception e) {
            return "Error processing model response: " + e.getMessage();
        }
    }
    
    /**
     * 解析模型响应以检查是否需要继续执行
     * @param response 模型响应
     * @return 解析后的JSON对象
     */
    private JsonObject parseResponseForContinuation(String response) {
        try {
            // 首先尝试解析为JSON格式
            if (response.trim().startsWith("{")) {
                return JsonParser.parseString(response).getAsJsonObject();
            }
            
            // 如果不是JSON格式，创建一个默认的响应对象
            JsonObject defaultResponse = new JsonObject();
            defaultResponse.addProperty("action", "complete");
            defaultResponse.addProperty("content", response);
            return defaultResponse;
        } catch (Exception e) {
            // 如果解析失败，创建一个默认的响应对象
            JsonObject defaultResponse = new JsonObject();
            defaultResponse.addProperty("action", "complete");
            defaultResponse.addProperty("content", response);
            return defaultResponse;
        }
    }
    
    /**
     * 构造系统消息，指导模型如何响应
     * @return 系统消息
     */
    public String getSystemMessage() {
        return "你是一个智能AI助手，需要根据任务要求决定如何响应。你可以选择以下几种行动：\n" +
               "1. 直接回答 (complete): 当任务可以一次性完成时\n" +
               "2. 继续执行 (continue): 当任务需要多步骤完成时\n" +
               "3. 调用工具 (tool_call): 当需要使用特定工具时\n" +
               "4. 创建子Agent (subagent): 当任务非常复杂需要专门处理时\n\n" +
               "请以JSON格式回复，包含以下字段:\n" +
               "{\n" +
               "  \"action\": \"complete|continue|tool_call|subagent\",\n" +
               "  \"content\": \"你的回复内容\",\n" +
               "  \"next_step\": \"下一步要做什么（仅在action为continue时提供）\",\n" +
               "  \"tool_name\": \"工具名称（仅在action为tool_call时提供）\",\n" +
               "  \"tool_params\": \"工具参数（仅在action为tool_call时提供）\",\n" +
               "  \"task\": \"子Agent任务（仅在action为subagent时提供）\"\n" +
               "}\n\n" +
               "可用工具列表:\n" +
               "- read: 读取文件内容\n" +
               "- write: 写入文件内容\n" +
               "- search: 本地搜索\n" +
               "- web_search: 网页搜索（需要互联网访问）\n" +
               "- calculate: 数学计算\n\n" +
               "示例:\n" +
               "{\n" +
               "  \"action\": \"complete\",\n" +
               "  \"content\": \"这是问题的答案。\"\n" +
               "}\n\n" +
               "{\n" +
               "  \"action\": \"continue\",\n" +
               "  \"content\": \"我需要更多信息来完成这个任务。\",\n" +
               "  \"next_step\": \"请提供项目的具体要求。\"\n" +
               "}\n\n" +
               "{\n" +
               "  \"action\": \"tool_call\",\n" +
               "  \"content\": \"我需要读取一个文件来回答这个问题。\",\n" +
               "  \"tool_name\": \"read\",\n" +
               "  \"tool_params\": \"/path/to/file.txt\"\n" +
               "}\n\n" +
               "示例3:\n" +
               "{\n" +
               "  \"action\": \"subagent\",\n" +
               "  \"content\": \"这个任务很复杂，需要创建一个子Agent来专门处理。\",\n" +
               "  \"task\": \"设计一个完整的项目计划，包括需求分析、系统设计、开发阶段和测试策略\"\n" +
               "}\n\n" +
               "示例4:\n" +
               "{\n" +
               "  \"action\": \"tool_call\",\n" +
               "  \"content\": \"我需要进行网页搜索来获取最新信息。\",\n" +
               "  \"tool_name\": \"web_search\",\n" +
               "  \"tool_params\": \"2025年最新的人工智能技术发展趋势\"\n" +
               "}";
    }
}