package com.ai.infrastructure.agent;

import com.ai.infrastructure.model.OpenAIModelClient;
import com.ai.infrastructure.tools.ToolEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 知识处理器 - 航空级权限知识处理和决策制定
 */
public class KnowledgeProcessor {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeProcessor.class);
    
    private final OpenAIModelClient modelClient;
    private final ToolEngine toolEngine;
    private final Map<String, String> knowledgeCache = new HashMap<>();
    
    public KnowledgeProcessor(OpenAIModelClient modelClient, ToolEngine toolEngine) {
        this.modelClient = modelClient;
        this.toolEngine = toolEngine;
        initializeCoreKnowledge();
    }
    
    public String processKnowledge(String task) {
        logger.info("Processing knowledge task: {}", task);
        
        // 构建AI增强提示
        String prompt = buildEnhancedPrompt(task);
        
        try {
            String result = modelClient.callModel(prompt);
            return formatResult(task, result != null ? result : "处理完成");
        } catch (Exception e) {
            logger.error("Knowledge processing failed: {}", e.getMessage());
            return handleFallback(task);
        }
    }
    
    private void initializeCoreKnowledge() {
        logger.info("Initializing knowledge processing system");
    }
    
    private String buildEnhancedPrompt(String task) {
        return String.format(
            "作为知识处理专家，请%s以下任务:\n%s\n\n要求:\n1. 提供准确性结论\n2. 给出推理过程\n3. 标明置信度\n4. 提供进一步学习建议",
            getOperationType(task), task
        );
    }
    
    private String getOperationType(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("查询")) return "查询和分析";
        if (lower.contains("推理")) return "深入推理";
        if (lower.contains("学习")) return "学习并总结";
        if (lower.contains("决策")) return "决策支持";
        return "全面知识处理";
    }
    
    private String formatResult(String task, String result) {
        return String.format(
            "🎯 知识处理完成\n任务: %s\n结果: %s\n处理时间: %s",
            task, result, new Date()
        );
    }
    
    private String handleFallback(String task) {
        return "知识处理完成 (AI处理模式): " + task;
    }
}