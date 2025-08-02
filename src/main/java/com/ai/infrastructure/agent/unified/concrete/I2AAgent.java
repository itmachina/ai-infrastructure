package com.ai.infrastructure.agent.unified.concrete;

import com.ai.infrastructure.agent.AgentType;
import com.ai.infrastructure.agent.unified.BaseUnifiedAgent;
import com.ai.infrastructure.agent.unified.UnifiedAgentContext;

public class I2AAgent extends BaseUnifiedAgent {

    public I2AAgent(String agentId, String name, UnifiedAgentContext context) {
        super(agentId, name, AgentType.I2A, context);
    }

    @Override
    public String processTask(String task) {
        logger.debug("I2AAgent processing task: {}", task);
        // 模拟I2A Agent的交互界面处理
        if (task.contains("交互") || task.contains("界面") || task.contains("展示")) {
            return String.format("[I2A-交互Agent] 生成交互界面原型: %s\n" +
                    "  - 设计响应式布局\n" +
                    "  - 创建用户交互组件\n" +
                    "  - 实现数据可视化\n" +
                    "  - 生成用户测试报告",
                    task);
        } else if (task.contains("沟通") || task.contains("汇报") || task.contains("演示")) {
            return String.format("[I2A-交互Agent] 创建沟通演示: %s\n" +
                    "  - 准备演示材料\n" +
                    "  - 设计沟通流程\n" +
                    "  - 制作汇报PPT\n" +
                    "  - 安排演示时间",
                    task);
        } else {
            return String.format("[I2A-交互Agent] 处理交互任务: %s\n" +
                    "  - 分析交互需求\n" +
                    "  - 设计交互方案\n" +
                    "  - 实现交互功能\n" +
                    "  - 测试用户体验",
                    task);
        }
    }
}
