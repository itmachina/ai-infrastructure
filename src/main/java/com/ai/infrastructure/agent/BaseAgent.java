package com.ai.infrastructure.agent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent基类，定义Agent的基本行为
 */
public abstract class BaseAgent {
    protected String agentId;
    protected String name;
    protected AgentStatus status;
    protected final AtomicBoolean isAborted = new AtomicBoolean(false);
    
    public BaseAgent(String agentId, String name) {
        this.agentId = agentId;
        this.name = name;
        this.status = AgentStatus.IDLE;
    }
    
    /**
     * Agent执行任务的抽象方法
     * @param task 任务描述
     * @return 执行结果
     */
    public abstract CompletableFuture<String> executeTask(String task);
    
    /**
     * 获取Agent状态
     * @return 当前状态
     */
    public AgentStatus getStatus() {
        return status;
    }
    
    /**
     * 设置Agent状态
     * @param status 新状态
     */
    public void setStatus(AgentStatus status) {
        this.status = status;
    }
    
    /**
     * 获取Agent ID
     * @return Agent ID
     */
    public String getAgentId() {
        return agentId;
    }
    
    /**
     * 获取Agent名称
     * @return Agent名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 检查是否被中断
     * @return boolean
     */
    public boolean isAborted() {
        return isAborted.get() || status == AgentStatus.ABORTED;
    }
    
    /**
     * 中断Agent执行
     */
    public void abort() {
        if (isAborted.compareAndSet(false, true)) {
            setStatus(AgentStatus.ABORTED);
        }
    }
    
    /**
     * 重置Agent状态
     */
    public void reset() {
        isAborted.set(false);
        setStatus(AgentStatus.IDLE);
    }
}