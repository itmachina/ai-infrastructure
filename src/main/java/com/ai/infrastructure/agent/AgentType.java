package com.ai.infrastructure.agent;

/**
 * Agent类型枚举
 * 基于Claude Code分层多Agent架构定义
 */
public enum AgentType {
    /**
     * I2A交互Agent - 负责用户交互和界面更新任务
     * 处理用户输入、界面更新、结果展示等交互性任务
     */
    I2A("交互Agent", "处理用户交互和界面更新任务"),
    
    /**
     * UH1用户处理Agent - 负责用户请求解析和响应生成
     * 处理用户请求解析、数据转换、格式化、验证等任务
     */
    UH1("用户处理Agent", "处理用户请求解析和响应生成"),
    
    /**
     * KN5知识处理Agent - 负责知识查询、推理和学习任务
     * 处理知识查询、推理分析、学习优化、决策制定等任务
     */
    KN5("知识处理Agent", "处理知识查询、推理和学习任务");
    
    private final String displayName;
    private final String description;
    
    AgentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取Agent类型的能力集合
     */
    public String[] getCapabilities() {
        switch (this) {
            case I2A:
                return new String[]{"交互", "界面", "用户", "展示", "沟通", "汇报", "演示", "可视化"};
            case UH1:
                return new String[]{"处理", "解析", "响应", "转换", "格式化", "验证", "计算", "执行"};
            case KN5:
                return new String[]{"知识", "推理", "学习", "分析", "决策", "评估", "优化", "规划"};
            default:
                return new String[0];
        }
    }
    
    /**
     * 获取Agent类型的优先级分数
     */
    public int getPriorityScore() {
        switch (this) {
            case I2A: return 1;
            case UH1: return 2;
            case KN5: return 3;
            default: return 0;
        }
    }
}