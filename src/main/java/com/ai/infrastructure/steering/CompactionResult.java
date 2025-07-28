package com.ai.infrastructure.steering;

import java.util.List;

/**
 * 压缩结果类
 */
public class CompactionResult {
    private final List<Object> compactedMessages;
    private final boolean wasCompacted;
    
    public CompactionResult(List<Object> compactedMessages, boolean wasCompacted) {
        this.compactedMessages = compactedMessages;
        this.wasCompacted = wasCompacted;
    }
    
    /**
     * 获取压缩后的消息
     * @return List<Object>
     */
    public List<Object> getCompactedMessages() {
        return compactedMessages;
    }
    
    /**
     * 是否进行了压缩
     * @return boolean
     */
    public boolean wasCompacted() {
        return wasCompacted;
    }
}