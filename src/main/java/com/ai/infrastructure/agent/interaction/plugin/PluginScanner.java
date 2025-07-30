package com.ai.infrastructure.agent.interaction.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 插件扫描器
 */
public class PluginScanner {
    private static final Logger logger = LoggerFactory.getLogger(PluginScanner.class);
    
    private final List<String> pluginScanPaths = new ArrayList<>();
    private final Map<String, String> pluginMetadata = new HashMap<>();
    
    public PluginScanner() {
        // 默认扫描路径
        pluginScanPaths.add("plugins");
        pluginScanPaths.add("lib");
        pluginScanPaths.add("ext");
    }
    
    /**
     * 添加扫描路径
     */
    public void addScanPath(String path) {
        if (path != null && !pluginScanPaths.contains(path)) {
            pluginScanPaths.add(path);
        }
    }
    
    /**
     * 扫描插件
     */
    public List<InteractionPlugin> scanPlugins() {
        List<InteractionPlugin> foundPlugins = new ArrayList<>();
        
        logger.info("Starting plugin scan in paths: {}", pluginScanPaths);
        
        for (String scanPath : pluginScanPaths) {
            try {
                List<InteractionPlugin> pluginsInPath = scanPluginsInPath(scanPath);
                foundPlugins.addAll(pluginsInPath);
                logger.info("Found {} plugins in path: {}", pluginsInPath.size(), scanPath);
            } catch (Exception e) {
                logger.warn("Failed to scan plugins in path: " + scanPath, e);
            }
        }
        
        logger.info("Total plugins found: {}", foundPlugins.size());
        return foundPlugins;
    }
    
    /**
     * 在指定路径扫描插件
     */
    private List<InteractionPlugin> scanPluginsInPath(String path) throws Exception {
        List<InteractionPlugin> plugins = new ArrayList<>();
        File scanDir = new File(path);
        
        if (!scanDir.exists()) {
            logger.debug("Scan path does not exist: {}", path);
            return plugins;
        }
        
        // 扫描JAR文件
        File[] jarFiles = scanDir.listFiles((dir, name) -> 
            name.endsWith(".jar") && !name.contains("sources"));
        
        if (jarFiles != null) {
            for (File jarFile : jarFiles) {
                try {
                    List<InteractionPlugin> jarPlugins = scanJarFile(jarFile);
                    plugins.addAll(jarPlugins);
                } catch (Exception e) {
                    logger.warn("Failed to scan JAR file: " + jarFile.getName(), e);
                }
            }
        }
        
        return plugins;
    }
    
    /**
     * 扫描JAR文件中的插件
     */
    private List<InteractionPlugin> scanJarFile(File jarFile) throws Exception {
        List<InteractionPlugin> plugins = new ArrayList<>();
        
        try (JarFile jar = new JarFile(jarFile)) {
            // 创建类加载器
            URL jarUrl = jarFile.toURI().toURL();
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl});
            
            // 查找插件类
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().endsWith(".class") && 
                    !entry.getName().contains("$") &&
                    !entry.getName().startsWith("META-INF/")) {
                    
                    String className = entry.getName()
                        .replace('/', '.')
                        .replace(".class", "");
                    
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        
                        // 检查是否实现了InteractionPlugin接口
                        if (InteractionPlugin.class.isAssignableFrom(clazz) && 
                            !clazz.isInterface() &&
                            !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                            
                            // 实例化插件
                            InteractionPlugin plugin = (InteractionPlugin) clazz.getDeclaredConstructor().newInstance();
                            
                            // 验证插件
                            if (validatePlugin(plugin)) {
                                plugins.add(plugin);
                                logger.info("Found plugin in JAR {}: {} v{}", 
                                           jarFile.getName(), plugin.getName(), plugin.getVersion());
                            } else {
                                logger.warn("Invalid plugin found in JAR {}: {}", 
                                           jarFile.getName(), className);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to load class from JAR {}: {}", 
                                   jarFile.getName(), className, e);
                    }
                }
            }
        }
        
        return plugins;
    }
    
    /**
     * 验证插件
     */
    private boolean validatePlugin(InteractionPlugin plugin) {
        try {
            // 检查插件ID
            if (plugin.getPluginId() == null || plugin.getPluginId().trim().isEmpty()) {
                logger.warn("Plugin has invalid ID: {}", plugin.getClass().getName());
                return false;
            }
            
            // 检查插件名称
            if (plugin.getName() == null || plugin.getName().trim().isEmpty()) {
                logger.warn("Plugin has invalid name: {}", plugin.getPluginId());
                return false;
            }
            
            // 检查支持的渠道类型
            if (plugin.getSupportedChannelTypes() == null || 
                plugin.getSupportedChannelTypes().length == 0) {
                logger.warn("Plugin {} supports no channel types", plugin.getPluginId());
                return false;
            }
            
            // 尝试初始化插件
            plugin.initialize();
            
            // 检查是否可用
            if (!plugin.isAvailable()) {
                logger.warn("Plugin {} is not available", plugin.getPluginId());
                plugin.destroy();
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.warn("Plugin validation failed for: {}", plugin.getPluginId(), e);
            try {
                plugin.destroy();
            } catch (Exception ex) {
                logger.warn("Failed to destroy plugin during validation: {}", plugin.getPluginId(), ex);
            }
            return false;
        }
    }
    
    /**
     * 获取插件元数据
     */
    public Map<String, String> getPluginMetadata() {
        return new HashMap<>(pluginMetadata);
    }
    
    /**
     * 注册插件元数据
     */
    public void registerPluginMetadata(String pluginId, String metadata) {
        pluginMetadata.put(pluginId, metadata);
    }
    
    /**
     * 清除扫描路径
     */
    public void clearScanPaths() {
        pluginScanPaths.clear();
    }
    
    /**
     * 检查插件是否已加载
     */
    public boolean isPluginLoaded(String pluginId) {
        return pluginMetadata.containsKey(pluginId);
    }
}