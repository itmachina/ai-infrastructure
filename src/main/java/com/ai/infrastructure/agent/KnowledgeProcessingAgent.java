package com.ai.infrastructure.agent;

import com.ai.infrastructure.agent.KnowledgeProcessor;
import com.ai.infrastructure.model.OpenAIModelClient;
import com.ai.infrastructure.tools.ToolEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * KN5知识处理Agent - AI增强版本
 * 集成大模型进行知识检索、推理和学习
 */
public class KnowledgeProcessingAgent extends SpecializedAgent {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeProcessingAgent.class);
    
    // 知识处理相关模式
    private static final Pattern[] KNOWLEDGE_PATTERNS = {
        Pattern.compile("知识|推理|学习|分析|决策|评估|优化|规划"),
        Pattern.compile("研究|调研|探索|发现"),
        Pattern.compile("预测|判断|推断|结论"),
        Pattern.compile("策略|方案|计划|设计"),
        Pattern.compile("智能|智慧|认知|理解")
    };
    
    private final KnowledgeProcessor processor;
    
    public KnowledgeProcessingAgent(String agentId, String name, OpenAIModelClient modelClient, ToolEngine toolEngine) {
        super(agentId, name, AgentType.KN5);
        this.processor = new KnowledgeProcessor(modelClient, toolEngine);
        logger.info("KN5 Knowledge Processing Agent initialized with AI processor: {}", agentId);
    }
    
    public KnowledgeProcessingAgent(String agentId, String name) {
        super(agentId, name, AgentType.KN5);
        this.processor = new KnowledgeProcessor(
            new OpenAIModelClient(System.getenv("AI_API_KEY")), 
            new ToolEngine()
        );
        logger.info("KN5 Knowledge Processing Agent initialized: {}", agentId);
    }
    
    @Override
    protected String processSpecializedTask(String task) {
        logger.info("KN5 AI-Enhanced Agent processing: {}", task);
        return processor.processKnowledge(task);
    }
    
    @Override
    public boolean supportsTaskType(String taskType) {
        String lower = taskType.toLowerCase();
        return lower.contains("知识") || lower.contains("推理") || lower.contains("学习") || 
               lower.contains("分析") || lower.contains("决策") || lower.contains("评估") || 
               lower.contains("优化") || lower.contains("规划");
    }
}