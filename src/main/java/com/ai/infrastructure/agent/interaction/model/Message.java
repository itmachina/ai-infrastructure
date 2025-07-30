package com.ai.infrastructure.agent.interaction.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 统一消息模型
 */
@Slf4j
@Data
public class Message {
    private String id;
    private String content;
    private ChannelType channelType;
    private MessageType messageType;
    private Map<String, Object> metadata;
    private String userId;
    private String sessionId;
    private LocalDateTime timestamp;
    private MessageStatus status;
    private String replyToMessageId;
    
    public Message() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.PENDING;
        this.metadata = new HashMap<>();
    }
    
    public Message(String content, ChannelType channelType, MessageType messageType) {
        this();
        this.content = content;
        this.channelType = channelType;
        this.messageType = messageType;
    }
    
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }
    
    public boolean hasMetadata(String key) {
        return metadata != null && metadata.containsKey(key);
    }
    
    public void markAsSent() {
        this.status = MessageStatus.SENT;
    }
    
    public void markAsFailed(String error) {
        this.status = MessageStatus.FAILED;
        addMetadata("error", error);
    }
    
    public void markAsDelivered() {
        this.status = MessageStatus.DELIVERED;
    }
    
    public boolean isTextMessage() {
        return messageType == MessageType.TEXT;
    }
    
    public boolean isInteractive() {
        return messageType == MessageType.BUTTON || 
               messageType == MessageType.MENU || 
               messageType == MessageType.CARD;
    }
    
    public String toString() {
        return String.format("Message{id='%s', channel='%s', type='%s', status='%s'}", 
                           id, channelType, messageType, status);
    }
}