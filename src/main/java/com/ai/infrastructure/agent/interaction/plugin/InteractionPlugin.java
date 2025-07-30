package com.ai.infrastructure.agent.interaction.plugin;

import com.ai.infrastructure.agent.interaction.channel.InteractionChannel;
import com.ai.infrastructure.agent.interaction.model.ChannelType;

import java.util.Map;

/**
 * 交互渠道插件接口
 */
public interface InteractionPlugin {
    
    /**
     * 插件ID
     */
    String getPluginId();
    
    /**
     * 插件名称
     */
    String getName();
    
    /**
     * 插件版本
     */
    String getVersion();
    
    /**
     * 支持的渠道类型
     */
    ChannelType[] getSupportedChannelTypes();
    
    /**
     * 创建渠道实例
     * @param channelId 渠道ID
     * @param config 配置参数
     * @return 渠道实例
     */
    InteractionChannel createChannel(String channelId, Map<String, Object> config);
    
    /**
     * 插件初始化
     */
    void initialize();
    
    /**
     * 插件销毁
     */
    void destroy();
    
    /**
     * 检查插件是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取插件配置
     */
    Map<String, Object> getPluginConfig();
    
    /**
     * 更新插件配置
     */
    void updateConfig(Map<String, Object> config);
}