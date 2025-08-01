package com.ai.infrastructure.agent.interaction.model;

public enum InteractionTaskType {
    MULTIPLE_CHANNEL_SEND("多渠道发送"),
    CHANNEL_SPECIFIC_SEND("特定渠道发送"),
    USER_PREFERENCE_MANAGE("用户偏好管理"),
    CHANNEL_STATUS_QUERY("渠道状态查询"),
    REPORT_GENERATE("报告生成"),
    VISUALIZATION_CREATE("可视化创建"),
    GENERIC_INTERACTION("通用交互"),
    USER_INPUT("用户输入"),
    INTERFACE_UPDATE("界面更新"),
    STATUS_FEEDBACK("状态反馈");

    private final String displayName;

    InteractionTaskType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}