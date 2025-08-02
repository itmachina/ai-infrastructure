package com.ai.infrastructure.agent.unified.framework;

import com.ai.infrastructure.agent.unified.BaseUnifiedAgent;
import com.ai.infrastructure.agent.unified.UnifiedAgentContext;
import com.ai.infrastructure.memory.MemoryManager;
import com.ai.infrastructure.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 统一的任务执行框架
 */
public class TaskExecutionFramework {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionFramework.class);

    private final BaseUnifiedAgent agent;
    private final ExecutorService executorService;
    private final AtomicBoolean isAborted = new AtomicBoolean(false);
    private final Map<String, Object> executionConfig = new ConcurrentHashMap<>();

    public TaskExecutionFramework(BaseUnifiedAgent agent, UnifiedAgentContext context) {
        this.agent = agent;
        this.executorService = Executors.newFixedThreadPool(5);
        initializeExecutionConfig();
    }

    private void initializeExecutionConfig() {
        executionConfig.put("defaultTimeout", 30000L);
        executionConfig.put("maxRetries", 3);
    }

    public CompletableFuture<String> execute(String task) {
        return CompletableFuture.supplyAsync(() -> executeTaskWithFramework(task), executorService);
    }

    private String executeTaskWithFramework(String task) {
        long startTime = System.currentTimeMillis();
        try {
            String processedTask = preprocessTask(task);
            validateSecurity(processedTask);
            checkMemoryPressure();
            String result = executeTaskWithRetry(processedTask);
            return postprocessResult(result);
        } catch (Exception e) {
            logger.error("Task execution failed for agent {}: {}", agent.getAgentId(), e.getMessage(), e);
            return handleFallback(task, e);
        }
    }

    private String preprocessTask(String task) {
        return task.trim();
    }

    private void validateSecurity(String task) {
        SecurityManager securityManager = agent.getSecurityManager();
        if (securityManager != null && !securityManager.validateInput(task)) {
            throw new SecurityException("Security validation failed for task: " + task);
        }
    }

    private void checkMemoryPressure() {
        MemoryManager memoryManager = agent.getMemoryManager();
        if (memoryManager != null) {
            memoryManager.checkMemoryPressure();
        }
    }

    private String executeTaskWithRetry(String task) {
        int maxRetries = (int) executionConfig.get("maxRetries");
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (isAborted.get()) throw new RuntimeException("Execution was aborted.");
                // Delegate to the agent's specific implementation
                return agent.processTask(task);
            } catch (Exception e) {
                logger.warn("Task execution attempt {} failed for agent {}: {}", attempt, agent.getAgentId(), e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("Task failed after " + maxRetries + " attempts.", e);
                }
                try {
                    Thread.sleep(1000 * (long) Math.pow(2, attempt - 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry delay was interrupted.", ie);
                }
            }
        }
        throw new RuntimeException("Task execution failed unexpectedly.");
    }

    private String postprocessResult(String result) {
        String postprocessedResult = String.format("[%s] %s", agent.getAgentId(), result);
        MemoryManager memoryManager = agent.getMemoryManager();
        if (memoryManager != null) {
            memoryManager.updateContext("LAST_EXECUTION_RESULT", postprocessedResult);
        }
        return postprocessedResult;
    }

    private String handleFallback(String task, Exception e) {
        logger.warn("Attempting fallback for task '{}' due to error: {}", task, e.getMessage());
        return String.format("[Fallback] Task '%s' failed. Error: %s", task, e.getMessage());
    }

    public void abort() {
        if (isAborted.compareAndSet(false, true)) {
            logger.info("Aborting all tasks for agent {}", agent.getAgentId());
            executorService.shutdownNow();
        }
    }
}
