package com.ai.infrastructure.agent.interaction.plugin;

/**
 * 插件感知接口
 */
public interface PluginAware {
    
    /**
     * 注册插件
     * @param plugin 插件实例
     */
    void registerPlugin(InteractionPlugin plugin);
    
    /**
     * 注销插件
     * @param pluginId 插件ID
     */
    void unregisterPlugin(String pluginId);
    
    /**
     * 获取插件
     * @param pluginId 插件ID
     * @return 插件实例
     */
    InteractionPlugin getPlugin(String pluginId);
    
    /**
     * 获取所有已注册的插件
     * @return 插件列表
     */
    java.util.List<InteractionPlugin> getAllPlugins();
}