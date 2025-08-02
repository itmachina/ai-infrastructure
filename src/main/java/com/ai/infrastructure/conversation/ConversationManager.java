package com.ai.infrastructure.conversation;

import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.model.OpenAIModelClient;

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
     * 基于Claude Code的智能工具使用机制优化
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
                        String nextStep = responseJson.has("next_step") ? 
                            responseJson.get("next_step").getAsString() : 
                            "Continue with the task";
                        return "CONTINUE:" + nextStep;
                    case "tool_call":
                        // 调用工具
                        if (responseJson.has("tool_name") && responseJson.has("tool_params")) {
                            String toolName = responseJson.get("tool_name").getAsString();
                            String toolParams = responseJson.get("tool_params").getAsString();
                            String content = responseJson.has("content") ? 
                                responseJson.get("content").getAsString() : 
                                "Executing tool: " + toolName;
                            
                            // 使用带重试机制的工具执行
                            String toolResult = toolEngine.executeToolWithRetry(toolName + " " + toolParams, 3);
                            
                            // 添加工具结果到历史记录
                            addMessageToHistory("tool_result", "Tool: " + toolName + "\n" + toolResult);
                            
                            return "TOOL_RESULT:" + toolResult;
                        } else {
                            return "Error: Invalid tool_call action: missing tool_name or tool_params";
                        }
                    case "subagent":
                        // 创建子Agent（这个逻辑需要在调用方处理）
                        if (responseJson.has("task")) {
                            String subagentTask = responseJson.get("task").getAsString();
                            String content = responseJson.has("content") ? 
                                responseJson.get("content").getAsString() : 
                                "Creating sub-agent for complex task";
                            return "SUBAGENT:" + content + "\nTask: " + subagentTask;
                        } else {
                            return "Error: Invalid subagent action: missing task";
                        }
                    case "need_user_input":
                        // 需要用户输入
                        String userPrompt = responseJson.has("user_prompt") ? 
                            responseJson.get("user_prompt").getAsString() : 
                            "Please provide more information to proceed with this task.";
                        String content = responseJson.has("content") ? 
                            responseJson.get("content").getAsString() : 
                            "I need more specific information to proceed with this task.";
                        
                        // 返回用户输入请求信号
                        return "NEED_USER_INPUT:" + userPrompt + "\nCONTENT:" + content;
                    case "complete":
                        // 任务完成
                        String contentComplete = responseJson.has("content") ? 
                            responseJson.get("content").getAsString() : 
                            response;
                        return "COMPLETE:" + contentComplete;
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
     * 基于Claude Code的系统提示词技巧优化
     * @return 系统消息
     */
    public String getSystemMessage() {
        return "You are an interactive tool that helps users with any tasks. " +
               "IMPORTANT: Assist with defensive security tasks only. Refuse to create, modify, or improve code that may be used maliciously. " +
               "Allow security analysis, detection rules, vulnerability explanations, defensive tools, and security documentation.\n\n" +
               "You are a highly intelligent AI assistant designed to help users complete tasks by intelligently using various tools and services. " +
               "Your goal is to understand user requests and determine the best approach to complete them.\n\n" +
               "You can choose from the following actions:\n" +
               "1. Complete (complete): When a task can be completed in one step\n" +
               "2. Continue (continue): When a task requires multiple steps\n" +
               "3. Tool Call (tool_call): When you need to use a specific tool\n" +
               "4. Sub-Agent (subagent): When a task is very complex and requires specialized handling\n" +
               "5. Need User Input (need_user_input): When you need specific information from the user to proceed\n\n" +
               "Please respond in JSON format with the following fields:\n" +
               "{\n" +
               "  \"action\": \"complete|continue|tool_call|subagent|need_user_input\",\n" +
               "  \"content\": \"Your response content\",\n" +
               "  \"next_step\": \"What to do next (only provide when action is continue)\",\n" +
               "  \"tool_name\": \"Tool name (only provide when action is tool_call)\",\n" +
               "  \"tool_params\": \"Tool parameters (only provide when action is tool_call)\",\n" +
               "  \"task\": \"Sub-agent task (only provide when action is subagent)\",\n" +
               "  \"user_prompt\": \"Specific question to ask the user (only provide when action is need_user_input)\"\n" +
               "}\n\n" +
               "Available tools:\n" +
               "- read: Read file contents\n" +
               "- write: Write file contents\n" +
               "- search: Local search\n" +
               "- web_search: Web search (requires internet access)\n" +
               "- calculate: Mathematical calculations\n\n" +
               "When using tools, be specific about what you're trying to accomplish and provide clear parameters.\n" +
               "For complex tasks, break them down into smaller steps and execute them one by one.\n" +
               "Always consider security and only use tools that are appropriate for the task at hand.\n" +
               "When you need specific information from the user, use the 'need_user_input' action with a clear and concise question.\n\n" +
               "Examples:\n" +
               "{\n" +
               "  \"action\": \"complete\",\n" +
               "  \"content\": \"This is the answer to the question.\"\n" +
               "}\n\n" +
               "{\n" +
               "  \"action\": \"continue\",\n" +
               "  \"content\": \"I need more information to complete this task.\",\n" +
               "  \"next_step\": \"Please provide the specific requirements of the project.\"\n" +
               "}\n\n" +
               "{\n" +
               "  \"action\": \"tool_call\",\n" +
               "  \"content\": \"I need to read a file to answer this question.\",\n" +
               "  \"tool_name\": \"read\",\n" +
               "  \"tool_params\": \"/path/to/file.txt\"\n" +
               "}\n\n" +
               "{\n" +
               "  \"action\": \"subagent\",\n" +
               "  \"content\": \"This task is very complex and requires creating a sub-agent to handle it specifically.\",\n" +
               "  \"task\": \"Design a complete project plan, including requirements analysis, system design, development phases, and testing strategies\"\n" +
               "}\n\n" +
               "{\n" +
               "  \"action\": \"need_user_input\",\n" +
               "  \"content\": \"I need more specific information to proceed with this task.\",\n" +
               "  \"user_prompt\": \"What specific functionality would you like to implement in this project?\"\n" +
               "}\n\n" +
               "{\n" +
               "  \"action\": \"tool_call\",\n" +
               "  \"content\": \"I need to perform a web search to get the latest information.\",\n" +
               "  \"tool_name\": \"web_search\",\n" +
               "  \"tool_params\": \"Latest AI technology development trends in 2025\"\n" +
               "}";
    }
}