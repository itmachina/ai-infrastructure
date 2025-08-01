package com.ai.infrastructure.agent.unified;

import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.model.OpenAIModelClient;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.agent.unified.coordinator.AgentCoordinator;
import com.ai.infrastructure.agent.unified.pool.AgentComponentPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一Agent上下文，提供组件池化和共享管理
 */
public class UnifiedAgentContext {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedAgentContext.class);
    private static UnifiedAgentContext instance;
    
    // 组件池
    private final AgentComponentPool componentPool;
    
    // 全局配置
    private final Map<String, Object> globalConfig;
    private final AgentCoordinator coordinator;
    private OpenAIModelClient modelClient;
    
    // 私有构造函数
    private UnifiedAgentContext() {
        this.componentPool = new AgentComponentPool();
        this.globalConfig = new ConcurrentHashMap<>();
        this.coordinator = new AgentCoordinator(this);
        
        initializeGlobalConfig();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized UnifiedAgentContext getInstance() {
        if (instance == null) {
            instance = new UnifiedAgentContext();
        }
        return instance;
    }
    
    /**
     * 初始化全局配置
     */
    private void initializeGlobalConfig() {
        globalConfig.put("maxAgents", 50);
        globalConfig.put("defaultTimeout", 30000);
        globalConfig.put("memoryThreshold", 0.8);
        globalConfig.put("enableCollaboration", true);
        globalConfig.put("enableMonitoring", true);
        
        // 初始化模型客户端
        initializeModelClient();
    }
    
    /**
     * 初始化模型客户端
     */
    private void initializeModelClient() {
        try {
            String apiKey = System.getenv("AI_API_KEY");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                this.modelClient = new OpenAIModelClient(apiKey);
                logger.info("Model client initialized successfully");
            } else {
                logger.warn("OPENAI_API_KEY not found, model client not initialized");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize model client: {}", e.getMessage());
        }
    }
    
    /**
     * 创建新的Agent上下文（用于独立环境）
     */
    public static UnifiedAgentContext createContext() {
        return new UnifiedAgentContext();
    }
    
    /**
     * 获取内存管理器（从池中获取）
     */
    public MemoryManager getMemoryManager() {
        return componentPool.getMemoryManager();
    }
    
    /**
     * 获取安全管理器（从池中获取）
     */
    public SecurityManager getSecurityManager() {
        return componentPool.getSecurityManager();
    }
    
    /**
     * 获取工具引擎（从池中获取）
     */
    public ToolEngine getToolEngine() {
        return componentPool.getToolEngine();
    }
    
    /**
     * 获取协调器
     */
    public AgentCoordinator getCoordinator() {
        return coordinator;
    }
    
    /**
     * 获取组件池
     */
    public AgentComponentPool getComponentPool() {
        return componentPool;
    }
    
    /**
     * 获取全局配置
     */
    public Object getConfig(String key) {
        return globalConfig.get(key);
    }
    
    /**
     * 设置全局配置
     */
    public void setConfig(String key, Object value) {
        globalConfig.put(key, value);
    }
    
    /**
     * 获取所有配置
     */
    public Map<String, Object> getAllConfig() {
        return new ConcurrentHashMap<>(globalConfig);
    }
    
    /**
     * 获取模型客户端
     */
    public OpenAIModelClient getModelClient() {
        return modelClient;
    }
    
    /**
     * 设置模型客户端
     */
    public void setModelClient(OpenAIModelClient modelClient) {
        this.modelClient = modelClient;
    }
    
    /**
     * 重置上下文（谨慎使用）
     */
    public void reset() {
        componentPool.reset();
        globalConfig.clear();
        initializeGlobalConfig();
    }
    
    /**
     * 关闭上下文，释放资源
     */
    public void shutdown() {
        componentPool.shutdown();
        coordinator.shutdown();
    }
}