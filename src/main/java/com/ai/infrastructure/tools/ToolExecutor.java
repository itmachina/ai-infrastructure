package com.ai.infrastructure.tools;

/**
 * 工具执行器接口
 */
public interface ToolExecutor {
    /**
     * 执行工具
     * @param task 任务描述
     * @return 执行结果
     */
    String execute(String task);
}