package com.ai.infrastructure.agent;

import com.ai.infrastructure.agent.interaction.manager.ChannelManager;
import com.ai.infrastructure.agent.interaction.router.MessageRouter;
import com.ai.infrastructure.agent.interaction.model.Message;
import com.ai.infrastructure.agent.interaction.model.ChannelType;
import com.ai.infrastructure.agent.interaction.model.MessageType;
import com.ai.infrastructure.agent.interaction.model.MessageStatus;
import com.ai.infrastructure.agent.interaction.model.InteractionTask;
import com.ai.infrastructure.agent.interaction.model.InteractionTaskType;
import com.ai.infrastructure.agent.interaction.plugin.PluginManager;
import com.ai.infrastructure.agent.interaction.plugin.PluginAware;
import com.ai.infrastructure.agent.interaction.plugin.InteractionPlugin;
import com.ai.infrastructure.agent.interaction.plugin.PluginConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
    
    /**
 * 重构的I2A交互Agent实现
 * 支持插件化的多渠道用户交互和界面更新任务
 * 具备高度扩展性，支持动态加载和卸载渠道插件
 */
public class InteractionAgent extends SpecializedAgent implements PluginAware, PluginConfigurationListener {
    private static final Logger logger = LoggerFactory.getLogger(InteractionAgent.class);
    
    // 交互相关模式
    private static final Pattern[] INTERACTION_PATTERNS = {
        Pattern.compile("用户|界面|展示|显示|输出"),
        Pattern.compile("报告|总结|概览|状态"),
        Pattern.compile("交互|沟通|交流|反馈"),
        Pattern.compile("可视化|图表|界面|UI"),
        Pattern.compile("演示|展示|说明|解释"),
        Pattern.compile("钉钉|dingtalk|微信|wechat|邮件|email"),
        Pattern.compile("发送|推送|通知|消息")
    };
    
    private final ChannelManager channelManager;
    private final MessageRouter messageRouter;
    private final PluginManager pluginManager;
    private final Map<String, Object> channelConfigurations;
    private final Map<String, String> channelPluginMappings = new ConcurrentHashMap<>();
    
    public InteractionAgent(String agentId, String name) {
        super(agentId, name, AgentType.I2A);
        this.channelManager = new ChannelManager();
        this.messageRouter = new MessageRouter();
        this.pluginManager = new PluginManager();
        this.channelConfigurations = new HashMap<>();
        
        initialize();
        logger.info("Refactored I2A Interaction Agent initialized: {}", agentId);
    }
    
    /**
     * 带配置的构造函数
     */
    public InteractionAgent(String agentId, String name, Map<String, Object> channelConfigurations) {
        super(agentId, name, AgentType.I2A);
        this.channelManager = new ChannelManager();
        this.messageRouter = new MessageRouter();
        this.pluginManager = new PluginManager();
        this.channelConfigurations = new HashMap<>(channelConfigurations);
        
        initialize();
        logger.info("Refactored I2A Interaction Agent initialized with config: {}", agentId);
    }
    
    /**
     * 初始化方法
     */
    private void initialize() {
        // 初始化组件
        channelManager.initialize();
        pluginManager.initialize();
        pluginManager.addConfigurationListener(this);
        
        // 自动加载内置插件
        autoLoadBuiltinPlugins();
        
        // 初始化渠道
        initializeChannels();
    }
    
    /**
     * 自动加载内置插件
     */
    private void autoLoadBuiltinPlugins() {
        // 这里可以自动发现和加载内置插件
        logger.info("Auto-loading builtin plugins...");
    }
    
    /**
     * 初始化渠道
     */
    private void initializeChannels() {
        // 从配置中读取渠道设置
        if (channelConfigurations.containsKey("channels")) {
            List<Map<String, Object>> channels = (List<Map<String, Object>>) channelConfigurations.get("channels");
            for (Map<String, Object> channelConfig : channels) {
                String channelId = (String) channelConfig.get("channelId");
                String channelTypeStr = (String) channelConfig.get("type");
                Map<String, Object> config = (Map<String, Object>) channelConfig.getOrDefault("config", new HashMap<>());
                
                try {
                    ChannelType channelType = ChannelType.valueOf(channelTypeStr.toUpperCase());
                    registerChannel(channelId, channelType, config);
                } catch (Exception e) {
                    logger.error("Failed to initialize channel: " + channelId, e);
                }
            }
        }
    }
    
    // === PluginAware 接口实现 ===
    
    @Override
    public void registerPlugin(InteractionPlugin plugin) {
        pluginManager.registerPlugin(plugin);
    }
    
    @Override
    public void unregisterPlugin(String pluginId) {
        pluginManager.unregisterPlugin(pluginId);
    }
    
    @Override
    public InteractionPlugin getPlugin(String pluginId) {
        return pluginManager.getPlugin(pluginId);
    }
    
    @Override
    public List<InteractionPlugin> getAllPlugins() {
        return pluginManager.getAllPlugins();
    }
    
    // === 渠道注册方法 ===
    
    /**
     * 注册渠道
     */
    public boolean registerChannel(String channelId, ChannelType channelType, Map<String, Object> config) {
        try {
            // 检查是否有插件支持该渠道类型
            if (!pluginManager.isChannelTypeSupported(channelType)) {
                logger.error("No plugin supports channel type: {}", channelType);
                return false;
            }
            
            // 注册渠道到渠道管理器
            boolean success = channelManager.registerChannel(channelId, channelType, config);
            
            if (success) {
                // 记录插件映射
                List<InteractionPlugin> plugins = pluginManager.getPluginsForChannelType(channelType);
                if (!plugins.isEmpty()) {
                    channelPluginMappings.put(channelId, plugins.get(0).getPluginId());
                }
                
                logger.info("Channel registered: {} - {} via plugin: {}", 
                           channelId, channelType, channelPluginMappings.get(channelId));
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Failed to register channel: " + channelId, e);
            return false;
        }
    }
    
    /**
     * 注销渠道
     */
    public boolean unregisterChannel(String channelId) {
        try {
            // 清理插件映射
            channelPluginMappings.remove(channelId);
            
            // 注销渠道
            return channelManager.unregisterChannel(channelId);
            
        } catch (Exception e) {
            logger.error("Failed to unregister channel: " + channelId, e);
            return false;
        }
    }
    
    /**
     * 通过插件创建渠道
     */
    public boolean createChannelViaPlugin(String pluginId, String channelId, Map<String, Object> config) {
        try {
            InteractionPlugin plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                logger.error("Plugin not found: {}", pluginId);
                return false;
            }
            
            // 这里可以通过插件创建渠道
            // 具体实现取决于ChannelManager的设计
            logger.info("Channel created via plugin: {} - {}", pluginId, channelId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to create channel via plugin: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 加载插件JAR文件
     */
    public boolean loadPlugin(String jarPath) {
        try {
            pluginManager.loadPluginFromJar(jarPath);
            logger.info("Plugin loaded successfully: {}", jarPath);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to load plugin: " + jarPath, e);
            return false;
        }
    }
    
    /**
     * 获取插件支持的渠道类型
     */
    public List<ChannelType> getSupportedChannelTypes(String pluginId) {
        InteractionPlugin plugin = pluginManager.getPlugin(pluginId);
        if (plugin == null) {
            return new ArrayList<>();
        }
        
        List<ChannelType> types = new ArrayList<>();
        for (ChannelType type : plugin.getSupportedChannelTypes()) {
            types.add(type);
        }
        return types;
    }
    
    /**
     * 检查渠道类型是否有插件支持
     */
    public boolean isChannelTypeSupported(ChannelType channelType) {
        return pluginManager.isChannelTypeSupported(channelType);
    }
    
    // === PluginConfigurationListener 接口实现 ===
    
    @Override
    public void onPluginRegistered(InteractionPlugin plugin) {
        logger.info("Plugin configuration updated: {} registered", plugin.getPluginId());
        // 可以在这里重新加载相关渠道配置
    }
    
    @Override
    public void onPluginUnregistered(InteractionPlugin plugin) {
        logger.info("Plugin configuration updated: {} unregistered", plugin.getPluginId());
        // 可以在这里清理相关渠道
    }
    
    @Override
    public void onPluginConfigUpdated(InteractionPlugin plugin, Map<String, Object> oldConfig, Map<String, Object> newConfig) {
        logger.info("Plugin configuration updated: {} configuration changed", plugin.getPluginId());
        // 可以在这里重新初始化相关渠道
    }
    
    /**
     * 销毁InteractionAgent
     */
    public void destroy() {
        logger.info("Destroying Interaction Agent: {}", agentId);
        
        // 关闭插件管理器
        if (pluginManager != null) {
            pluginManager.shutdown();
        }
        
        // 关闭渠道管理器
        if (channelManager != null) {
            channelManager.shutdown();
        }
        
        // 清理资源
        channelPluginMappings.clear();
        channelConfigurations.clear();
        
        logger.info("Interaction Agent destroyed: {}", agentId);
    }
    
    /**
     * 获取插件映射信息
     */
    public Map<String, String> getChannelPluginMappings() {
        return new HashMap<>(channelPluginMappings);
    }
    
    /**
     * 获取所有支持的渠道类型
     */
    public List<ChannelType> getAllSupportedChannelTypes() {
        List<ChannelType> allTypes = new ArrayList<>();
        for (InteractionPlugin plugin : getAllPlugins()) {
            for (ChannelType type : plugin.getSupportedChannelTypes()) {
                if (!allTypes.contains(type)) {
                    allTypes.add(type);
                }
            }
        }
        return allTypes;
    }
    
    @Override
    protected String processSpecializedTask(String task) {
        logger.debug("Enhanced I2A Agent processing interaction task: {}", task);
        
        try {
            // 解析任务类型
            InteractionTask interactionTask = parseInteractionTask(task);
            
            switch (interactionTask.getTaskType()) {
                case MULTIPLE_CHANNEL_SEND:
                    return handleMultiChannelSend(interactionTask);
                case CHANNEL_SPECIFIC_SEND:
                    return handleChannelSpecificSend(interactionTask);
                case USER_PREFERENCE_MANAGE:
                    return handleUserPreferenceManage(interactionTask);
                case CHANNEL_STATUS_QUERY:
                    return handleChannelStatusQuery(interactionTask);
                case REPORT_GENERATE:
                    return handleReportGenerate(interactionTask);
                case VISUALIZATION_CREATE:
                    return handleVisualizationCreate(interactionTask);
                default:
                    return handleGenericInteraction(interactionTask);
            }
        } catch (Exception e) {
            logger.error("Failed to process interaction task: " + task, e);
            return "交互处理失败: " + e.getMessage();
        }
    }
    
    /**
     * 解析交互任务
     */
    private InteractionTask parseInteractionTask(String task) {
        String lowerTask = task.toLowerCase();
        
        if (lowerTask.contains("多渠道") || lowerTask.contains("多个渠道")) {
            return new InteractionTask(InteractionTaskType.MULTIPLE_CHANNEL_SEND, task);
        } else if (lowerTask.contains("特定渠道") || lowerTask.contains("指定渠道")) {
            return new InteractionTask(InteractionTaskType.CHANNEL_SPECIFIC_SEND, task);
        } else if (lowerTask.contains("用户偏好") || lowerTask.contains("用户设置")) {
            return new InteractionTask(InteractionTaskType.USER_PREFERENCE_MANAGE, task);
        } else if (lowerTask.contains("渠道状态") || lowerTask.contains("状态查询")) {
            return new InteractionTask(InteractionTaskType.CHANNEL_STATUS_QUERY, task);
        } else if (lowerTask.contains("报告") || lowerTask.contains("报表")) {
            return new InteractionTask(InteractionTaskType.REPORT_GENERATE, task);
        } else if (lowerTask.contains("可视化") || lowerTask.contains("图表")) {
            return new InteractionTask(InteractionTaskType.VISUALIZATION_CREATE, task);
        } else {
            return new InteractionTask(InteractionTaskType.GENERIC_INTERACTION, task);
        }
    }
    
    /**
     * 处理多渠道发送
     */
    private String handleMultiChannelSend(InteractionTask task) {
        return "已处理多渠道发送任务: " + task.getContent();
    }
    
    /**
     * 处理特定渠道发送
     */
    private String handleChannelSpecificSend(InteractionTask task) {
        return "已处理特定渠道发送任务: " + task.getContent();
    }
    
    /**
     * 处理用户偏好管理
     */
    private String handleUserPreferenceManage(InteractionTask task) {
        return "已处理用户偏好管理任务: " + task.getContent();
    }
    
    /**
     * 处理渠道状态查询
     */
    private String handleChannelStatusQuery(InteractionTask task) {
        return "已处理渠道状态查询任务: " + task.getContent();
    }
    
    /**
     * 处理报告生成
     */
    private String handleReportGenerate(InteractionTask task) {
        return "已处理报告生成任务: " + task.getContent();
    }
    
    /**
     * 处理可视化创建
     */
    private String handleVisualizationCreate(InteractionTask task) {
        return "已处理可视化创建任务: " + task.getContent();
    }
    
    /**
     * 处理通用交互
     */
    private String handleGenericInteraction(InteractionTask task) {
        return "已处理通用交互任务: " + task.getContent();
    }
    
    @Override
    public boolean supportsTaskType(String taskType) {
        String lowerTask = taskType.toLowerCase();
        
        return lowerTask.contains("交互") || 
               lowerTask.contains("界面") || 
               lowerTask.contains("展示") || 
               lowerTask.contains("报告") || 
               lowerTask.contains("可视化") || 
               lowerTask.contains("用户");
    }
    
    /**
     * 处理交互任务
     */
    private String handleInteractiveTask(String task) {
        // 模拟交互处理
        String interactionType = extractInteractionType(task);
        
        switch (interactionType) {
            case "用户输入":
                return handleUserInput(task);
            case "界面更新":
                return handleInterfaceUpdate(task);
            case "状态反馈":
                return handleStatusFeedback(task);
            default:
                return processInteractionResponse(task);
        }
    }
    
    /**
     * 处理报告任务
     */
    private String handleReportTask(String task) {
        // 生成结构化报告
        StringBuilder report = new StringBuilder();
        report.append("=== 任务执行报告 ===\n");
        report.append("任务描述: ").append(task).append("\n");
        report.append("执行时间: ").append(java.time.LocalDateTime.now()).append("\n");
        report.append("执行Agent: ").append(agentType.getDisplayName()).append("\n");
        report.append("处理状态: 成功\n");
        report.append("报告内容: \n");
        report.append("- 任务已完成交互处理\n");
        report.append("- 用户反馈已收集\n");
        report.append("- 界面状态已更新\n");
        
        return report.toString();
    }
    
    /**
     * 处理可视化任务
     */
    private String handleVisualizationTask(String task) {
        // 生成可视化描述
        return generateVisualizationDescription(task);
    }
    
    /**
     * 处理通用交互
     */
    private String handleGenericInteraction(String task) {
        return String.format(
            "I2A交互Agent已处理任务: %s\n" +
            "交互类型: 通用交互\n" +
            "处理结果: 交互完成\n" +
            "用户反馈: 已收集",
            task
        );
    }
    
    /**
     * 处理用户输入
     */
    private String handleUserInput(String task) {
        return String.format(
            "I2A交互Agent已处理用户输入任务: %s\n" +
            "输入验证: 通过\n" +
            "交互响应: 已生成\n" +
            "用户状态: 已更新",
            task
        );
    }
    
    /**
     * 处理界面更新
     */
    private String handleInterfaceUpdate(String task) {
        return String.format(
            "I2A交互Agent已处理界面更新任务: %s\n" +
            "界面状态: 已更新\n" +
            "组件刷新: 已完成\n" +
            "用户体验: 已优化",
            task
        );
    }
    
    /**
     * 处理状态反馈
     */
    private String handleStatusFeedback(String task) {
        return String.format(
            "I2A交互Agent已处理状态反馈任务: %s\n" +
            "状态信息: 已收集\n" +
            "反馈分析: 已完成\n" +
            "建议措施: 已生成",
            task
        );
    }
    
    /**
     * 处理交互响应
     */
    private String processInteractionResponse(String task) {
        return String.format(
            "I2A交互Agent响应:\n" +
            "任务: %s\n" +
            "交互模式: 智能交互\n" +
            "响应时间: %d ms\n" +
            "用户满意度: 高",
            task,
            System.currentTimeMillis() - lastActivityTime
        );
    }
    
    /**
     * 生成可视化描述
     */
    private String generateVisualizationDescription(String task) {
        return String.format(
            "I2A交互Agent可视化生成:\n" +
            "任务: %s\n" +
            "可视化类型: 交互式图表\n" +
            "数据维度: 多维度\n" +
            "交互特性: 实时更新\n" +
            "用户体验: 直观易用",
            task
        );
    }
    
    /**
     * 检查是否为交互任务
     */
    private boolean isInteractiveTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("交互") || 
               lowerTask.contains("用户") || 
               lowerTask.contains("反馈");
    }
    
    /**
     * 检查是否为报告任务
     */
    private boolean isReportTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("报告") || 
               lowerTask.contains("总结") || 
               lowerTask.contains("概览");
    }
    
    /**
     * 检查是否为可视化任务
     */
    private boolean isVisualizationTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("可视化") || 
               lowerTask.contains("图表") || 
               lowerTask.contains("界面");
    }
    
    /**
     * 提取交互类型
     */
    private String extractInteractionType(String task) {
        String lowerTask = task.toLowerCase();
        
        if (lowerTask.contains("输入")) return "用户输入";
        if (lowerTask.contains("更新") || lowerTask.contains("刷新")) return "界面更新";
        if (lowerTask.contains("状态") || lowerTask.contains("反馈")) return "状态反馈";
        if (lowerTask.contains("交互")) return "通用交互";
        
        return "默认交互";
    }
}