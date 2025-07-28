package com.ai.infrastructure.agent;

/**
 * Agent状态枚举
 */
public enum AgentStatus {
    IDLE,        // 空闲
    RUNNING,     // 运行中
    BUSY,        // 忙碌
    ERROR,       // 错误
    TERMINATED,  // 终止
    ABORTED      // 中断
}