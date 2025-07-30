package com.ai.infrastructure.agent.interaction.channel;

import com.ai.infrastructure.agent.interaction.model.Message;
import com.ai.infrastructure.agent.interaction.model.ChannelType;

import java.util.Map;

/**
 * 交互渠道接口
 */
public interface InteractionChannel {
    
    /**
     * 发送消息
     * @param message 消息内容
     * @return 发送结果
     */
    SendResult sendMessage(Message message);
    
    /**
     * 接收消息
     * @return 接收到的消息
     */
    Message receiveMessage();
    
    /**
     * 检查渠道连接状态
     * @return 是否已连接
     */
    boolean isConnected();
    
    /**
     * 获取渠道类型
     * @return 渠道类型
     */
    ChannelType getChannelType();
    
    /**
     * 连接渠道
     */
    void connect();
    
    /**
     * 断开渠道连接
     */
    void disconnect();
    
    /**
     * 获取渠道配置信息
     * @return 配置信息
     */
    ChannelConfig getConfig();
    
    /**
     * 验证渠道配置
     * @return 是否配置有效
     */
    boolean validateConfig();
    
    /**
     * 获取渠道状态信息
     * @return 状态信息
     */
    ChannelStatus getStatus();
    
    /**
     * 发送结果
     */
    class SendResult {
        private final boolean success;
        private final String messageId;
        private final String error;
        private final long responseTime;
        
        public SendResult(boolean success, String messageId, String error, long responseTime) {
            this.success = success;
            this.messageId = messageId;
            this.error = error;
            this.responseTime = responseTime;
        }
        
        public static SendResult success(String messageId, long responseTime) {
            return new SendResult(true, messageId, null, responseTime);
        }
        
        public static SendResult failure(String error, long responseTime) {
            return new SendResult(false, null, error, responseTime);
        }
        
        // getters
        public boolean isSuccess() { return success; }
        public String getMessageId() { return messageId; }
        public String getError() { return error; }
        public long getResponseTime() { return responseTime; }
    }
    
    /**
     * 渠道配置
     */
    class ChannelConfig {
        private final String name;
        private final String endpoint;
        private final Map<String, Object> parameters;
        
        public ChannelConfig(String name, String endpoint, Map<String, Object> parameters) {
            this.name = name;
            this.endpoint = endpoint;
            this.parameters = parameters;
        }
        
        // getters
        public String getName() { return name; }
        public String getEndpoint() { return endpoint; }
        public Map<String, Object> getParameters() { return parameters; }
    }
    
    /**
     * 渠道状态
     */
    class ChannelStatus {
        private final boolean connected;
        private final long lastActivityTime;
        private final int messageCount;
        private final String lastError;
        
        public ChannelStatus(boolean connected, long lastActivityTime, int messageCount, String lastError) {
            this.connected = connected;
            this.lastActivityTime = lastActivityTime;
            this.messageCount = messageCount;
            this.lastError = lastError;
        }
        
        // getters
        public boolean isConnected() { return connected; }
        public long getLastActivityTime() { return lastActivityTime; }
        public int getMessageCount() { return messageCount; }
        public String getLastError() { return lastError; }
    }
}