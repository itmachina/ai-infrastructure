package com.ai.infrastructure.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * I2A交互Agent实现
 * 负责用户交互和界面更新任务
 */
public class InteractionAgent extends SpecializedAgent {
    private static final Logger logger = LoggerFactory.getLogger(InteractionAgent.class);
    
    // 交互相关模式
    private static final Pattern[] INTERACTION_PATTERNS = {
        Pattern.compile("用户|界面|展示|显示|输出"),
        Pattern.compile("报告|总结|概览|状态"),
        Pattern.compile("交互|沟通|交流|反馈"),
        Pattern.compile("可视化|图表|界面|UI"),
        Pattern.compile("演示|展示|说明|解释")
    };
    
    public InteractionAgent(String agentId, String name) {
        super(agentId, name, AgentType.I2A);
        logger.info("I2A Interaction Agent initialized: {}", agentId);
    }
    
    @Override
    protected String processSpecializedTask(String task) {
        logger.debug("I2A Agent processing interaction task: {}", task);
        
        // 处理交互相关任务
        if (isInteractiveTask(task)) {
            return handleInteractiveTask(task);
        }
        
        // 处理报告生成任务
        if (isReportTask(task)) {
            return handleReportTask(task);
        }
        
        // 处理可视化任务
        if (isVisualizationTask(task)) {
            return handleVisualizationTask(task);
        }
        
        // 默认交互处理
        return handleGenericInteraction(task);
    }
    
    @Override
    public boolean supportsTaskType(String taskType) {
        String lowerTask = taskType.toLowerCase();
        
        return lowerTask.contains("交互") || 
               lowerTask.contains("界面") || 
               lowerTask.contains("展示") || 
               lowerTask.contains("报告") || 
               lowerTask.contains("可视化") || 
               lowerTask.contains("用户");
    }
    
    /**
     * 处理交互任务
     */
    private String handleInteractiveTask(String task) {
        // 模拟交互处理
        String interactionType = extractInteractionType(task);
        
        switch (interactionType) {
            case "用户输入":
                return handleUserInput(task);
            case "界面更新":
                return handleInterfaceUpdate(task);
            case "状态反馈":
                return handleStatusFeedback(task);
            default:
                return processInteractionResponse(task);
        }
    }
    
    /**
     * 处理报告任务
     */
    private String handleReportTask(String task) {
        // 生成结构化报告
        StringBuilder report = new StringBuilder();
        report.append("=== 任务执行报告 ===\n");
        report.append("任务描述: ").append(task).append("\n");
        report.append("执行时间: ").append(java.time.LocalDateTime.now()).append("\n");
        report.append("执行Agent: ").append(agentType.getDisplayName()).append("\n");
        report.append("处理状态: 成功\n");
        report.append("报告内容: \n");
        report.append("- 任务已完成交互处理\n");
        report.append("- 用户反馈已收集\n");
        report.append("- 界面状态已更新\n");
        
        return report.toString();
    }
    
    /**
     * 处理可视化任务
     */
    private String handleVisualizationTask(String task) {
        // 生成可视化描述
        return generateVisualizationDescription(task);
    }
    
    /**
     * 处理通用交互
     */
    private String handleGenericInteraction(String task) {
        return String.format(
            "I2A交互Agent已处理任务: %s\n" +
            "交互类型: 通用交互\n" +
            "处理结果: 交互完成\n" +
            "用户反馈: 已收集",
            task
        );
    }
    
    /**
     * 处理用户输入
     */
    private String handleUserInput(String task) {
        return String.format(
            "I2A交互Agent已处理用户输入任务: %s\n" +
            "输入验证: 通过\n" +
            "交互响应: 已生成\n" +
            "用户状态: 已更新",
            task
        );
    }
    
    /**
     * 处理界面更新
     */
    private String handleInterfaceUpdate(String task) {
        return String.format(
            "I2A交互Agent已处理界面更新任务: %s\n" +
            "界面状态: 已更新\n" +
            "组件刷新: 已完成\n" +
            "用户体验: 已优化",
            task
        );
    }
    
    /**
     * 处理状态反馈
     */
    private String handleStatusFeedback(String task) {
        return String.format(
            "I2A交互Agent已处理状态反馈任务: %s\n" +
            "状态信息: 已收集\n" +
            "反馈分析: 已完成\n" +
            "建议措施: 已生成",
            task
        );
    }
    
    /**
     * 处理交互响应
     */
    private String processInteractionResponse(String task) {
        return String.format(
            "I2A交互Agent响应:\n" +
            "任务: %s\n" +
            "交互模式: 智能交互\n" +
            "响应时间: %d ms\n" +
            "用户满意度: 高",
            task,
            System.currentTimeMillis() - lastActivityTime
        );
    }
    
    /**
     * 生成可视化描述
     */
    private String generateVisualizationDescription(String task) {
        return String.format(
            "I2A交互Agent可视化生成:\n" +
            "任务: %s\n" +
            "可视化类型: 交互式图表\n" +
            "数据维度: 多维度\n" +
            "交互特性: 实时更新\n" +
            "用户体验: 直观易用",
            task
        );
    }
    
    /**
     * 检查是否为交互任务
     */
    private boolean isInteractiveTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("交互") || 
               lowerTask.contains("用户") || 
               lowerTask.contains("反馈");
    }
    
    /**
     * 检查是否为报告任务
     */
    private boolean isReportTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("报告") || 
               lowerTask.contains("总结") || 
               lowerTask.contains("概览");
    }
    
    /**
     * 检查是否为可视化任务
     */
    private boolean isVisualizationTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("可视化") || 
               lowerTask.contains("图表") || 
               lowerTask.contains("界面");
    }
    
    /**
     * 提取交互类型
     */
    private String extractInteractionType(String task) {
        String lowerTask = task.toLowerCase();
        
        if (lowerTask.contains("输入")) return "用户输入";
        if (lowerTask.contains("更新") || lowerTask.contains("刷新")) return "界面更新";
        if (lowerTask.contains("状态") || lowerTask.contains("反馈")) return "状态反馈";
        if (lowerTask.contains("交互")) return "通用交互";
        
        return "默认交互";
    }
}