package com.ai.infrastructure.tools;

/**
 * 写入工具执行器
 */
public class WriteToolExecutor implements ToolExecutor {
    @Override
    public String execute(String task) {
        // 简化的文件写入实现
        return "Writing content to file as specified in: " + task;
    }
}