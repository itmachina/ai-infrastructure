package com.ai.infrastructure.agent.interaction.model;

/**
 * 消息类型枚举
 */
public enum MessageType {
    TEXT("text", "文本消息"),
    IMAGE("image", "图片消息"),
    FILE("file", "文件消息"),
    VOICE("voice", "语音消息"),
    VIDEO("video", "视频消息"),
    CARD("card", "卡片消息"),
    BUTTON("button", "按钮消息"),
    MENU("menu", "菜单消息"),
    TEMPLATE("template", "模板消息"),
    RICH_TEXT("rich_text", "富文本消息");

    private final String code;
    private final String description;

    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MessageType fromCode(String code) {
        for (MessageType type : MessageType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + code);
    }
}