package com.ai.infrastructure.tools;

public class CalculateToolExecutorTest {
    public static void main(String[] args) {
        CalculateToolExecutor executor = new CalculateToolExecutor();
        
        // 测试基本运算
        System.out.println("Basic operations:");
        System.out.println(executor.execute("Calculate 2 + 3"));
        System.out.println(executor.execute("Calculate 5 - 2"));
        System.out.println(executor.execute("Calculate 4 * 3"));
        System.out.println(executor.execute("Calculate 8 / 2"));
        
        // 测试指数运算
        System.out.println("\nExponent operations:");
        System.out.println(executor.execute("Calculate 2 ^ 3"));
        System.out.println(executor.execute("Calculate 5 ^ 2"));
        
        // 测试括号
        System.out.println("\nParentheses operations:");
        System.out.println(executor.execute("Calculate (2 + 3) * 4"));
        System.out.println(executor.execute("Calculate 2 * (3 + 4)"));
        
        // 测试复杂表达式
        System.out.println("\nComplex expressions:");
        System.out.println(executor.execute("Calculate 2 + 3 * 4"));
        System.out.println(executor.execute("Calculate (2 + 3) * (4 - 1)"));
        System.out.println(executor.execute("Calculate 2 ^ 3 + 1"));
        System.out.println(executor.execute("Calculate (2 + 3) ^ 2"));
        System.out.println(executor.execute("Calculate 2 ^ (3 + 1)"));
    }
}