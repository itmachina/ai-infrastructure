package com.ai.infrastructure.tools;

/**
 * 搜索工具执行器
 */
public class SearchToolExecutor implements ToolExecutor {
    @Override
    public String execute(String task) {
        // 简化的搜索实现
        return "Searching for content related to: " + task;
    }
}