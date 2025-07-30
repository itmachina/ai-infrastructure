package com.ai.infrastructure.agent.interaction.model;

/**
 * 消息状态枚举
 */
public enum MessageStatus {
    PENDING("pending", "待发送"),
    SENT("sent", "已发送"),
    DELIVERED("delivered", "已送达"),
    READ("read", "已读"),
    FAILED("failed", "发送失败"),
    RETRY("retry", "重试中");

    private final String code;
    private final String description;

    MessageStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MessageStatus fromCode(String code) {
        for (MessageStatus status : MessageStatus.values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown message status: " + code);
    }
}