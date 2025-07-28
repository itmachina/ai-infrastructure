package com.ai.infrastructure.memory;

/**
 * 内存项类，表示一个记忆单元
 */
public class MemoryItem {
    private String input;
    private String output;
    private long timestamp;
    
    public MemoryItem(String input, String output, long timestamp) {
        this.input = input;
        this.output = output;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getInput() {
        return input;
    }
    
    public String getOutput() {
        return output;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}