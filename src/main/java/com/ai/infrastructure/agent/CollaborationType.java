package com.ai.infrastructure.agent;

/**
 * 子Agent协作类型枚举
 */
public enum CollaborationType {
    NONE("无协作"),
    PARALLEL("并行执行"),
    SEQUENTIAL("顺序执行"),
    PIPELINE("流水线执行"),
    REDUNDANT("冗余执行"),
    ADAPTIVE("自适应执行");
    
    private final String description;
    
    CollaborationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}