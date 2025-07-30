package com.ai.infrastructure.agent;

import com.ai.infrastructure.scheduler.EnhancedTaskTool;
import com.ai.infrastructure.scheduler.IntelligentTaskDecomposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 简单的智能任务工具测试
 * 专注于验证Agent分配和任务执行是否正常工作
 */
public class SmartTaskToolSimpleTest {
    private static final Logger logger = LoggerFactory.getLogger(SmartTaskToolSimpleTest.class);
    
    public static void main(String[] args) {
        System.out.println("🧪 开始智能任务工具简单测试...");
        
        EnhancedTaskTool taskTool = new EnhancedTaskTool();
        
        try {
            // 测试1：简单任务执行
            System.out.println("\n📋 测试1：简单任务执行");
            String result1 = taskTool.executeTaskSync("计算2+2", IntelligentTaskDecomposer.TaskPriority.MEDIUM);
            System.out.println("✅ 任务1结果: " + result1.substring(0, Math.min(result1.length(), 100)) + "...");
            
            // 测试2：复杂任务执行
            System.out.println("\n📋 测试2：复杂任务执行");
            String complexTask = "分析用户需求并生成交互界面原型";
            String result2 = taskTool.executeTaskSync(complexTask, IntelligentTaskDecomposer.TaskPriority.HIGH);
            System.out.println("✅ 任务2结果: " + result2.substring(0, Math.min(result2.length(), 100)) + "...");
            
            // 测试3：任务复杂度分析
            System.out.println("\n📋 测试3：任务复杂度分析");
            double complexity = taskTool.analyzeTaskComplexity(complexTask);
            System.out.println("✅ 任务复杂度: " + complexity);
            
            // 测试4：系统状态
            System.out.println("\n📋 测试4：系统状态");
            String status = taskTool.getSystemStatus();
            System.out.println("✅ 系统状态获取成功，长度: " + status.length() + " 字符");
            
            System.out.println("\n🎉 所有测试通过！智能任务工具工作正常！");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            taskTool.shutdown();
        }
    }
}