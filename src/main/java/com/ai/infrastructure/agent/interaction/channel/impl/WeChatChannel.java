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
 * 微信渠道适配器
 */
public class WeChatChannel implements InteractionChannel {
    private static final Logger logger = LoggerFactory.getLogger(WeChatChannel.class);
    
    private final String appId;
    private final String appSecret;
    private final String accessToken;
    private final String openId;
    private final Map<String, Object> parameters;
    private final Map<String, Message> pendingMessages = new ConcurrentHashMap<>();
    private volatile boolean connected = false;
    private volatile long lastActivityTime = System.currentTimeMillis();
    private volatile int messageCount = 0;
    private volatile String lastError = null;
    
    public WeChatChannel(String channelId, Map<String, Object> config) {
        this.appId = (String) config.get("appid");
        this.appSecret = (String) config.get("secret");
        this.accessToken = (String) config.get("accessToken");
        this.openId = (String) config.get("openId");
        this.parameters = new HashMap<>(config);
        
        // 验证必要配置
        if (this.appId == null || this.appId.trim().isEmpty()) {
            throw new IllegalArgumentException("WeChat appid cannot be null or empty");
        }
        if (this.openId == null || this.openId.trim().isEmpty()) {
            throw new IllegalArgumentException("WeChat openId cannot be null or empty");
        }
        
        // 如果没有accessToken，尝试获取
        if (this.accessToken == null || this.accessToken.trim().isEmpty()) {
            this.accessToken = ""; // 实际项目中这里应该调用微信API获取token
        }
    }
    
    @Override
    public SendResult sendMessage(Message message) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Sending message via WeChat channel: {}", message);
            
            if (!connected) {
                logger.warn("WeChat channel is not connected");
                return SendResult.failure("Channel not connected", System.currentTimeMillis() - startTime);
            }
            
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return SendResult.failure("Empty message content", System.currentTimeMillis() - startTime);
            }
            
            // 添加微信特定的元数据
            message.addMetadata("appId", appId);
            message.addMetadata("openId", openId);
            message.addMetadata("msgType", determineMessageType(message));
            message.addMetadata("accessToken", accessToken);
            
            // 模拟微信API调用
            Thread.sleep(200);
            
            message.markAsSent();
            pendingMessages.put(message.getId(), message);
            messageCount++;
            lastActivityTime = System.currentTimeMillis();
            lastError = null;
            
            logger.info("Message sent successfully via WeChat channel: {}", message.getId());
            return SendResult.success(message.getId(), System.currentTimeMillis() - startTime);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while sending message via WeChat channel", e);
            return SendResult.failure("Interrupted", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("Failed to send message via WeChat channel", e);
            lastError = e.getMessage();
            return SendResult.failure(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public Message receiveMessage() {
        try {
            // 模拟微信事件接收
            Thread.sleep(120);
            
            if (pendingMessages.isEmpty()) {
                return null;
            }
            
            Message message = pendingMessages.values().iterator().next();
            pendingMessages.remove(message.getId());
            
            message.setStatus(MessageStatus.DELIVERED);
            lastActivityTime = System.currentTimeMillis();
            
            logger.debug("Message received via WeChat channel: {}", message);
            return message;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while receiving message via WeChat channel", e);
            return null;
        } catch (Exception e) {
            logger.error("Failed to receive message via WeChat channel", e);
            return null;
        }
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public ChannelType getChannelType() {
        return ChannelType.WECHAT;
    }
    
    @Override
    public void connect() {
        if (!connected) {
            connected = true;
            logger.info("WeChat channel connected - AppId: {}, OpenId: {}", appId, openId);
            
            try {
                Thread.sleep(400);
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
            logger.info("WeChat channel disconnected - AppId: {}, OpenId: {}", appId, openId);
        }
    }
    
    @Override
    public ChannelConfig getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("appId", appId);
        config.put("appSecret", appSecret);
        config.put("accessToken", accessToken);
        config.put("openId", openId);
        config.put("timeout", parameters.get("timeout"));
        config.put("retryCount", parameters.get("retryCount"));
        
        return new ChannelConfig("WeChatChannel", "wechat://api", config);
    }
    
    @Override
    public boolean validateConfig() {
        return appId != null && !appId.trim().isEmpty() && 
               openId != null && !openId.trim().isEmpty();
    }
    
    @Override
    public ChannelStatus getStatus() {
        return new ChannelStatus(connected, lastActivityTime, messageCount, lastError);
    }
    
    /**
     * 根据消息内容确定微信消息类型
     */
    private String determineMessageType(Message message) {
        if (message.getContent().contains("图文")) {
            return "news";
        } else if (message.getContent().contains("菜单")) {
            return "click";
        } else if (message.getContent().contains("模板")) {
            return "template";
        } else {
            return "text";
        }
    }
}