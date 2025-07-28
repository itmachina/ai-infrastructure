package com.ai.infrastructure.tools;

/**
 * 读取工具执行器
 */
public class ReadToolExecutor implements ToolExecutor {
    @Override
    public String execute(String task) {
        // 简化的文件读取实现
        return "Reading content from file specified in: " + task;
    }
}