package com.ai.infrastructure.agent.interaction.model;

/**
 * 交互渠道类型枚举
 */
public enum ChannelType {
    WEB("web", "Web界面"),
    DINGTALK("dingtalk", "钉钉"),
    WECHAT("wechat", "微信"),
    EMAIL("email", "邮件"),
    API("api", "API接口"),
    SMS("sms", "短信"),
    PUSH("push", "推送通知"),
    VOICE("voice", "语音交互");

    private final String code;
    private final String description;

    ChannelType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ChannelType fromCode(String code) {
        for (ChannelType type : ChannelType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown channel type: " + code);
    }
}