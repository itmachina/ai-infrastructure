package com.ai.infrastructure.agent.unified.pool;

import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import com.ai.infrastructure.tools.ToolEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent组件池，实现组件的池化和共享管理
 * 解决原有架构中组件重复实例化的问题
 */
public class AgentComponentPool {
    private static final Logger logger = LoggerFactory.getLogger(AgentComponentPool.class);
    
    // 组件实例池
    private final Map<String, MemoryManager> memoryManagerPool;
    private final Map<String, SecurityManager> securityManagerPool;
    private final Map<String, ToolEngine> toolEnginePool;
    
    // 使用计数
    private final AtomicInteger memoryManagerCount;
    private final AtomicInteger securityManagerCount;
    private final AtomicInteger toolEngineCount;
    
    // 池配置
    private final int maxPoolSize;
    private final boolean enablePooling;
    
    public AgentComponentPool() {
        this.maxPoolSize = 10;
        this.enablePooling = true;
        
        this.memoryManagerPool = new ConcurrentHashMap<>();
        this.securityManagerPool = new ConcurrentHashMap<>();
        this.toolEnginePool = new ConcurrentHashMap<>();
        
        this.memoryManagerCount = new AtomicInteger(0);
        this.securityManagerCount = new AtomicInteger(0);
        this.toolEngineCount = new AtomicInteger(0);
        
        logger.info("AgentComponentPool initialized with maxPoolSize: {}", maxPoolSize);
    }
    
    public AgentComponentPool(int maxPoolSize, boolean enablePooling) {
        this.maxPoolSize = maxPoolSize;
        this.enablePooling = enablePooling;
        
        this.memoryManagerPool = new ConcurrentHashMap<>();
        this.securityManagerPool = new ConcurrentHashMap<>();
        this.toolEnginePool = new ConcurrentHashMap<>();
        
        this.memoryManagerCount = new AtomicInteger(0);
        this.securityManagerCount = new AtomicInteger(0);
        this.toolEngineCount = new AtomicInteger(0);
        
        logger.info("AgentComponentPool initialized with maxPoolSize: {}, pooling: {}", 
                   maxPoolSize, enablePooling);
    }
    
    /**
     * 获取MemoryManager实例
     */
    public MemoryManager getMemoryManager() {
        if (!enablePooling) {
            return new MemoryManager();
        }
        
        // 尝试从池中获取可用实例
        MemoryManager instance = findAvailableInstance(memoryManagerPool);
        if (instance != null) {
            logger.debug("Retrieved MemoryManager from pool");
            return instance;
        }
        
        // 池中没有可用实例，创建新实例
        if (memoryManagerCount.get() < maxPoolSize) {
            MemoryManager newInstance = new MemoryManager();
            memoryManagerPool.put("memory_" + memoryManagerCount.incrementAndGet(), newInstance);
            logger.debug("Created new MemoryManager, total: {}", memoryManagerCount.get());
            return newInstance;
        }
        
        // 达到最大池大小，创建非池化实例
        logger.warn("MemoryManager pool reached max size {}, creating non-pooled instance", 
                   maxPoolSize);
        return new MemoryManager();
    }
    
    /**
     * 获取SecurityManager实例
     */
    public SecurityManager getSecurityManager() {
        if (!enablePooling) {
            return new SecurityManager();
        }
        
        SecurityManager instance = findAvailableInstance(securityManagerPool);
        if (instance != null) {
            logger.debug("Retrieved SecurityManager from pool");
            return instance;
        }
        
        if (securityManagerCount.get() < maxPoolSize) {
            SecurityManager newInstance = new SecurityManager();
            securityManagerPool.put("security_" + securityManagerCount.incrementAndGet(), newInstance);
            logger.debug("Created new SecurityManager, total: {}", securityManagerCount.get());
            return newInstance;
        }
        
        logger.warn("SecurityManager pool reached max size {}, creating non-pooled instance", 
                   maxPoolSize);
        return new SecurityManager();
    }
    
    /**
     * 获取ToolEngine实例
     */
    public ToolEngine getToolEngine() {
        if (!enablePooling) {
            return new ToolEngine();
        }
        
        ToolEngine instance = findAvailableInstance(toolEnginePool);
        if (instance != null) {
            logger.debug("Retrieved ToolEngine from pool");
            return instance;
        }
        
        if (toolEngineCount.get() < maxPoolSize) {
            ToolEngine newInstance = new ToolEngine();
            toolEnginePool.put("tool_" + toolEngineCount.incrementAndGet(), newInstance);
            logger.debug("Created new ToolEngine, total: {}", toolEngineCount.get());
            return newInstance;
        }
        
        logger.warn("ToolEngine pool reached max size {}, creating non-pooled instance", 
                   maxPoolSize);
        return new ToolEngine();
    }
    
    /**
     * 从池中查找可用实例
     */
    private <T> T findAvailableInstance(Map<String, T> pool) {
        // 简化实现：返回第一个可用实例
        // 实际实现应该考虑实例的使用状态和健康检查
        return pool.isEmpty() ? null : pool.values().iterator().next();
    }
    
    /**
     * 回收组件到池中（如果支持）
     */
    public void recycleComponent(Object component) {
        if (!enablePooling) {
            return;
        }
        
        if (component instanceof MemoryManager) {
            logger.debug("Attempting to recycle MemoryManager");
            // 这里可以实现更复杂的回收逻辑
        } else if (component instanceof SecurityManager) {
            logger.debug("Attempting to recycle SecurityManager");
        } else if (component instanceof ToolEngine) {
            logger.debug("Attempting to recycle ToolEngine");
        }
    }
    
    /**
     * 获取池统计信息
     */
    public PoolStatistics getStatistics() {
        return new PoolStatistics(
            memoryManagerCount.get(),
            securityManagerCount.get(),
            toolEngineCount.get(),
            maxPoolSize,
            enablePooling
        );
    }
    
    /**
     * 重置池
     */
    public void reset() {
        logger.info("Resetting AgentComponentPool");
        
        memoryManagerPool.clear();
        securityManagerPool.clear();
        toolEnginePool.clear();
        
        memoryManagerCount.set(0);
        securityManagerCount.set(0);
        toolEngineCount.set(0);
    }
    
    /**
     * 关闭池，释放资源
     */
    public void shutdown() {
        logger.info("Shutting down AgentComponentPool");
        
        // 关闭所有组件
        memoryManagerPool.values().forEach(manager -> {
            if (manager != null) {
                // 这里可以实现具体的关闭逻辑
            }
        });
        
        securityManagerPool.values().forEach(manager -> {
            if (manager != null) {
                // 这里可以实现具体的关闭逻辑
            }
        });
        
        toolEnginePool.values().forEach(engine -> {
            if (engine != null) {
                // 这里可以实现具体的关闭逻辑
            }
        });
        
        reset();
    }
    
    /**
     * 池统计信息
     */
    public static class PoolStatistics {
        private final int memoryManagerCount;
        private final int securityManagerCount;
        private final int toolEngineCount;
        private final int maxPoolSize;
        private final boolean poolingEnabled;
        
        public PoolStatistics(int memoryManagerCount, int securityManagerCount, 
                             int toolEngineCount, int maxPoolSize, boolean poolingEnabled) {
            this.memoryManagerCount = memoryManagerCount;
            this.securityManagerCount = securityManagerCount;
            this.toolEngineCount = toolEngineCount;
            this.maxPoolSize = maxPoolSize;
            this.poolingEnabled = poolingEnabled;
        }
        
        public int getMemoryManagerCount() {
            return memoryManagerCount;
        }
        
        public int getSecurityManagerCount() {
            return securityManagerCount;
        }
        
        public int getToolEngineCount() {
            return toolEngineCount;
        }
        
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        public boolean isPoolingEnabled() {
            return poolingEnabled;
        }
        
        public int getTotalInstances() {
            return memoryManagerCount + securityManagerCount + toolEngineCount;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PoolStatistics[MemoryManagers: %d, SecurityManagers: %d, ToolEngines: %d, MaxPoolSize: %d, Pooling: %b]",
                memoryManagerCount, securityManagerCount, toolEngineCount, maxPoolSize, poolingEnabled
            );
        }
    }
}