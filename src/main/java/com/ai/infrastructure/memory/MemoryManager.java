package com.ai.infrastructure.memory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存管理器，实现三层记忆架构
 */
public class MemoryManager {
    // 短期记忆：当前会话上下文
    private final List<MemoryItem> shortTermMemory;
    
    // 中期记忆：压缩后的上下文
    private final List<CompressedMemory> mediumTermMemory;
    
    // 长期记忆：持久化存储
    private final Map<String, String> longTermMemory;
    
    // 内存使用阈值
    private static final double COMPACTION_THRESHOLD = 0.92;
    private static final int MAX_SHORT_TERM_ITEMS = 100;
    
    // 当前Token使用量
    private int currentTokenUsage;
    
    public MemoryManager() {
        this.shortTermMemory = new ArrayList<>();
        this.mediumTermMemory = new ArrayList<>();
        this.longTermMemory = new ConcurrentHashMap<>();
        this.currentTokenUsage = 0;
    }
    
    /**
     * 更新上下文
     * @param input 输入内容
     * @param output 输出内容
     */
    public void updateContext(String input, String output) {
        MemoryItem item = new MemoryItem(input, output, System.currentTimeMillis());
        shortTermMemory.add(item);
        currentTokenUsage += estimateTokens(input) + estimateTokens(output);
        
        // 检查是否需要压缩
        checkMemoryPressure();
    }
    
    /**
     * 检查内存压力
     */
    public void checkMemoryPressure() {
        if (shouldCompact()) {
            compactMemory();
        }
    }
    
    /**
     * 判断是否需要压缩
     * @return 是否需要压缩
     */
    private boolean shouldCompact() {
        return (double) currentTokenUsage / getMaxTokenLimit() > COMPACTION_THRESHOLD;
    }
    
    /**
     * 压缩内存
     */
    private void compactMemory() {
        if (shortTermMemory.size() > 10) {
            // 使用8段式结构化压缩算法
            CompressedMemory compressed = perform8SegmentCompression();
            mediumTermMemory.add(compressed);
            
            // 清理短期记忆中的旧数据
            int itemsToRemove = shortTermMemory.size() / 2;
            for (int i = 0; i < itemsToRemove; i++) {
                MemoryItem item = shortTermMemory.remove(0);
                currentTokenUsage -= estimateTokens(item.getInput()) + estimateTokens(item.getOutput());
            }
        }
    }
    
    /**
     * 8段式结构化压缩算法
     * @return 压缩后的内存对象
     */
    private CompressedMemory perform8SegmentCompression() {
        StringBuilder backgroundContext = new StringBuilder();
        StringBuilder keyDecisions = new StringBuilder();
        StringBuilder toolUsage = new StringBuilder();
        StringBuilder userIntent = new StringBuilder();
        StringBuilder executionResults = new StringBuilder();
        StringBuilder errorsAndSolutions = new StringBuilder();
        StringBuilder openIssues = new StringBuilder();
        StringBuilder futurePlans = new StringBuilder();
        
        int itemCount = Math.min(10, shortTermMemory.size());
        for (int i = 0; i < itemCount; i++) {
            MemoryItem item = shortTermMemory.get(i);
            
            // 简单的分类逻辑（实际实现中会更复杂）
            if (item.getInput().contains("背景") || item.getInput().contains("context")) {
                backgroundContext.append(item.getInput()).append("; ");
            } else if (item.getInput().contains("决策") || item.getInput().contains("decision")) {
                keyDecisions.append(item.getInput()).append("; ");
            } else if (item.getInput().contains("工具") || item.getInput().contains("tool")) {
                toolUsage.append(item.getInput()).append("; ");
            } else if (item.getOutput().contains("完成") || item.getOutput().contains("success")) {
                executionResults.append(item.getOutput()).append("; ");
            } else if (item.getOutput().contains("错误") || item.getOutput().contains("error")) {
                errorsAndSolutions.append(item.getOutput()).append("; ");
            } else {
                userIntent.append(item.getInput()).append("; ");
                openIssues.append(item.getOutput()).append("; ");
            }
        }
        
        return new CompressedMemory(
            backgroundContext.toString(),
            keyDecisions.toString(),
            toolUsage.toString(),
            userIntent.toString(),
            executionResults.toString(),
            errorsAndSolutions.toString(),
            openIssues.toString(),
            futurePlans.toString(),
            System.currentTimeMillis()
        );
    }
    
    /**
     * 估算Token数量（简化实现）
     * @param text 文本内容
     * @return 估算的Token数量
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简化的Token估算：每个字符约0.3个token，每个词约1.3个token
        return (int) (text.length() * 0.4);
    }
    
    /**
     * 获取最大Token限制
     * @return 最大Token限制
     */
    private int getMaxTokenLimit() {
        return 16384; // 与Claude Code中的CU2常量一致
    }
    
    /**
     * 获取当前Token使用量
     * @return 当前Token使用量
     */
    public int getCurrentTokenUsage() {
        return currentTokenUsage;
    }
    
    /**
     * 获取短期记忆
     * @return 短期记忆列表
     */
    public List<MemoryItem> getShortTermMemory() {
        return new ArrayList<>(shortTermMemory);
    }
    
    /**
     * 获取中期记忆
     * @return 中期记忆列表
     */
    public List<CompressedMemory> getMediumTermMemory() {
        return new ArrayList<>(mediumTermMemory);
    }
    
    /**
     * 更新长期记忆
     * @param key 键
     * @param value 值
     */
    public void updateLongTermMemory(String key, String value) {
        longTermMemory.put(key, value);
    }
    
    /**
     * 从长期记忆中获取值
     * @param key 键
     * @return 值
     */
    public String getFromLongTermMemory(String key) {
        return longTermMemory.get(key);
    }
    
    /**
     * 清理内存
     */
    public void clear() {
        shortTermMemory.clear();
        mediumTermMemory.clear();
        longTermMemory.clear();
        currentTokenUsage = 0;
    }
}