package com.ai.infrastructure.agent.interaction.channel;

import com.ai.infrastructure.agent.interaction.model.ChannelType;

import java.util.HashMap;
import java.util.Map;

/**
 * 渠道配置助手类
 */
public class ChannelConfigHelper {
    
    /**
     * 创建Web渠道配置
     */
    public static Map<String, Object> createWebConfig(String endpoint) {
        Map<String, Object> config = new HashMap<>();
        config.put("endpoint", endpoint);
        config.put("timeout", 5000);
        config.put("retryCount", 3);
        config.put("authToken", null);
        config.put("connectionPoolSize", 10);
        config.put("sslEnabled", true);
        return config;
    }
    
    /**
     * 创建钉钉渠道配置
     */
    public static Map<String, Object> createDingTalkConfig(String accessToken, String secret, String robotCode) {
        Map<String, Object> config = new HashMap<>();
        config.put("accessToken", accessToken);
        config.put("secret", secret);
        config.put("robotCode", robotCode);
        config.put("timeout", 10000);
        config.put("retryCount", 3);
        config.put("syncMode", false);
        config.put("webhookUrl", "https://oapi.dingtalk.com/robot/send?access_token=" + accessToken);
        return config;
    }
    
    /**
     * 创建微信渠道配置
     */
    public static Map<String, Object> createWeChatConfig(String appid, String secret, String templateId) {
        Map<String, Object> config = new HashMap<>();
        config.put("appid", appid);
        config.put("secret", secret);
        config.put("templateId", templateId);
        config.put("timeout", 8000);
        config.put("retryCount", 3);
        config.put("accessTokenUrl", "https://api.weixin.qq.com/cgi-bin/token");
        config.put("sendMessageUrl", "https://api.weixin.qq.com/cgi-bin/message/template/send");
        return config;
    }
    
    /**
     * 创建邮箱渠道配置
     */
    public static Map<String, Object> createEmailConfig(String smtpHost, int smtpPort, String username, String password) {
        Map<String, Object> config = new HashMap<>();
        config.put("smtpHost", smtpHost);
        config.put("smtpPort", smtpPort);
        config.put("username", username);
        config.put("password", password);
        config.put("timeout", 30000);
        config.put("retryCount", 3);
        config.put("sslEnabled", true);
        config.put("fromEmail", username);
        config.put("charset", "UTF-8");
        return config;
    }
    
    /**
     * 从配置创建渠道参数
     */
    public static Map<String, Object> createChannelParameters(ChannelType channelType, 
                                                              String endpoint, 
                                                              Map<String, Object> additionalConfig) {
        
        Map<String, Object> parameters = new HashMap<>();
        
        switch (channelType) {
            case WEB:
                parameters.putAll(createWebConfig(endpoint));
                break;
            case DINGTALK:
                // 对于钉钉，endpoint通常包含配置信息
                if (endpoint.contains("access_token")) {
                    String[] parts = endpoint.split("\\?");
                    String baseUrl = parts[0];
                    String queryParams = parts.length > 1 ? parts[1] : "";
                    
                    String accessToken = extractQueryParam(queryParams, "access_token");
                    String secret = extractQueryParam(queryParams, "secret");
                    String robotCode = extractQueryParam(queryParams, "robot_code");
                    
                    parameters.putAll(createDingTalkConfig(accessToken, secret, robotCode));
                } else {
                    parameters.putAll(createDingTalkConfig(endpoint, "", ""));
                }
                break;
            case WECHAT:
                parameters.putAll(createWeChatConfig(endpoint, "", ""));
                break;
            case EMAIL:
                // 对于邮箱，endpoint通常是SMTP服务器地址
                String[] emailParts = endpoint.split(":");
                String host = emailParts[0];
                int port = emailParts.length > 1 ? Integer.parseInt(emailParts[1]) : 587;
                
                parameters.putAll(createEmailConfig(host, port, "", ""));
                break;
            default:
                parameters.putAll(createDefaultConfig(endpoint));
        }
        
        // 合并额外配置
        if (additionalConfig != null) {
            parameters.putAll(additionalConfig);
        }
        
        return parameters;
    }
    
    /**
     * 提取查询参数
     */
    private static String extractQueryParam(String query, String paramName) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        
        return "";
    }
    
    /**
     * 创建默认配置
     */
    private static Map<String, Object> createDefaultConfig(String endpoint) {
        Map<String, Object> config = new HashMap<>();
        config.put("endpoint", endpoint);
        config.put("timeout", 5000);
        config.put("retryCount", 3);
        config.put("sslEnabled", true);
        return config;
    }
}