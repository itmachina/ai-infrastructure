package com.ai.infrastructure.agent.interaction.plugin;

/**
 * 插件配置监听器接口
 */
public interface PluginConfigurationListener {
    
    /**
     * 插件注册回调
     */
    void onPluginRegistered(InteractionPlugin plugin);
    
    /**
     * 插件注销回调
     */
    void onPluginUnregistered(InteractionPlugin plugin);
    
    /**
     * 插件配置更新回调
     */
    void onPluginConfigUpdated(InteractionPlugin plugin, java.util.Map<String, Object> oldConfig, java.util.Map<String, Object> newConfig);
}