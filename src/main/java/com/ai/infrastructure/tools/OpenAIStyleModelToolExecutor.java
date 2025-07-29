package com.ai.infrastructure.tools;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * OpenAI风格大模型工具执行器
 * 支持调用心流开放平台的OpenAI兼容API接口
 */
public class OpenAIStyleModelToolExecutor implements ToolExecutor {
    private static final String API_URL = "https://apis.iflow.cn/v1/chat/completions";
    private static final String DEFAULT_MODEL = "Qwen3-235B-A22B-Thinking-2507";
    private static final int DEFAULT_MAX_TOKENS = 1000;
    private static final double DEFAULT_TEMPERATURE = 0.7;
    
    private final String apiKey;
    private final Gson gson;
    
    public OpenAIStyleModelToolExecutor(String apiKey) {
        this.apiKey = apiKey;
        this.gson = new Gson();
    }
    
    /**
     * 执行工具调用
     * @param task 任务描述
     * @return 执行结果
     */
    @Override
    public String execute(String task) {
        try {
            // 解析任务参数
            JsonObject taskParams = parseTaskParameters(task);
            
            // 构建请求消息
            List<Map<String, String>> messages = buildMessages(taskParams);
            
            // 构建请求体
            JsonObject requestBody = buildRequestBody(taskParams, messages);
            
            // 发送API请求
            String response = sendApiRequest(requestBody);
            
            // 解析响应
            return parseResponse(response);
        } catch (Exception e) {
            return "Error executing OpenAI style model tool: " + e.getMessage();
        }
    }
    
    /**
     * 解析任务参数
     * @param task 任务描述
     * @return 参数对象
     */
    private JsonObject parseTaskParameters(String task) {
        try {
            // 尝试解析JSON格式的任务参数
            if (task.trim().startsWith("{")) {
                return JsonParser.parseString(task).getAsJsonObject();
            }
            
            // 默认情况下，将任务作为用户消息处理
            JsonObject params = new JsonObject();
            params.addProperty("prompt", task);
            return params;
        } catch (Exception e) {
            JsonObject params = new JsonObject();
            params.addProperty("prompt", task);
            return params;
        }
    }
    
    /**
     * 构建消息列表
     * @param params 参数对象
     * @return 消息列表
     */
    private List<Map<String, String>> buildMessages(JsonObject params) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 添加系统消息（如果提供）
        if (params.has("system_message")) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", params.get("system_message").getAsString());
            messages.add(systemMessage);
        }
        
        // 添加用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        if (params.has("prompt")) {
            userMessage.put("content", params.get("prompt").getAsString());
        } else {
            userMessage.put("content", "请回答问题");
        }
        messages.add(userMessage);
        
        return messages;
    }
    
    /**
     * 构建请求体
     * @param params 参数对象
     * @param messages 消息列表
     * @return 请求体对象
     */
    private JsonObject buildRequestBody(JsonObject params, List<Map<String, String>> messages) {
        JsonObject requestBody = new JsonObject();
        
        // 设置模型
        requestBody.addProperty("model", 
            params.has("model") ? params.get("model").getAsString() : DEFAULT_MODEL);
        
        // 设置消息
        JsonObject[] messagesArray = messages.stream()
            .map(msg -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("role", msg.get("role"));
                obj.addProperty("content", msg.get("content"));
                return obj;
            })
            .toArray(JsonObject[]::new);
        requestBody.add("messages", gson.toJsonTree(messagesArray));
        
        // 设置其他参数
        requestBody.addProperty("temperature", 
            params.has("temperature") ? params.get("temperature").getAsDouble() : DEFAULT_TEMPERATURE);
        requestBody.addProperty("max_tokens", 
            params.has("max_tokens") ? params.get("max_tokens").getAsInt() : DEFAULT_MAX_TOKENS);
        
        // 设置流式响应（默认为false）
        requestBody.addProperty("stream", 
            params.has("stream") ? params.get("stream").getAsBoolean() : false);
        
        return requestBody;
    }
    
    /**
     * 发送API请求
     * @param requestBody 请求体
     * @return API响应
     * @throws Exception 网络或API错误
     */
    private String sendApiRequest(JsonObject requestBody) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置请求方法和头部
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        
        // 发送请求体
        String jsonInputString = gson.toJson(requestBody);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // 读取响应
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("API request failed with response code: " + responseCode);
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        return response.toString();
    }
    
    /**
     * 解析API响应
     * @param response API响应
     * @return 模型回复内容
     */
    private String parseResponse(String response) {
        try {
            JsonObject responseObject = JsonParser.parseString(response).getAsJsonObject();
            
            if (responseObject.has("choices") && !responseObject.get("choices").isJsonNull()) {
                JsonArray choices = responseObject.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    if (choice.has("message") && !choice.get("message").isJsonNull()) {
                        JsonObject message = choice.getAsJsonObject("message");
                        if (message.has("content") && !message.get("content").isJsonNull()) {
                            return message.get("content").getAsString();
                        }
                    }
                }
            }
            
            return "No response content found in API response";
        } catch (Exception e) {
            return "Error parsing API response: " + e.getMessage() + "\nResponse: " + response;
        }
    }
}