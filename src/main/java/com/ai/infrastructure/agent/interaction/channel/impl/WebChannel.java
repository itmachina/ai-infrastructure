package com.ai.infrastructure.agent.interaction.channel.impl;

import com.ai.infrastructure.agent.interaction.channel.InteractionChannel;
import com.ai.infrastructure.agent.interaction.model.Message;
import com.ai.infrastructure.agent.interaction.model.ChannelType;
import com.ai.infrastructure.agent.interaction.model.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web渠道适配器
 */
public class WebChannel implements InteractionChannel {
    private static final Logger logger = LoggerFactory.getLogger(WebChannel.class);
    
    private final String endpoint;
    private final Map<String, Object> parameters;
    private final Map<String, Message> pendingMessages = new ConcurrentHashMap<>();
    private volatile boolean connected = false;
    private volatile long lastActivityTime = System.currentTimeMillis();
    private volatile int messageCount = 0;
    private volatile String lastError = null;
    
    public WebChannel(String channelId, Map<String, Object> config) {
        String endpoint = (String) config.get("endpoint");
        this.endpoint = endpoint != null ? endpoint : "http://localhost:8080";
        this.parameters = new HashMap<>(config);
        
        // 验证必要配置
        if (this.endpoint == null || this.endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Web endpoint cannot be null or empty");
        }
    }
    
    @Override
    public SendResult sendMessage(Message message) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Sending message via Web channel: {}", message);
            
            // 模拟Web消息发送
            if (!connected) {
                logger.warn("Web channel is not connected");
                return SendResult.failure("Channel not connected", System.currentTimeMillis() - startTime);
            }
            
            // 验证消息内容
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return SendResult.failure("Empty message content", System.currentTimeMillis() - startTime);
            }
            
            // 添加Web特定的元数据
            message.addMetadata("endpoint", endpoint);
            message.addMetadata("contentType", "text/html");
            message.addMetadata("charset", "UTF-8");
            
            // 模拟发送延迟
            Thread.sleep(100);
            
            // 更新状态
            message.markAsSent();
            pendingMessages.put(message.getId(), message);
            messageCount++;
            lastActivityTime = System.currentTimeMillis();
            lastError = null;
            
            logger.info("Message sent successfully via Web channel: {}", message.getId());
            return SendResult.success(message.getId(), System.currentTimeMillis() - startTime);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while sending message via Web channel", e);
            return SendResult.failure("Interrupted", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("Failed to send message via Web channel", e);
            lastError = e.getMessage();
            return SendResult.failure(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public Message receiveMessage() {
        try {
            // 模拟从Web端接收消息
            Thread.sleep(50);
            
            // 检查是否有待处理的消息
            if (pendingMessages.isEmpty()) {
                return null;
            }
            
            // 获取第一个消息
            Message message = pendingMessages.values().iterator().next();
            pendingMessages.remove(message.getId());
            
            message.setStatus(MessageStatus.DELIVERED);
            lastActivityTime = System.currentTimeMillis();
            
            logger.debug("Message received via Web channel: {}", message);
            return message;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while receiving message via Web channel", e);
            return null;
        } catch (Exception e) {
            logger.error("Failed to receive message via Web channel", e);
            return null;
        }
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public ChannelType getChannelType() {
        return ChannelType.WEB;
    }
    
    @Override
    public void connect() {
        if (!connected) {
            connected = true;
            logger.info("Web channel connected to: {}", endpoint);
            
            // 模拟连接建立
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @Override
    public void disconnect() {
        if (connected) {
            connected = false;
            pendingMessages.clear();
            logger.info("Web channel disconnected from: {}", endpoint);
        }
    }
    
    @Override
    public ChannelConfig getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("endpoint", endpoint);
        config.put("timeout", parameters.get("timeout"));
        config.put("retryCount", parameters.get("retryCount"));
        config.put("authToken", parameters.get("authToken"));
        
        return new ChannelConfig("WebChannel", endpoint, config);
    }
    
    @Override
    public boolean validateConfig() {
        return endpoint != null && !endpoint.trim().isEmpty();
    }
    
    @Override
    public ChannelStatus getStatus() {
        return new ChannelStatus(connected, lastActivityTime, messageCount, lastError);
    }
}