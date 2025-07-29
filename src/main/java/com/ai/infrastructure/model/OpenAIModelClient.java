package com.ai.infrastructure.model;

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
 * OpenAI风格大模型客户端
 * 独立封装的心流开放平台API调用客户端
 */
public class OpenAIModelClient {
    private static final String API_URL = "https://apis.iflow.cn/v1/chat/completions";
    private static final String DEFAULT_MODEL = "Qwen3-235B-A22B-Thinking-2507";
    private static final int DEFAULT_MAX_TOKENS = 1000;
    private static final double DEFAULT_TEMPERATURE = 0.7;
    
    private final String apiKey;
    private final Gson gson;
    private String model;
    private int maxTokens;
    private double temperature;
    
    /**
     * 构造函数
     * @param apiKey API密钥
     */
    public OpenAIModelClient(String apiKey) {
        this.apiKey = apiKey;
        this.gson = new Gson();
        this.model = DEFAULT_MODEL;
        this.maxTokens = DEFAULT_MAX_TOKENS;
        this.temperature = DEFAULT_TEMPERATURE;
    }
    
    /**
     * 设置模型
     * @param model 模型名称
     * @return 当前实例
     */
    public OpenAIModelClient setModel(String model) {
        this.model = model;
        return this;
    }
    
    /**
     * 设置最大token数
     * @param maxTokens 最大token数
     * @return 当前实例
     */
    public OpenAIModelClient setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }
    
    /**
     * 设置温度参数
     * @param temperature 温度参数
     * @return 当前实例
     */
    public OpenAIModelClient setTemperature(double temperature) {
        this.temperature = temperature;
        return this;
    }
    
    /**
     * 调用大模型
     * @param prompt 用户提示
     * @return 模型响应
     */
    public String callModel(String prompt) {
        return callModel(prompt, "你是一个专业的AI助手，基于Claude Code技术构建。");
    }
    
    /**
     * 调用大模型（自定义系统消息）
     * @param prompt 用户提示
     * @param systemMessage 系统消息
     * @return 模型响应
     */
    public String callModel(String prompt, String systemMessage) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Error: OpenAI model API key not set";
        }
        
        try {
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统消息
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemMessage);
            messages.add(systemMsg);
            
            // 添加用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);
            
            // 调用模型
            return callModelWithMessages(messages);
        } catch (Exception e) {
            return "Error calling OpenAI model: " + e.getMessage();
        }
    }
    
    /**
     * 调用大模型（自定义消息列表）
     * @param messages 消息列表
     * @return 模型响应
     */
    public String callModelWithMessages(List<Map<String, String>> messages) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Error: OpenAI model API key not set";
        }
        
        try {
            // 构建请求体
            JsonObject requestBody = buildRequestBody(messages);
            
            // 发送API请求
            String response = sendApiRequest(requestBody);
            
            // 解析响应
            return parseModelResponse(response);
        } catch (Exception e) {
            return "Error calling OpenAI model: " + e.getMessage();
        }
    }
    
    /**
     * 构建请求体
     * @param messages 消息列表
     * @return 请求体对象
     */
    private JsonObject buildRequestBody(List<Map<String, String>> messages) {
        JsonObject requestBody = new JsonObject();
        
        // 设置模型
        requestBody.addProperty("model", model);
        
        // 转换消息列表为JSON数组
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
        requestBody.addProperty("temperature", temperature);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("stream", false);
        
        return requestBody;
    }
    
    /**
     * 发送API请求到OpenAI风格大模型
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
     * 解析模型API响应
     * @param response API响应
     * @return 模型回复内容
     */
    private String parseModelResponse(String response) {
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
            
            return "No response content found in model response";
        } catch (Exception e) {
            return "Error parsing model response: " + e.getMessage() + "\nResponse: " + response;
        }
    }
    
    /**
     * 获取当前配置的模型
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }
    
    /**
     * 获取当前配置的最大token数
     * @return 最大token数
     */
    public int getMaxTokens() {
        return maxTokens;
    }
    
    /**
     * 获取当前配置的温度参数
     * @return 温度参数
     */
    public double getTemperature() {
        return temperature;
    }
}