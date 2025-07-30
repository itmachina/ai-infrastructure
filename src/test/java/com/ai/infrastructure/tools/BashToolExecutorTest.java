package com.ai.infrastructure.tools;

public class BashToolExecutorTest {
    public static void main(String[] args) {
        BashToolExecutor executor = new BashToolExecutor();
        
        // 测试简单的echo命令
        System.out.println("Testing echo command:");
        String result = executor.execute("echo 'Hello, World!'");
        System.out.println(result);
        System.out.println();
        
        // 测试pwd命令
        System.out.println("Testing pwd command:");
        result = executor.execute("pwd");
        System.out.println(result);
        System.out.println();
        
        // 测试ls命令
        System.out.println("Testing ls command:");
        result = executor.execute("ls -la");
        System.out.println(result);
        System.out.println();
        
        // 测试JSON格式命令
        System.out.println("Testing JSON format command:");
        result = executor.execute("{\"command\": \"echo 'JSON test'\", \"timeout\": 10}");
        System.out.println(result);
    }
}