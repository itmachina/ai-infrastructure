package com.ai.infrastructure.core;

import com.ai.infrastructure.steering.RealtimeSteeringSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI基础设施主应用类
 */
public class AIInfrastructureApplication {
    private static final Logger logger = LoggerFactory.getLogger(AIInfrastructureApplication.class);
    
    private RealtimeSteeringSystem realtimeSteeringSystem;
    
    public AIInfrastructureApplication(String apiKey) {
        this.realtimeSteeringSystem = new RealtimeSteeringSystem(apiKey);
    }
    
    /**
     * 启动应用
     */
    public void start() {
        logger.info("AI Infrastructure Application started");
        realtimeSteeringSystem.start();
        logger.info("Realtime Steering System initialized and started");
    }
    
    /**
     * 执行任务 - 完整实现
     * @param task 任务描述
     * @return 执行结果
     */
    public void executeTask(String task) {
        logger.debug("Executing task: {}", task);
        
        try {
            // 检查系统是否已关闭
            if (realtimeSteeringSystem == null || realtimeSteeringSystem.isClosed()) {
                logger.error("RealtimeSteeringSystem is not available");
                return;
            }
            
            // 检查任务是否为空
            if (task == null || task.trim().isEmpty()) {
                return;
            }
            
            // 通过实时转向系统处理任务
            logger.info("Sending task to RealtimeSteeringSystem: {}", task);
            realtimeSteeringSystem.sendInput(task);
        } catch (Exception e) {
            logger.error("Error in task execution: {}", e.getMessage(), e);
        }
    }

    
    /**
     * 获取实时转向系统
     * @return 实时转向系统
     */
    public RealtimeSteeringSystem getRealtimeSteeringSystem() {
        return realtimeSteeringSystem;
    }
    
    /**
     * 关闭应用
     */
    public void shutdown() {
        logger.info("AI Infrastructure Application shutting down");
        if (realtimeSteeringSystem != null) {
            realtimeSteeringSystem.close();
        }
    }
}