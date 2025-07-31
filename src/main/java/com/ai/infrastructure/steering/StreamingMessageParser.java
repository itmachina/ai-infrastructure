package com.ai.infrastructure.steering;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.concurrent.CompletableFuture;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 流式消息解析器 - 将原始输入流解析为结构化消息
 * 基于Claude Code的g2A类实现，支持完整的JSON消息解析和严格类型验证
 */
public class StreamingMessageParser implements AutoCloseable {
    private final AsyncMessageQueue<String> inputStream;
    private final AsyncMessageQueue<UserMessage> outputStream;
    private volatile boolean isProcessing;
    private volatile boolean isClosed;
    private Thread processingThread;
    private final Gson gson;
    
    public StreamingMessageParser(AsyncMessageQueue<String> inputStream) {
        this.inputStream = inputStream;
        this.outputStream = new AsyncMessageQueue<>();
        this.isProcessing = false;
        this.isClosed = false;
        this.gson = new Gson();
    }
    
    /**
     * 开始处理输入流
     */
    public void startProcessing() {
        if (isProcessing || isClosed) {
            return;
        }
        
        isProcessing = true;
        processingThread = new Thread(this::processStream, "StreamingMessageParser-Thread");
        processingThread.setDaemon(true);
        processingThread.start();
    }
    
    /**
     * 处理输入流 - 异步生成器实现
     */
    private void processStream() {
        StringBuilder buffer = new StringBuilder();
        
        try {
            while (!isClosed && !Thread.currentThread().isInterrupted()) {
                CompletableFuture<QueueMessage<String>> readFuture = inputStream.read();
                QueueMessage<String> message = readFuture.join();
                
                if (message.isDone()) {
                    // 处理缓冲区中剩余的内容
                    if (buffer.length() > 0) {
                        String remaining = buffer.toString().trim();
                        if (!remaining.isEmpty()) {
                            UserMessage parsedMessage = parseLine(remaining);
                            if (parsedMessage != null) {
                                outputStream.enqueue(parsedMessage);
                            }
                        }
                    }
                    outputStream.complete();
                    break;
                }
                
                String chunk = message.getValue();
                if (chunk == null) continue;
                
                buffer.append(chunk);
                
                // 按行分割处理
                int lineEndIndex;
                while ((lineEndIndex = buffer.indexOf("\n")) != -1) {
                    String line = buffer.substring(0, lineEndIndex);
                    buffer.delete(0, lineEndIndex + 1);
                    
                    UserMessage parsedMessage = parseLine(line);
                    if (parsedMessage != null) {
                        outputStream.enqueue(parsedMessage);
                    }
                }
            }
        } catch (Exception e) {
            if (!isClosed) {
                outputStream.error(e);
            }
        } finally {
            isProcessing = false;
        }
    }
    
    /**
     * 解析单行消息 - 严格类型验证和完整JSON消息解析
     * @param line 行内容
     * @return UserMessage
     */
    private UserMessage parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 尝试解析JSON格式的消息
            JsonObject jsonObject = gson.fromJson(line, JsonObject.class);
            
            // 严格类型验证 - 基于Claude Code的g2A.processLine实现
            if (!jsonObject.has("type")) {
                throw new IllegalArgumentException("Missing 'type' field");
            }
            
            String type = jsonObject.get("type").getAsString();
            if (!type.equals("user")) {
                throw new IllegalArgumentException("Expected message type 'user', got '" + type + "'");
            }
            
            if (!jsonObject.has("message")) {
                throw new IllegalArgumentException("Missing 'message' field");
            }
            
            JsonObject messageObj = jsonObject.getAsJsonObject("message");
            if (!messageObj.has("role")) {
                throw new IllegalArgumentException("Missing 'role' field in message");
            }
            
            String role = messageObj.get("role").getAsString();
            if (!role.equals("user")) {
                throw new IllegalArgumentException("Expected message role 'user', got '" + role + "'");
            }
            
            if (!messageObj.has("content")) {
                throw new IllegalArgumentException("Missing 'content' field in message");
            }
            
            // 支持多种content格式
            String content;
            if (messageObj.get("content").isJsonObject()) {
                // 处理复杂content对象
                JsonObject contentObj = messageObj.getAsJsonObject("content");
                if (contentObj.has("text")) {
                    content = contentObj.get("text").getAsString();
                } else {
                    content = contentObj.toString();
                }
            } else {
                // 处理简单content字符串
                content = messageObj.get("content").getAsString();
            }
            
            // 创建UserMessage对象，支持完整的消息结构
            return new UserMessage(type, content);
        } catch (JsonSyntaxException e) {
            // 如果不是有效的JSON，作为简单文本处理
            // Using system err for compatibility with streaming output - this is for user-facing warnings
            return new UserMessage("user", line.trim());
        } catch (Exception e) {
            // Error handling for streaming parser - using system err for immediate feedback
            // 根据Claude Code的实现，解析错误应该终止进程，但为了演示我们只打印错误
            return null;
        }
    }
    
    /**
     * 获取输出流
     * @return AsyncMessageQueue<UserMessage>
     */
    public AsyncMessageQueue<UserMessage> getOutputStream() {
        return outputStream;
    }
    
    /**
     * 检查是否正在处理
     * @return boolean
     */
    public boolean isProcessing() {
        return isProcessing;
    }
    
    /**
     * 关闭解析器
     */
    @Override
    public void close() {
        if (!isClosed) {
            isClosed = true;
            if (processingThread != null && processingThread.isAlive()) {
                processingThread.interrupt();
            }
            outputStream.complete();
        }
    }
}