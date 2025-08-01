package com.ai.infrastructure.agent;

/**
 * 任务协调策略枚举
 */
public enum CoordinationStrategy {
    PARALLEL("并行策略"),
    SEQUENTIAL("顺序策略"),
    PIPELINE("流水线策略"),
    REDUNDANT("冗余策略"),
    ADAPTIVE("自适应策略");
    
    private final String description;
    
    CoordinationStrategy(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}