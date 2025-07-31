package com.ai.infrastructure.agent.interaction.model;

import lombok.Data;

@Data
public class InteractionTask {
    private final InteractionTaskType taskType;
    private final String content;
    private final long timestamp;
    
    public InteractionTask(InteractionTaskType taskType, String content) {
        this(taskType, content, System.currentTimeMillis());
    }
    
    public InteractionTask(InteractionTaskType taskType, String content, long timestamp) {
        this.taskType = taskType;
        this.content = content;
        this.timestamp = timestamp;
    }
}