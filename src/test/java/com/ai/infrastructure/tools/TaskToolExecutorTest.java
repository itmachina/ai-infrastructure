package com.ai.infrastructure.tools;

public class TaskToolExecutorTest {
    public static void main(String[] args) {
        TaskToolExecutor executor = new TaskToolExecutor();
        
        // 测试简单的任务执行
        System.out.println("Testing simple task execution:");
        String result = executor.execute("task \"Simple Test\" \"Calculate 2 + 3\"");
        System.out.println(result);
        System.out.println();
        
        // 测试JSON格式的任务执行
        System.out.println("Testing JSON format task execution:");
        String jsonTask = "{\"description\": \"JSON Test\", \"prompt\": \"Calculate 5 * 6\"}";
        result = executor.execute(jsonTask);
        System.out.println(result);
    }
}