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
 * 钉钉渠道适配器
 */
public class DingTalkChannel implements InteractionChannel {
    private static final Logger logger = LoggerFactory.getLogger(DingTalkChannel.class);
    
    private final String accessToken;
    private final String secret;
    private final String robotCode;
    private final Map<String, Object> parameters;
    private final Map<String, Message> pendingMessages = new ConcurrentHashMap<>();
    private volatile boolean connected = false;
    private volatile long lastActivityTime = System.currentTimeMillis();
    private volatile int messageCount = 0;
    private volatile String lastError = null;
    
    public DingTalkChannel(String channelId, Map<String, Object> config) {
        this.accessToken = (String) config.get("accessToken");
        this.secret = (String) config.get("secret");
        this.robotCode = (String) config.get("robotCode");
        this.parameters = new HashMap<>(config);
        
        // 验证必要配置
        if (this.accessToken == null || this.accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("DingTalk access token cannot be null or empty");
        }
        if (this.robotCode == null || this.robotCode.trim().isEmpty()) {
            throw new IllegalArgumentException("DingTalk robot code cannot be null or empty");
        }
    }
    
    @Override
    public SendResult sendMessage(Message message) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Sending message via DingTalk channel: {}", message);
            
            if (!connected) {
                logger.warn("DingTalk channel is not connected");
                return SendResult.failure("Channel not connected", System.currentTimeMillis() - startTime);
            }
            
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return SendResult.failure("Empty message content", System.currentTimeMillis() - startTime);
            }
            
            // 添加钉钉特定的元数据
            message.addMetadata("accessToken", accessToken);
            message.addMetadata("robotCode", robotCode);
            message.addMetadata("msgType", determineMessageType(message));
            
            // 模拟钉钉API调用
            Thread.sleep(150);
            
            message.markAsSent();
            pendingMessages.put(message.getId(), message);
            messageCount++;
            lastActivityTime = System.currentTimeMillis();
            lastError = null;
            
            logger.info("Message sent successfully via DingTalk channel: {}", message.getId());
            return SendResult.success(message.getId(), System.currentTimeMillis() - startTime);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while sending message via DingTalk channel", e);
            return SendResult.failure("Interrupted", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("Failed to send message via DingTalk channel", e);
            lastError = e.getMessage();
            return SendResult.failure(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public Message receiveMessage() {
        try {
            // 模拟钉钉webhook接收消息
            Thread.sleep(100);
            
            if (pendingMessages.isEmpty()) {
                return null;
            }
            
            Message message = pendingMessages.values().iterator().next();
            pendingMessages.remove(message.getId());
            
            message.setStatus(MessageStatus.DELIVERED);
            lastActivityTime = System.currentTimeMillis();
            
            logger.debug("Message received via DingTalk channel: {}", message);
            return message;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while receiving message via DingTalk channel", e);
            return null;
        } catch (Exception e) {
            logger.error("Failed to receive message via DingTalk channel", e);
            return null;
        }
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public ChannelType getChannelType() {
        return ChannelType.DINGTALK;
    }
    
    @Override
    public void connect() {
        if (!connected) {
            connected = true;
            logger.info("DingTalk channel connected - Robot: {}", robotCode);
            
            try {
                Thread.sleep(300);
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
            logger.info("DingTalk channel disconnected - Robot: {}", robotCode);
        }
    }
    
    @Override
    public ChannelConfig getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("accessToken", accessToken);
        config.put("secret", secret);
        config.put("robotCode", robotCode);
        config.put("timeout", parameters.get("timeout"));
        config.put("retryCount", parameters.get("retryCount"));
        
        return new ChannelConfig("DingTalkChannel", "dingtalk://webhook", config);
    }
    
    @Override
    public boolean validateConfig() {
        return accessToken != null && !accessToken.trim().isEmpty() && 
               robotCode != null && !robotCode.trim().isEmpty();
    }
    
    @Override
    public ChannelStatus getStatus() {
        return new ChannelStatus(connected, lastActivityTime, messageCount, lastError);
    }
    
    /**
     * 根据消息内容确定钉钉消息类型
     */
    private String determineMessageType(Message message) {
        if (message.getContent().contains("[") && message.getContent().contains("]")) {
            return "actionCard";
        } else if (message.getContent().contains("标题")) {
            return "feedCard";
        } else {
            return "text";
        }
    }
}