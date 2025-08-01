package com.ai.infrastructure.agent.unified.state;

import com.ai.infrastructure.agent.AgentStatus;
import com.ai.infrastructure.agent.AgentType;
import com.ai.infrastructure.agent.unified.UnifiedAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统一的Agent状态管理器
 * 提供集中式的Agent状态管理和监控
 */
public class AgentStateManager {
    private static final Logger logger = LoggerFactory.getLogger(AgentStateManager.class);
    
    // 状态数据
    private final UnifiedAgent agent;
    private volatile AgentStatus currentStatus;
    private volatile long lastStatusChangeTime;
    private volatile String currentTask;
    private volatile long currentTaskStartTime;
    
    // 状态历史
    private final Map<Long, AgentStatus> statusHistory;
    private final Map<String, Object> stateAttributes;
    private final AtomicLong statusChangeCount;
    
    public AgentStateManager(UnifiedAgent agent) {
        this.agent = agent;
        this.currentStatus = AgentStatus.IDLE;
        this.lastStatusChangeTime = System.currentTimeMillis();
        this.currentTask = null;
        this.currentTaskStartTime = 0;
        
        this.statusHistory = new ConcurrentHashMap<>();
        this.stateAttributes = new ConcurrentHashMap<>();
        this.statusChangeCount = new AtomicLong(0);
        
        recordStatusChange(currentStatus);
        
        logger.debug("AgentStateManager initialized for agent: {}", agent.getAgentId());
    }
    
    /**
     * 设置Agent状态
     */
    public void setStatus(AgentStatus newStatus) {
        if (newStatus == null) {
            logger.warn("Attempted to set null status for agent: {}", agent.getAgentId());
            return;
        }
        
        if (newStatus != currentStatus) {
            AgentStatus oldStatus = currentStatus;
            currentStatus = newStatus;
            lastStatusChangeTime = System.currentTimeMillis();
            statusChangeCount.incrementAndGet();
            
            recordStatusChange(newStatus);
            
            logger.debug("Agent {} status changed: {} -> {} at {}", 
                       agent.getAgentId(), oldStatus, newStatus, lastStatusChangeTime);
        }
    }
    
    /**
     * 获取当前状态
     */
    public AgentStatus getStatus() {
        return currentStatus;
    }
    
    /**
     * 检查Agent是否空闲
     */
    public boolean isIdle() {
        return currentStatus == AgentStatus.IDLE;
    }
    
    /**
     * 检查Agent是否运行中
     */
    public boolean isRunning() {
        return currentStatus == AgentStatus.RUNNING;
    }
    
    /**
     * 记录状态变更
     */
    private void recordStatusChange(AgentStatus newStatus) {
        long timestamp = System.currentTimeMillis();
        statusHistory.put(timestamp, newStatus);
        
        if (statusHistory.size() > 1000) {
            long oldestTimestamp = statusHistory.keySet().stream()
                .min(Long::compareTo)
                .orElse(0L);
            statusHistory.remove(oldestTimestamp);
        }
    }
    
    /**
     * 重置状态
     */
    public void reset() {
        currentStatus = AgentStatus.IDLE;
        lastStatusChangeTime = System.currentTimeMillis();
        currentTask = null;
        currentTaskStartTime = 0;
        
        statusHistory.clear();
        stateAttributes.clear();
        statusChangeCount.set(0);
        
        logger.debug("AgentStateManager reset for agent: {}", agent.getAgentId());
    }
    
    /**
     * 设置当前任务
     */
    public void setCurrentTask(String task, long startTime) {
        this.currentTask = task;
        this.currentTaskStartTime = startTime;
    }
    
    /**
     * 获取当前任务
     */
    public String getCurrentTask() {
        return currentTask;
    }
    
    /**
     * 获取当前状态持续时间
     */
    public long getCurrentStatusDuration() {
        return System.currentTimeMillis() - lastStatusChangeTime;
    }
    
    /**
     * 获取当前任务持续时间
     */
    public long getCurrentTaskDuration() {
        if (currentTaskStartTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - currentTaskStartTime;
    }
    
    /**
     * 检查是否错误状态
     */
    public boolean isError() {
        return currentStatus == AgentStatus.ERROR;
    }
    
    /**
     * Agent健康状态枚举
     */
    public enum AgentHealthStatus {
        HEALTHY, WARNING, CRITICAL, UNKNOWN
    }
}