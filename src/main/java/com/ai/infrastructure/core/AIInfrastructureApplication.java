package com.ai.infrastructure.core;

import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.model.OpenAIModelClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * AI基础设施主应用类
 */
public class AIInfrastructureApplication {
    private static final Logger logger = LoggerFactory.getLogger(AIInfrastructureApplication.class);
    
    private MainAgent mainAgent;
    
    public AIInfrastructureApplication() {
        this.mainAgent = new MainAgent("main-001", "Main AI Agent");
    }
    
    /**
     * 启动应用
     */
    public void start() {
        logger.info("AI Infrastructure Application started");
        logger.info("Main Agent initialized: " + mainAgent.getName());
    }
    
    /**
     * 执行任务
     * @param task 任务描述
     * @return 执行结果
     */
    public CompletableFuture<String> executeTask(String task) {
        logger.debug("Executing task: " + task);
        return mainAgent.executeTask(task);
    }
    
    /**
     * 设置OpenAI模型API密钥
     * @param apiKey API密钥
     */
    public void setOpenAIModelApiKey(String apiKey) {
        mainAgent.setOpenAIModelApiKey(apiKey);
        logger.info("OpenAI model API key set");
    }
    
    /**
     * 获取主Agent
     * @return 主Agent
     */
    public MainAgent getMainAgent() {
        return mainAgent;
    }
    
    /**
     * 关闭应用
     */
    public void shutdown() {
        logger.info("AI Infrastructure Application shutting down");
    }
}