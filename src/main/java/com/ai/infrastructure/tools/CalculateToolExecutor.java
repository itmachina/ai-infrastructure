package com.ai.infrastructure.tools;

/**
 * 计算工具执行器
 */
public class CalculateToolExecutor implements ToolExecutor {
    @Override
    public String execute(String task) {
        // 简化的计算实现
        return "Performing calculation based on: " + task;
    }
}