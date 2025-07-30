package com.ai.infrastructure.agent.interaction.plugin;

import com.ai.infrastructure.agent.interaction.channel.InteractionChannel;
import com.ai.infrastructure.agent.interaction.factory.ChannelFactory;
import com.ai.infrastructure.agent.interaction.model.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 插件管理器
 */
public class PluginManager implements PluginAware {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    
    private final Map<String, InteractionPlugin> plugins = new ConcurrentHashMap<>();
    private final List<InteractionPlugin> pluginList = new CopyOnWriteArrayList<>();
    private final ChannelFactory channelFactory;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    // 配置监听器
    private final List<PluginConfigurationListener> configListeners = new ArrayList<>();
    
    // 插件扫描器
    private final PluginScanner pluginScanner = new PluginScanner();
    
    // 自动加载标志
    private final AtomicBoolean autoLoadEnabled = new AtomicBoolean(true);
    
    public PluginManager() {
        this.channelFactory = ChannelFactory.getInstance();
    }
    
    @Override
    public void registerPlugin(InteractionPlugin plugin) {
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        
        try {
            logger.info("Registering plugin: {} v{}", plugin.getName(), plugin.getVersion());
            
            // 验证插件ID唯一性
            if (plugins.containsKey(plugin.getPluginId())) {
                logger.error("Plugin ID already exists: {}", plugin.getPluginId());
                throw new IllegalArgumentException("Plugin ID already exists: " + plugin.getPluginId());
            }
            
            // 初始化插件
            plugin.initialize();
            
            // 注册插件
            plugins.put(plugin.getPluginId(), plugin);
            pluginList.add(plugin);
            
            // 通知配置监听器
            notifyPluginRegistered(plugin);
            
            logger.info("Plugin registered successfully: {}", plugin.getPluginId());
            
        } catch (Exception e) {
            logger.error("Failed to register plugin: " + plugin.getPluginId(), e);
            throw new RuntimeException("Failed to register plugin: " + plugin.getPluginId(), e);
        }
    }
    
    @Override
    public void unregisterPlugin(String pluginId) {
        Objects.requireNonNull(pluginId, "Plugin ID cannot be null");
        
        try {
            logger.info("Unregistering plugin: {}", pluginId);
            
            InteractionPlugin plugin = plugins.remove(pluginId);
            if (plugin == null) {
                logger.warn("Plugin not found: {}", pluginId);
                return;
            }
            
            pluginList.remove(plugin);
            
            // 销毁插件
            plugin.destroy();
            
            // 通知配置监听器
            notifyPluginUnregistered(plugin);
            
            logger.info("Plugin unregistered successfully: {}", pluginId);
            
        } catch (Exception e) {
            logger.error("Failed to unregister plugin: " + pluginId, e);
            throw new RuntimeException("Failed to unregister plugin: " + pluginId, e);
        }
    }
    
    @Override
    public InteractionPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }
    
    @Override
    public List<InteractionPlugin> getAllPlugins() {
        return new ArrayList<>(pluginList);
    }
    
    /**
     * 根据渠道类型获取支持的插件
     */
    public List<InteractionPlugin> getPluginsForChannelType(ChannelType channelType) {
        List<InteractionPlugin> result = new ArrayList<>();
        for (InteractionPlugin plugin : pluginList) {
            for (ChannelType supportedType : plugin.getSupportedChannelTypes()) {
                if (supportedType == channelType) {
                    result.add(plugin);
                    break;
                }
            }
        }
        return result;
    }
    
    /**
     * 创建渠道实例
     */
    public InteractionChannel createChannel(String channelId, ChannelType channelType, Map<String, Object> config) {
        List<InteractionPlugin> plugins = getPluginsForChannelType(channelType);
        if (plugins.isEmpty()) {
            logger.error("No plugin found for channel type: {}", channelType);
            throw new IllegalArgumentException("No plugin found for channel type: " + channelType);
        }
        
        // 使用第一个可用的插件
        for (InteractionPlugin plugin : plugins) {
            if (plugin.isAvailable()) {
                return plugin.createChannel(channelId, config);
            }
        }
        
        logger.error("No available plugin for channel type: {}", channelType);
        throw new IllegalStateException("No available plugin for channel type: " + channelType);
    }
    
    /**
     * 检查插件是否支持指定的渠道类型
     */
    public boolean isChannelTypeSupported(ChannelType channelType) {
        return !getPluginsForChannelType(channelType).isEmpty();
    }
    
    /**
     * 添加配置监听器
     */
    public void addConfigurationListener(PluginConfigurationListener listener) {
        configListeners.add(listener);
    }
    
    /**
     * 移除配置监听器
     */
    public void removeConfigurationListener(PluginConfigurationListener listener) {
        configListeners.remove(listener);
    }
    
    /**
     * 从JAR文件动态加载插件
     */
    public void loadPluginFromJar(String jarPath) {
        try {
            logger.info("Loading plugin from JAR: {}", jarPath);
            
            // 创建类加载器
            URL jarUrl = new URL("file:" + jarPath);
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl});
            
            // 加载插件类
            Class<?> pluginClass = classLoader.loadClass("com.ai.infrastructure.agent.interaction.plugin.DefaultInteractionPlugin");
            
            // 实例化插件
            InteractionPlugin plugin = (InteractionPlugin) pluginClass.getDeclaredConstructor().newInstance();
            
            // 注册插件
            registerPlugin(plugin);
            
            logger.info("Plugin loaded successfully from JAR: {}", jarPath);
            
        } catch (Exception e) {
            logger.error("Failed to load plugin from JAR: " + jarPath, e);
            throw new RuntimeException("Failed to load plugin from JAR: " + jarPath, e);
        }
    }
    
    /**
     * 初始化插件管理器
     */
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            logger.info("Initializing PluginManager");
            
            // 扫描并加载内置插件
            scanAndLoadBuiltinPlugins();
            
            // 启动自动扫描
            if (autoLoadEnabled.get()) {
                startAutoScan();
            }
            
            logger.info("PluginManager initialized successfully");
        }
    }
    
    /**
     * 销毁插件管理器
     */
    public void shutdown() {
        logger.info("Shutting down PluginManager");
        
        // 注销所有插件
        for (String pluginId : new ArrayList<>(plugins.keySet())) {
            try {
                unregisterPlugin(pluginId);
            } catch (Exception e) {
                logger.error("Failed to shutdown plugin: " + pluginId, e);
            }
        }
        
        plugins.clear();
        pluginList.clear();
        initialized.set(false);
        logger.info("PluginManager shutdown completed");
    }
    
    /**
     * 扫描并加载内置插件
     */
    private void scanAndLoadBuiltinPlugins() {
        try {
            // 扫描插件目录
            List<InteractionPlugin> discoveredPlugins = pluginScanner.scanPlugins();
            
            // 加载发现的插件
            for (InteractionPlugin plugin : discoveredPlugins) {
                try {
                    registerPlugin(plugin);
                    logger.info("Builtin plugin loaded: {} v{}", plugin.getName(), plugin.getVersion());
                } catch (Exception e) {
                    logger.warn("Failed to load builtin plugin: " + plugin.getPluginId(), e);
                }
            }
            
            // 如果没有发现插件，加载默认插件
            if (discoveredPlugins.isEmpty()) {
                loadDefaultPlugins();
            }
            
        } catch (Exception e) {
            logger.error("Failed to scan builtin plugins", e);
            // 回退到加载默认插件
            loadDefaultPlugins();
        }
    }
    
    /**
     * 加载默认插件
     */
    private void loadDefaultPlugins() {
        try {
            DefaultInteractionPlugin defaultPlugin = DefaultInteractionPlugin.createBuiltinPlugin();
            registerPlugin(defaultPlugin);
            logger.info("Default plugin loaded: {} v{}", defaultPlugin.getName(), defaultPlugin.getVersion());
        } catch (Exception e) {
            logger.error("Failed to load default plugins", e);
        }
    }
    
    /**
     * 启动自动扫描
     */
    private void startAutoScan() {
        Thread autoScanThread = new Thread(() -> {
            try {
                while (initialized.get()) {
                    Thread.sleep(30000); // 30秒扫描一次
                    if (initialized.get()) {
                        autoLoadNewPlugins();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        autoScanThread.setDaemon(true);
        autoScanThread.setName("Plugin-Auto-Scanner");
        autoScanThread.start();
        logger.info("Auto scan started for new plugins");
    }
    
    /**
     * 自动加载新插件
     */
    private void autoLoadNewPlugins() {
        try {
            List<InteractionPlugin> newPlugins = pluginScanner.scanPlugins();
            for (InteractionPlugin plugin : newPlugins) {
                if (!plugins.containsKey(plugin.getPluginId())) {
                    try {
                        registerPlugin(plugin);
                        logger.info("Auto-loaded new plugin: {} v{}", plugin.getName(), plugin.getVersion());
                    } catch (Exception e) {
                        logger.warn("Failed to auto-load new plugin: " + plugin.getPluginId(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Auto scan failed", e);
        }
    }
    
    /**
     * 通知插件已注册
     */
    private void notifyPluginRegistered(InteractionPlugin plugin) {
        for (PluginConfigurationListener listener : configListeners) {
            listener.onPluginRegistered(plugin);
        }
    }
    
    /**
     * 通知插件已注销
     */
    private void notifyPluginUnregistered(InteractionPlugin plugin) {
        for (PluginConfigurationListener listener : configListeners) {
            listener.onPluginUnregistered(plugin);
        }
    }
}