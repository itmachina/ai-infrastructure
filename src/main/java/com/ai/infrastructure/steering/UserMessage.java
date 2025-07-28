package com.ai.infrastructure.steering;

import java.util.Map;
import java.util.HashMap;

/**
 * 用户消息类 - 基于Claude Code的消息格式实现
 * 支持完整的用户消息结构
 */
public class UserMessage {
    private final String type;
    private final Map<String, Object> message;
    private final long timestamp;
    
    public UserMessage(String type, String content) {
        this.type = type;
        this.message = new HashMap<>();
        this.message.put("role", "user");
        this.message.put("content", content);
        this.timestamp = System.currentTimeMillis();
    }
    
    public UserMessage(String type, Map<String, Object> message) {
        this.type = type;
        this.message = new HashMap<>(message);
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取消息类型
     * @return String
     */
    public String getType() {
        return type;
    }
    
    /**
     * 获取消息内容
     * @return String
     */
    public String getContent() {
        Object content = message.get("content");
        return content != null ? content.toString() : "";
    }
    
    /**
     * 获取消息角色
     * @return String
     */
    public String getRole() {
        Object role = message.get("role");
        return role != null ? role.toString() : "user";
    }
    
    /**
     * 获取完整消息对象
     * @return Map<String, Object>
     */
    public Map<String, Object> getMessage() {
        return new HashMap<>(message);
    }
    
    /**
     * 获取时间戳
     * @return long
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 检查消息是否有效
     * @return boolean
     */
    public boolean isValid() {
        return type != null && !type.isEmpty() && 
               message != null && message.containsKey("role") && 
               message.containsKey("content");
    }
    
    @Override
    public String toString() {
        return "UserMessage{type='" + type + "', message=" + message + ", timestamp=" + timestamp + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        UserMessage that = (UserMessage) obj;
        return type.equals(that.type) && message.equals(that.message);
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }
}