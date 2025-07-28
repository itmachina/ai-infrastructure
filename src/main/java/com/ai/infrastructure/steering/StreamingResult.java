package com.ai.infrastructure.steering;

/**
 * 流式处理结果类
 */
public class StreamingResult {
    private final String type;
    private final String content;
    private final long timestamp;
    
    public StreamingResult(String type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取结果类型
     * @return String
     */
    public String getType() {
        return type;
    }
    
    /**
     * 获取结果内容
     * @return String
     */
    public String getContent() {
        return content;
    }
    
    /**
     * 获取时间戳
     * @return long
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "StreamingResult{type='" + type + "', content='" + content + "', timestamp=" + timestamp + "}";
    }
}