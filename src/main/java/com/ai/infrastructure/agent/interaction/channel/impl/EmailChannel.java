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
 * 邮件渠道适配器
 */
public class EmailChannel implements InteractionChannel {
    private static final Logger logger = LoggerFactory.getLogger(EmailChannel.class);
    
    private final String smtpHost;
    private final int smtpPort;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final Map<String, Object> parameters;
    private final Map<String, Message> pendingMessages = new ConcurrentHashMap<>();
    private volatile boolean connected = false;
    private volatile long lastActivityTime = System.currentTimeMillis();
    private volatile int messageCount = 0;
    private volatile String lastError = null;
    
    public EmailChannel(String channelId, Map<String, Object> config) {
        this.smtpHost = (String) config.get("smtpHost");
        this.smtpPort = (Integer) config.getOrDefault("smtpPort", 587);
        this.username = (String) config.get("username");
        this.password = (String) config.get("password");
        this.fromAddress = (String) config.get("fromEmail");
        this.parameters = new HashMap<>(config);
        
        // 验证必要配置
        if (this.smtpHost == null || this.smtpHost.trim().isEmpty()) {
            throw new IllegalArgumentException("SMTP host cannot be null or empty");
        }
        if (this.username == null || this.username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (this.fromAddress == null || this.fromAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("From address cannot be null or empty");
        }
        
        // 如果没有设置默认发件人，使用用户名
        // (this.fromAddress已经在第37行设置过了)
    }
    
    @Override
    public SendResult sendMessage(Message message) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Sending message via Email channel: {}", message);
            
            if (!connected) {
                logger.warn("Email channel is not connected");
                return SendResult.failure("Channel not connected", System.currentTimeMillis() - startTime);
            }
            
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return SendResult.failure("Empty message content", System.currentTimeMillis() - startTime);
            }
            
            // 添加邮件特定的元数据
            message.addMetadata("smtpHost", smtpHost);
            message.addMetadata("smtpPort", smtpPort);
            message.addMetadata("fromAddress", fromAddress);
            
            // 解析收件人地址
            String toAddress = parseToAddress(message);
            if (toAddress == null) {
                return SendResult.failure("Invalid to address", System.currentTimeMillis() - startTime);
            }
            
            message.addMetadata("toAddress", toAddress);
            message.addMetadata("subject", generateSubject(message));
            
            // 模拟邮件发送
            Thread.sleep(500);
            
            message.markAsSent();
            pendingMessages.put(message.getId(), message);
            messageCount++;
            lastActivityTime = System.currentTimeMillis();
            lastError = null;
            
            logger.info("Email sent successfully via Email channel: {} -> {}", 
                       message.getId(), toAddress);
            return SendResult.success(message.getId(), System.currentTimeMillis() - startTime);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while sending message via Email channel", e);
            return SendResult.failure("Interrupted", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("Failed to send message via Email channel", e);
            lastError = e.getMessage();
            return SendResult.failure(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public Message receiveMessage() {
        try {
            // 模拟邮件接收
            Thread.sleep(300);
            
            if (pendingMessages.isEmpty()) {
                return null;
            }
            
            Message message = pendingMessages.values().iterator().next();
            pendingMessages.remove(message.getId());
            
            message.setStatus(MessageStatus.DELIVERED);
            lastActivityTime = System.currentTimeMillis();
            
            logger.debug("Message received via Email channel: {}", message);
            return message;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while receiving message via Email channel", e);
            return null;
        } catch (Exception e) {
            logger.error("Failed to receive message via Email channel", e);
            return null;
        }
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }
    
    @Override
    public void connect() {
        if (!connected) {
            connected = true;
            logger.info("Email channel connected - SMTP: {}:{}", smtpHost, smtpPort);
            
            try {
                Thread.sleep(600);
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
            logger.info("Email channel disconnected - SMTP: {}:{}", smtpHost, smtpPort);
        }
    }
    
    @Override
    public ChannelConfig getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("smtpHost", smtpHost);
        config.put("smtpPort", smtpPort);
        config.put("username", username);
        config.put("fromAddress", fromAddress);
        config.put("timeout", parameters.get("timeout"));
        config.put("retryCount", parameters.get("retryCount"));
        config.put("sslEnabled", parameters.get("sslEnabled"));
        config.put("starttlsEnabled", parameters.get("starttlsEnabled"));
        
        return new ChannelConfig("EmailChannel", "smtp://" + smtpHost + ":" + smtpPort, config);
    }
    
    @Override
    public boolean validateConfig() {
        return smtpHost != null && !smtpHost.trim().isEmpty() && 
               fromAddress != null && !fromAddress.trim().isEmpty();
    }
    
    @Override
    public ChannelStatus getStatus() {
        return new ChannelStatus(connected, lastActivityTime, messageCount, lastError);
    }
    
    /**
     * 解析收件人地址
     */
    private String parseToAddress(Message message) {
        String content = message.getContent();
        
        // 尝试从内容中提取邮箱地址
        if (content.contains("@")) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.contains("@") && line.contains(".")) {
                    return line.trim();
                }
            }
        }
        
        // 从元数据中获取
        if (message.hasMetadata("to")) {
            return message.getMetadata("to").toString();
        }
        
        return null;
    }
    
    /**
     * 生成邮件主题
     */
    private String generateSubject(Message message) {
        String content = message.getContent();
        
        if (content.length() > 50) {
            return content.substring(0, 47) + "...";
        } else if (content.contains("\n")) {
            return content.split("\n")[0];
        } else {
            return "系统通知";
        }
    }
}