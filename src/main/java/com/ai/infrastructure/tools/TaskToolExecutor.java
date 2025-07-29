package com.ai.infrastructure.tools;

import com.ai.infrastructure.agent.AgentStatus;
import com.ai.infrastructure.agent.MainAgent;
import com.ai.infrastructure.agent.SubAgent;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.model.OpenAIModelClient;
import com.ai.infrastructure.security.SecurityManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Task工具执行器 - 基于Claude Code的Task工具实现分层多Agent架构
 * 实现完整的Agent创建、生命周期管理、并发执行协调和安全隔离机制
 */
public class TaskToolExecutor implements ToolExecutor {
    private static final Logger logger = LoggerFactory.getLogger(TaskToolExecutor.class);
    private final Gson gson;
    private final SecurityManager securityManager;
    private final MemoryManager memoryManager;
    
    // 并发执行限制
    private static final int MAX_CONCURRENT_AGENTS = 10;
    private static final int MAX_PARALLEL_TASKS = 3;
    
    public TaskToolExecutor() {
        this.gson = new Gson();
        this.securityManager = new SecurityManager();
        this.memoryManager = new MemoryManager();
    }
    
    @Override
    public String execute(String task) {
        logger.info("Executing Task tool with task: {}", task);
        
        try {
            // 解析任务参数
            TaskParameters params = parseTaskParameters(task);
            
            if (params.description == null || params.description.trim().isEmpty()) {
                return "Error: Task description is required";
            }
            
            if (params.prompt == null || params.prompt.trim().isEmpty()) {
                return "Error: Task prompt is required";
            }
            
            // 执行Task工具的核心逻辑
            return executeTaskTool(params);
            
        } catch (Exception e) {
            logger.error("Error executing Task tool: {}", e.getMessage(), e);
            return "Error executing Task tool: " + e.getMessage();
        }
    }
    
    /**
     * 解析Task参数
     * @param task 任务描述
     * @return Task参数对象
     */
    private TaskParameters parseTaskParameters(String task) {
        TaskParameters params = new TaskParameters();
        
        try {
            // 尝试解析为JSON格式
            JsonObject taskJson = gson.fromJson(task, JsonObject.class);
            
            params.description = taskJson.has("description") ? taskJson.get("description").getAsString() : null;
            params.prompt = taskJson.has("prompt") ? taskJson.get("prompt").getAsString() : null;
            
        } catch (JsonSyntaxException e) {
            // 如果不是JSON格式，使用简单解析
            params = parseSimpleTaskParameters(task);
        }
        
        return params;
    }
    
    /**
     * 解析简单格式的Task参数
     * @param task 任务描述
     * @return Task参数对象
     */
    private TaskParameters parseSimpleTaskParameters(String task) {
        TaskParameters params = new TaskParameters();
        
        // 移除前缀
        String taskContent = task.trim();
        if (taskContent.startsWith("task ")) {
            taskContent = taskContent.substring(5); // 移除"task "前缀
        } else if (taskContent.startsWith("Task ")) {
            taskContent = taskContent.substring(5); // 移除"Task "前缀
        }
        
        // 简单格式：task "description" "prompt"
        // 或者：task description prompt
        String[] parts = taskContent.split(" ", 2);
        if (parts.length >= 2) {
            params.description = removeQuotes(parts[0]);
            params.prompt = removeQuotes(parts[1]);
        } else {
            params.description = "Task";
            params.prompt = taskContent;
        }
        
        return params;
    }
    
    /**
     * 移除字符串两端的引号
     * @param str 字符串
     * @return 移除引号后的字符串
     */
    private String removeQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        } else if (str.startsWith("'") && str.endsWith("'")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
    
    /**
     * 执行Task工具的核心逻辑
     * @param params Task参数
     * @return 执行结果
     */
    private String executeTaskTool(TaskParameters params) {
        logger.debug("Executing Task tool with description: {}, prompt: {}", 
                    params.description, params.prompt);
        
        try {
            // 创建多个Agent任务（并行执行模式）
            List<CompletableFuture<String>> agentTasks = new ArrayList<>();
            
            int parallelTasksCount = Math.min(MAX_PARALLEL_TASKS, 
                Math.max(1, getParallelTasksCount()));
            
            // 创建多个相同的Agent任务
            for (int i = 0; i < parallelTasksCount; i++) {
                String enhancedPrompt = params.prompt + "\n\nProvide a thorough and complete analysis.";
                CompletableFuture<String> agentTask = launchSubAgent(enhancedPrompt, i);
                agentTasks.add(agentTask);
            }
            
            // 等待所有Agent任务完成
            List<String> agentResults = new ArrayList<>();
            for (CompletableFuture<String> agentTask : agentTasks) {
                try {
                    String result = agentTask.get();
                    agentResults.add(result);
                } catch (Exception e) {
                    logger.warn("Agent task failed: {}", e.getMessage());
                    agentResults.add("Error: Agent task failed - " + e.getMessage());
                }
            }
            
            // 合并结果
            String finalResult = synthesizeResults(params.prompt, agentResults);
            
            logger.info("Task tool executed successfully with {} parallel agents", parallelTasksCount);
            return finalResult;
            
        } catch (Exception e) {
            logger.error("Task tool execution failed: {}", e.getMessage(), e);
            return "Error executing Task tool: " + e.getMessage();
        }
    }
    
    /**
     * 启动SubAgent执行任务
     * @param prompt 任务提示
     * @param agentIndex Agent索引
     * @return 执行结果的CompletableFuture
     */
    private CompletableFuture<String> launchSubAgent(String prompt, int agentIndex) {
        logger.debug("Launching SubAgent {} with prompt: {}", agentIndex, prompt);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 创建SubAgent实例
                SubAgent subAgent = new SubAgent("sub-" + System.currentTimeMillis() + "-" + agentIndex, 
                                               "SubAgent-" + agentIndex + " for: " + prompt);
                
                // 执行任务
                CompletableFuture<String> resultFuture = subAgent.executeTask(prompt);
                return resultFuture.get(); // 等待结果
                
            } catch (Exception e) {
                logger.error("SubAgent {} execution failed: {}", agentIndex, e.getMessage(), e);
                return "Error: SubAgent " + agentIndex + " execution failed - " + e.getMessage();
            }
        });
    }
    
    /**
     * 合并多个Agent的结果
     * @param originalPrompt 原始任务提示
     * @param agentResults Agent执行结果列表
     * @return 合成后的结果
     */
    private String synthesizeResults(String originalPrompt, List<String> agentResults) {
        logger.debug("Synthesizing results from {} agents", agentResults.size());
        
        StringBuilder synthesisPrompt = new StringBuilder();
        synthesisPrompt.append("Original task: ").append(originalPrompt).append("\n\n");
        synthesisPrompt.append("I've assigned multiple agents to tackle this task. Each agent has analyzed the problem and provided their findings.\n\n");
        
        // 添加每个Agent的响应
        for (int i = 0; i < agentResults.size(); i++) {
            synthesisPrompt.append("== AGENT ").append(i + 1).append(" RESPONSE ==\n");
            synthesisPrompt.append(agentResults.get(i)).append("\n\n");
        }
        
        synthesisPrompt.append("Based on all the information provided by these agents, synthesize a comprehensive and cohesive response that:\n");
        synthesisPrompt.append("1. Combines the key insights from all agents\n");
        synthesisPrompt.append("2. Resolves any contradictions between agent findings\n");
        synthesisPrompt.append("3. Presents a unified solution that addresses the original task\n");
        synthesisPrompt.append("4. Includes all important details and code examples from the individual responses\n");
        synthesisPrompt.append("5. Is well-structured and complete\n\n");
        synthesisPrompt.append("Your synthesis should be thorough but focused on the original task.");
        
        // 使用SubAgent来执行合成任务
        try {
            SubAgent synthesisAgent = new SubAgent("synthesis-" + System.currentTimeMillis(), 
                                                 "Synthesis Agent for task results");
            CompletableFuture<String> resultFuture = synthesisAgent.executeTask(synthesisPrompt.toString());
            return resultFuture.get(); // 等待合成结果
            
        } catch (Exception e) {
            logger.error("Synthesis agent execution failed: {}", e.getMessage(), e);
            
            // 如果合成Agent失败，返回简单的合并结果
            StringBuilder simpleResult = new StringBuilder();
            simpleResult.append("Synthesized results from ").append(agentResults.size()).append(" agents:\n\n");
            
            for (int i = 0; i < agentResults.size(); i++) {
                simpleResult.append("Agent ").append(i + 1).append(" response:\n");
                simpleResult.append(agentResults.get(i)).append("\n\n");
            }
            
            return simpleResult.toString();
        }
    }
    
    /**
     * 获取并行任务数量配置
     * @return 并行任务数量
     */
    private int getParallelTasksCount() {
        // 可以从配置文件或环境变量中读取
        // 这里使用默认值
        return 3;
    }
    
    /**
     * Task参数类
     */
    private static class TaskParameters {
        String description;
        String prompt;
    }
}