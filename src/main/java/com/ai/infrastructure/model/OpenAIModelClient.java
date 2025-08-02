package com.ai.infrastructure.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 基于Claude Code的yj函数实现增强的交互系统
 */
public class OpenAIModelClient {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIModelClient.class);
    
    private static final String API_URL = "https://apis.iflow.cn/v1/chat/completions";
    private static final String DEFAULT_MODEL = "Qwen3-235B-A22B-Thinking-2507";
    private static final int DEFAULT_MAX_TOKENS = 1000;
    private static final double DEFAULT_TEMPERATURE = 0.7;
    
    private final String apiKey;
    private final Gson gson;
    private String model;
    private int maxTokens;
    private double temperature;
    
    // 调用统计信息
    private int totalCalls = 0;
    private int successfulCalls = 0;
    private int failedCalls = 0;
    private long totalResponseTime = 0;
    private int retryCountTotal = 0;
    
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
        
        // 初始化统计信息
        this.totalCalls = 0;
        this.successfulCalls = 0;
        this.failedCalls = 0;
        this.totalResponseTime = 0;
        this.retryCountTotal = 0;
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
        // 基于Claude Code的核心身份声明
        String systemMessage = "You are an interactive tool that helps users with any tasks. " +
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
        return callModel(prompt, systemMessage);
    }
    
    /**
     * 调用大模型（自定义系统消息）
     * @param prompt 用户提示
     * @param systemMessage 系统消息
     * @return 模型响应
     */
    public String callModel(String prompt, String systemMessage) {
        return callModelWithRetry(prompt, systemMessage, 3); // 默认重试3次
    }
    
    /**
     * 调用大模型（自定义系统消息，带重试机制）
     * 基于Claude Code的工具执行增强实现
     * 增强版本，提供更完善的错误处理和重试机制
     * @param prompt 用户提示
     * @param systemMessage 系统消息
     * @param maxRetries 最大重试次数
     * @return 模型响应
     */
    public String callModelWithRetry(String prompt, String systemMessage, int maxRetries) {
        int retryCount = 0;
        Exception lastException = null;
        
        // 更新调用统计
        totalCalls++;
        
        // 记录模型调用开始
        long startTime = System.currentTimeMillis();
//        logger.info("Starting model call with prompt: {}", prompt);
        
        // 检查API密钥
        if (apiKey == null || apiKey.isEmpty()) {
            String error = "Error: OpenAI model API key not set";
            logger.error("API key error: {}", error);
            failedCalls++;
            return error;
        }
        
        while (retryCount <= maxRetries) {
            try {
                // 记录当前尝试次数
                if (retryCount > 0) {
                    logger.info("Retrying model call (attempt {} of {})", (retryCount + 1), (maxRetries + 1));
                    retryCountTotal++;
                }
                
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
                String result = callModelWithMessages(messages);
                logger.info("Model call result: {}", result);
                // 检查结果是否包含错误
                if (result.startsWith("Error:")) {
                    // 记录错误信息
                    logger.error("Model call returned error: {}", result);
                    
                    // 根据错误类型决定是否重试
                    ModelCallErrorType errorType = classifyError(result);
                    switch (errorType) {
                        case CRITICAL:
                            logger.error("Critical error detected, stopping retries: {}", result);
                            failedCalls++;
                            return result;
                        case TRANSIENT:
                            // 瞬时错误可以重试
                            if (retryCount < maxRetries) {
                                retryCount++;
                                long delay = calculateRetryDelay(retryCount, errorType);
                                logger.info("Scheduling retry in {}ms for transient error", delay);
                                Thread.sleep(delay);
                                continue;
                            } else {
                                logger.error("Max retries reached for transient error, giving up on model call");
                                failedCalls++;
                            }
                            break;
                        case RATE_LIMIT:
                            // 速率限制错误需要更长的延迟
                            if (retryCount < maxRetries) {
                                retryCount++;
                                long delay = calculateRetryDelay(retryCount, errorType);
                                logger.info("Scheduling retry in {}ms for rate limit error", delay);
                                Thread.sleep(delay);
                                continue;
                            } else {
                                logger.error("Max retries reached for rate limit error, giving up on model call");
                                failedCalls++;
                            }
                            break;
                        case UNKNOWN:
                            // 未知错误可以重试
                            if (retryCount < maxRetries) {
                                retryCount++;
                                long delay = calculateRetryDelay(retryCount, errorType);
                                logger.info("Scheduling retry in {}ms for unknown error", delay);
                                Thread.sleep(delay);
                                continue;
                            } else {
                                logger.error("Max retries reached for unknown error, giving up on model call");
                                failedCalls++;
                            }
                            break;
                    }
                } else {
                    // 记录成功执行
                    long endTime = System.currentTimeMillis();
                    long responseTime = endTime - startTime;
                    totalResponseTime += responseTime;
                    successfulCalls++;
                    
                    logger.info("Model call executed successfully after {} attempts", (retryCount + 1));
                    logger.info("Response time: {}ms", responseTime);
                    return result;
                }
                
                return result;
            } catch (InterruptedException ie) {
                // 处理中断异常
                logger.error("Model call interrupted: {}", ie.getMessage());
                Thread.currentThread().interrupt();
                failedCalls++;
                return "Error: Model call interrupted";
            } catch (Exception e) {
                lastException = e;
                logger.error("Exception during model call (attempt {}): {}", (retryCount + 1), e.getMessage());
                
                // 根据异常类型决定是否重试
                if (shouldRetryOnException(e) && retryCount < maxRetries) {
                    retryCount++;
                    retryCountTotal++;
                    try {
                        long delay = calculateRetryDelay(retryCount, ModelCallErrorType.UNKNOWN);
                        logger.info("Scheduling retry in {}ms due to exception", delay);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        logger.error("Retry interrupted: {}", ie.getMessage());
                        Thread.currentThread().interrupt();
                        failedCalls++;
                        break;
                    }
                } else {
                    logger.error("Max retries reached after exception, giving up on model call");
                    failedCalls++;
                    break;
                }
            }
        }
        
        String errorMessage = "Error calling OpenAI model after " + maxRetries + " retries: " + 
               (lastException != null ? lastException.getMessage() : "Unknown error");
        logger.error("{}", errorMessage);
        return errorMessage;
    }
    
    /**
     * 错误类型分类
     */
    private enum ModelCallErrorType {
        CRITICAL,    // 关键错误，不应重试
        TRANSIENT,   // 瞬时错误，可以重试
        RATE_LIMIT,  // 速率限制错误
        UNKNOWN      // 未知错误
    }
    
    /**
     * 分类错误类型
     * @param error 错误消息
     * @return 错误类型
     */
    private ModelCallErrorType classifyError(String error) {
        String lowerError = error.toLowerCase();
        
        // 关键错误，不应重试
        if (lowerError.contains("api key") || lowerError.contains("security") || 
            lowerError.contains("unauthorized") || lowerError.contains("forbidden")) {
            return ModelCallErrorType.CRITICAL;
        }
        
        // 速率限制错误
        if (lowerError.contains("rate limit") || lowerError.contains("too many requests") ||
            lowerError.contains("429")) {
            return ModelCallErrorType.RATE_LIMIT;
        }
        
        // 瞬时错误
        if (lowerError.contains("timeout") || lowerError.contains("connection") ||
            lowerError.contains("network") || lowerError.contains("502") ||
            lowerError.contains("503") || lowerError.contains("504")) {
            return ModelCallErrorType.TRANSIENT;
        }
        
        // 默认为未知错误
        return ModelCallErrorType.UNKNOWN;
    }
    
    /**
     * 计算重试延迟
     * 基于Claude Code的智能重试策略优化
     * @param retryCount 重试次数
     * @param errorType 错误类型
     * @return 延迟时间（毫秒）
     */
    private long calculateRetryDelay(int retryCount, ModelCallErrorType errorType) {
        // 基础延迟时间
        long baseDelay = 1000;
        
        // 根据错误类型调整延迟
        switch (errorType) {
            case RATE_LIMIT:
                // 速率限制错误需要更长的延迟，可能包含服务器建议的重试时间
                baseDelay = 5000;
                break;
            case TRANSIENT:
                // 瞬时错误使用指数退避
                baseDelay = 1000;
                break;
            case UNKNOWN:
                // 未知错误使用中等延迟
                baseDelay = 2000;
                break;
            default:
                baseDelay = 1000;
        }
        
        // 指数退避 (2的幂次增长)
        long delay = baseDelay * (1L << Math.min(retryCount - 1, 4)); // 最多16倍延迟
        
        // 添加随机抖动以避免惊群效应 (±25%的抖动)
        double jitter = 0.75 + Math.random() * 0.5; // 0.75到1.25之间的随机数
        delay = (long) (delay * jitter);
        
        // 根据错误类型进一步调整延迟
        switch (errorType) {
            case RATE_LIMIT:
                // 对于速率限制错误，确保至少有5秒的延迟
                delay = Math.max(delay, 5000);
                break;
            case CRITICAL:
                // 关键错误不应该重试，但如果有，使用较短延迟
                delay = Math.min(delay, 1000);
                break;
            case TRANSIENT:
            case UNKNOWN:
            default:
                // 其他错误使用计算的延迟
                break;
        }
        
        // 限制最大延迟
        long maxDelay = 60000; // 最多60秒
        return Math.min(delay, maxDelay);
    }
    
    /**
     * 判断是否应该在异常情况下重试
     * @param e 异常
     * @return 是否应该重试
     */
    private boolean shouldRetryOnException(Exception e) {
        // 网络相关异常可以重试
        if (e instanceof java.net.SocketTimeoutException ||
            e instanceof java.net.ConnectException ||
            e instanceof java.net.UnknownHostException) {
            return true;
        }
        
        // IO异常可以重试
        if (e instanceof java.io.IOException) {
            return true;
        }
        
        // 其他异常默认不重试
        return false;
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
            logger.info("Sending request to OpenAI model request:{}, response:{}", requestBody, response);
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
     * 增强版本，添加超时控制和更详细的错误处理
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
        
        // 设置超时控制
        connection.setConnectTimeout(30000); // 30秒连接超时
        connection.setReadTimeout(60000);    // 60秒读取超时
        
        try {
            // 发送请求体
            String jsonInputString = gson.toJson(requestBody);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // 读取响应
            int responseCode = connection.getResponseCode();
            
            // 根据响应码处理不同情况
            if (responseCode == 200) {
                // 成功响应
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
                return response.toString();
            } else {
                // 错误响应
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                }
                
                // 根据响应码生成更详细的错误信息
                String errorMessage = parseErrorResponse(responseCode, errorResponse.toString());
                throw new RuntimeException("API request failed with response code: " + responseCode + ". " + errorMessage);
            }
        } catch (java.net.SocketTimeoutException e) {
            throw new RuntimeException("API request timed out: " + e.getMessage());
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("Failed to connect to API: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("API request failed: " + e.getMessage());
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * 解析错误响应
     * @param responseCode 响应码
     * @param errorResponse 错误响应内容
     * @return 解析后的错误信息
     */
    private String parseErrorResponse(int responseCode, String errorResponse) {
        try {
            JsonObject errorObject = JsonParser.parseString(errorResponse).getAsJsonObject();
            if (errorObject.has("error") && !errorObject.get("error").isJsonNull()) {
                JsonObject error = errorObject.getAsJsonObject("error");
                if (error.has("message") && !error.get("message").isJsonNull()) {
                    return error.get("message").getAsString();
                }
            }
        } catch (Exception e) {
            // 如果解析失败，返回原始错误响应
            return "Error response: " + errorResponse;
        }
        
        // 默认返回响应码信息
        switch (responseCode) {
            case 400:
                return "Bad Request - The request was invalid or cannot be served.";
            case 401:
                return "Unauthorized - API key is invalid or missing.";
            case 403:
                return "Forbidden - The request is understood but refused.";
            case 404:
                return "Not Found - The requested resource could not be found.";
            case 429:
                return "Too Many Requests - Rate limit exceeded.";
            case 500:
                return "Internal Server Error - Something went wrong on the server.";
            case 502:
                return "Bad Gateway - The server received an invalid response.";
            case 503:
                return "Service Unavailable - The server is temporarily unavailable.";
            case 504:
                return "Gateway Timeout - The server took too long to respond.";
            default:
                return "Unknown error with response code: " + responseCode;
        }
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
     * 获取模型调用统计信息
     * @return 统计信息字符串
     */
    public String getCallStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Model Call Statistics:\n");
        stats.append("  Total calls: ").append(totalCalls).append("\n");
        stats.append("  Successful calls: ").append(successfulCalls).append("\n");
        stats.append("  Failed calls: ").append(failedCalls).append("\n");
        stats.append("  Retry count total: ").append(retryCountTotal).append("\n");
        
        if (totalCalls > 0) {
            double successRate = (double) successfulCalls / totalCalls * 100;
            stats.append("  Success rate: ").append(String.format("%.2f", successRate)).append("%\n");
        }
        
        if (successfulCalls > 0) {
            long averageResponseTime = totalResponseTime / successfulCalls;
            stats.append("  Average response time: ").append(averageResponseTime).append("ms\n");
        }
        
        return stats.toString();
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalCalls = 0;
        successfulCalls = 0;
        failedCalls = 0;
        totalResponseTime = 0;
        retryCountTotal = 0;
    }
}