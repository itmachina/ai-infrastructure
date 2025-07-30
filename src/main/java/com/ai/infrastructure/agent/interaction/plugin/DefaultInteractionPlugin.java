package com.ai.infrastructure.agent.interaction.plugin;

import com.ai.infrastructure.agent.interaction.channel.InteractionChannel;
import com.ai.infrastructure.agent.interaction.channel.impl.WebChannel;
import com.ai.infrastructure.agent.interaction.channel.impl.DingTalkChannel;
import com.ai.infrastructure.agent.interaction.channel.impl.WeChatChannel;
import com.ai.infrastructure.agent.interaction.channel.impl.EmailChannel;
import com.ai.infrastructure.agent.interaction.model.ChannelType;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 默认交互插件实现
 */
public class DefaultInteractionPlugin implements InteractionPlugin {
    
    private final String pluginId;
    private final String name;
    private final String version;
    private final Map<String, Object> config;
    private boolean initialized = false;
    
    // 内置渠道类映射
    private static final Map<ChannelType, Class<? extends InteractionChannel>> CHANNEL_CLASSES = new HashMap<>();
    
    static {
        CHANNEL_CLASSES.put(ChannelType.WEB, WebChannel.class);
        CHANNEL_CLASSES.put(ChannelType.DINGTALK, DingTalkChannel.class);
        CHANNEL_CLASSES.put(ChannelType.WECHAT, WeChatChannel.class);
        CHANNEL_CLASSES.put(ChannelType.EMAIL, EmailChannel.class);
    }
    
    public DefaultInteractionPlugin() {
        this("default", "Default Interaction Plugin", "1.0.0", new HashMap<>());
    }
    
    public DefaultInteractionPlugin(String pluginId, String name, String version, Map<String, Object> config) {
        this.pluginId = pluginId;
        this.name = name;
        this.version = version;
        this.config = new HashMap<>(config);
    }
    
    @Override
    public String getPluginId() {
        return pluginId;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public ChannelType[] getSupportedChannelTypes() {
        return CHANNEL_CLASSES.keySet().toArray(new ChannelType[0]);
    }
    
    @Override
    public InteractionChannel createChannel(String channelId, Map<String, Object> config) {
        if (!initialized) {
            throw new IllegalStateException("Plugin not initialized");
        }
        
        if (channelId == null || channelId.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel ID cannot be null or empty");
        }
        
        // 这里需要根据配置来确定创建哪种类型的渠道
        // 暂时返回一个通用的渠道实现
        try {
            Class<? extends InteractionChannel> channelClass = determineChannelClass(config);
            return channelClass.getConstructor(String.class, Map.class).newInstance(channelId, config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create channel: " + channelId, e);
        }
    }
    
    @Override
    public void initialize() {
        if (initialized) {
            return;
        }
        
        logger().info("Initializing plugin: {} v{}", name, version);
        
        // 验证配置
        if (config != null) {
            validatePluginConfig();
        }
        
        initialized = true;
        logger().info("Plugin initialized successfully: {}", pluginId);
    }
    
    @Override
    public void destroy() {
        if (!initialized) {
            return;
        }
        
        logger().info("Destroying plugin: {}", pluginId);
        
        // 清理资源
        config.clear();
        
        initialized = false;
        logger().info("Plugin destroyed successfully: {}", pluginId);
    }
    
    @Override
    public boolean isAvailable() {
        return initialized;
    }
    
    @Override
    public Map<String, Object> getPluginConfig() {
        return new HashMap<>(config);
    }
    
    @Override
    public void updateConfig(Map<String, Object> newConfig) {
        if (newConfig == null) {
            newConfig = new HashMap<>();
        }
        
        logger().info("Updating plugin config for: {}", pluginId);
        
        // 备份旧配置
        Map<String, Object> oldConfig = new HashMap<>(config);
        
        // 更新配置
        this.config.clear();
        this.config.putAll(newConfig);
        
        // 验证新配置
        validatePluginConfig();
        
        logger().info("Plugin config updated successfully: {}", pluginId);
    }
    
    /**
     * 确定渠道类
     */
    private Class<? extends InteractionChannel> determineChannelClass(Map<String, Object> config) {
        String type = (String) config.getOrDefault("type", "WEB");
        ChannelType channelType = ChannelType.valueOf(type.toUpperCase());
        
        Class<? extends InteractionChannel> channelClass = CHANNEL_CLASSES.get(channelType);
        if (channelClass == null) {
            throw new IllegalArgumentException("Unsupported channel type: " + channelType);
        }
        
        return channelClass;
    }
    
    /**
     * 验证插件配置
     */
    private void validatePluginConfig() {
        // 验证必要配置
        if (config != null) {
            // 这里可以添加具体的配置验证逻辑
            if (config.containsKey("maxChannels")) {
                Object maxChannels = config.get("maxChannels");
                if (maxChannels instanceof Number) {
                    int max = ((Number) maxChannels).intValue();
                    if (max <= 0) {
                        throw new IllegalArgumentException("maxChannels must be positive");
                    }
                }
            }
        }
    }
    
    /**
     * 获取日志记录器
     */
    private org.slf4j.Logger logger() {
        return org.slf4j.LoggerFactory.getLogger(getClass());
    }
    
    // === 静态工厂方法 ===
    
    /**
     * 创建内置插件实例
     */
    public static DefaultInteractionPlugin createBuiltinPlugin() {
        Map<String, Object> config = new HashMap<>();
        config.put("autoLoad", true);
        config.put("maxChannels", 100);
        config.put("enableHealthCheck", true);
        
        return new DefaultInteractionPlugin(
            "builtin-interaction",
            "Builtin Interaction Plugin",
            "1.0.0",
            config
        );
    }
}