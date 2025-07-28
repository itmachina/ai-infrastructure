package com.ai.infrastructure.memory;

/**
 * 压缩内存类，用于存储压缩后的上下文信息
 */
public class CompressedMemory {
    private String backgroundContext;
    private String keyDecisions;
    private String toolUsage;
    private String userIntent;
    private String executionResults;
    private String errorsAndSolutions;
    private String openIssues;
    private String futurePlans;
    private long timestamp;
    
    public CompressedMemory(String backgroundContext, String keyDecisions, String toolUsage,
                           String userIntent, String executionResults, String errorsAndSolutions,
                           String openIssues, String futurePlans, long timestamp) {
        this.backgroundContext = backgroundContext;
        this.keyDecisions = keyDecisions;
        this.toolUsage = toolUsage;
        this.userIntent = userIntent;
        this.executionResults = executionResults;
        this.errorsAndSolutions = errorsAndSolutions;
        this.openIssues = openIssues;
        this.futurePlans = futurePlans;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getBackgroundContext() {
        return backgroundContext;
    }
    
    public String getKeyDecisions() {
        return keyDecisions;
    }
    
    public String getToolUsage() {
        return toolUsage;
    }
    
    public String getUserIntent() {
        return userIntent;
    }
    
    public String getExecutionResults() {
        return executionResults;
    }
    
    public String getErrorsAndSolutions() {
        return errorsAndSolutions;
    }
    
    public String getOpenIssues() {
        return openIssues;
    }
    
    public String getFuturePlans() {
        return futurePlans;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}