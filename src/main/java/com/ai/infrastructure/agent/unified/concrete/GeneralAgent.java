package com.ai.infrastructure.agent.unified.concrete;

import com.ai.infrastructure.agent.AgentType;
import com.ai.infrastructure.agent.unified.BaseUnifiedAgent;
import com.ai.infrastructure.agent.unified.UnifiedAgentContext;
import com.ai.infrastructure.model.OpenAIModelClient;

public class GeneralAgent extends BaseUnifiedAgent {

    private final OpenAIModelClient modelClient;

    public GeneralAgent(String agentId, String name, UnifiedAgentContext context) {
        super(agentId, name, AgentType.GENERAL, context);
        this.modelClient = context.getModelClient();
    }

    @Override
    public String processTask(String task) {
        logger.debug("GeneralAgent processing task: {}", task);
        if (modelClient == null) {
            logger.warn("OpenAIModelClient is not available. GeneralAgent cannot process the task.");
            return "Error: Model client is not initialized.";
        }
        try {
            // 使用模型进行通用任务处理
            return modelClient.callModel("Process the following general task: " + task);
        } catch (Exception e) {
            logger.error("GeneralAgent failed to process task with model: {}", e.getMessage(), e);
            return "Error processing task: " + e.getMessage();
        }
    }
}
