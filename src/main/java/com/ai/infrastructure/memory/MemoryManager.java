package com.ai.infrastructure.memory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存管理器，实现三层记忆架构
 * 基于Claude Code的AU2函数实现完整的8段式上下文压缩机制
 */
public class MemoryManager {
    // 短期记忆：当前会话上下文
    private final List<MemoryItem> shortTermMemory;
    
    // 中期记忆：压缩后的上下文
    private final List<CompressedMemory> mediumTermMemory;
    
    // 长期记忆：持久化存储
    private final Map<String, String> longTermMemory;
    
    // 内存使用阈值 - 与Claude Code中的h11常量一致
    private static final double COMPACTION_THRESHOLD = 0.92;
    private static final int MAX_SHORT_TERM_ITEMS = 100;
    
    // Token限制 - 与Claude Code中的CU2常量一致
    private static final int MAX_TOKEN_LIMIT = 16384;
    
    // 当前Token使用量
    private int currentTokenUsage;
    
    // 文件恢复相关
    private final Map<String, String> fileCache;
    private static final int MAX_FILE_RESTORE_TOKENS = 50000;
    private static final int MAX_SINGLE_FILE_TOKENS = 10000;
    
    public MemoryManager() {
        this.shortTermMemory = new ArrayList<>();
        this.mediumTermMemory = new ArrayList<>();
        this.longTermMemory = new ConcurrentHashMap<>();
        this.fileCache = new ConcurrentHashMap<>();
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
     * 压缩内存 - 基于Claude Code的AU2函数实现
     */
    private void compactMemory() {
        if (shortTermMemory.size() > 5) {
            // 使用8段式结构化压缩算法
            CompressedMemory compressed = perform8SegmentCompression();
            mediumTermMemory.add(compressed);
            
            // 记录压缩事件
            System.out.println("tengu_compact: Compacted " + shortTermMemory.size() + " messages");
            
            // 清理短期记忆中的旧数据，保留最近的几条消息
            int keepCount = Math.min(3, shortTermMemory.size());
            int itemsToRemove = shortTermMemory.size() - keepCount;
            
            for (int i = 0; i < itemsToRemove; i++) {
                MemoryItem item = shortTermMemory.remove(0);
                currentTokenUsage -= estimateTokens(item.getInput()) + estimateTokens(item.getOutput());
            }
        }
    }
    
    /**
     * 8段式结构化压缩算法 - 基于Claude Code的AU2函数完整实现
     * @return 压缩后的内存对象
     */
    private CompressedMemory perform8SegmentCompression() {
        // 1. Primary Request and Intent: 捕获用户的核心请求和意图
        StringBuilder primaryRequestAndIntent = new StringBuilder();
        
        // 2. Key Technical Concepts: 列出所有重要的技术概念、技术和框架
        StringBuilder keyTechnicalConcepts = new StringBuilder();
        
        // 3. Files and Code Sections: 枚举检查、修改或创建的特定文件和代码段
        StringBuilder filesAndCodeSections = new StringBuilder();
        
        // 4. Errors and fixes: 列出遇到的所有错误以及修复方法
        StringBuilder errorsAndFixes = new StringBuilder();
        
        // 5. Problem Solving: 记录已解决的问题和任何正在进行的故障排除工作
        StringBuilder problemSolving = new StringBuilder();
        
        // 6. All user messages: 列出所有非工具结果的用户消息
        StringBuilder allUserMessages = new StringBuilder();
        
        // 7. Pending Tasks: 概述明确要求处理的任何待处理任务
        StringBuilder pendingTasks = new StringBuilder();
        
        // 8. Current Work: 详细描述压缩请求之前正在进行的确切工作
        StringBuilder currentWork = new StringBuilder();
        
        // 分析所有短期记忆项
        for (int i = 0; i < shortTermMemory.size(); i++) {
            MemoryItem item = shortTermMemory.get(i);
            String input = item.getInput() != null ? item.getInput().toLowerCase() : "";
            String output = item.getOutput() != null ? item.getOutput().toLowerCase() : "";
            
            // 分析用户消息
            if (!input.isEmpty()) {
                allUserMessages.append(input).append("; ");
                
                // 识别主要请求和意图
                if (input.contains("实现") || input.contains("创建") || input.contains("开发") || 
                    input.contains("implement") || input.contains("create") || input.contains("build")) {
                    primaryRequestAndIntent.append(input).append("; ");
                }
                
                // 识别技术概念
                if (input.contains("java") || input.contains("python") || input.contains("javascript") ||
                    input.contains("react") || input.contains("spring") || input.contains("database") ||
                    input.contains("api") || input.contains("框架") || input.contains("库")) {
                    keyTechnicalConcepts.append(input).append("; ");
                }
                
                // 识别文件和代码段
                if (input.contains("文件") || input.contains("代码") || input.contains("函数") ||
                    input.contains("class") || input.contains("method") || input.contains("file")) {
                    filesAndCodeSections.append(input).append("; ");
                }
                
                // 识别错误和修复
                if (input.contains("错误") || input.contains("error") || input.contains("exception") ||
                    input.contains("修复") || input.contains("fix") || input.contains("解决")) {
                    errorsAndFixes.append(input).append("; ");
                }
                
                // 识别待处理任务
                if (input.contains("待办") || input.contains("todo") || input.contains("任务") ||
                    input.contains("task") || input.contains("需要")) {
                    pendingTasks.append(input).append("; ");
                }
            }
            
            // 分析输出消息
            if (!output.isEmpty()) {
                // 识别问题解决
                if (output.contains("解决") || output.contains("完成") || output.contains("success") ||
                    output.contains("resolved") || output.contains("completed")) {
                    problemSolving.append(output).append("; ");
                }
                
                // 识别当前工作（最近的消息）
                if (i >= shortTermMemory.size() - 2) {
                    currentWork.append(output).append("; ");
                }
            }
        }
        
        // 创建压缩内存对象
        return new CompressedMemory(
            primaryRequestAndIntent.toString(),
            keyTechnicalConcepts.toString(),
            filesAndCodeSections.toString(),
            allUserMessages.toString(), // 使用所有用户消息作为用户意图
            problemSolving.toString(), // 使用问题解决作为执行结果
            errorsAndFixes.toString(),
            pendingTasks.toString(), // 使用待处理任务作为开放问题
            currentWork.toString(), // 使用当前工作作为未来计划
            System.currentTimeMillis()
        );
    }
    
    /**
     * 智能文件恢复机制 - 基于Claude Code的TW5函数实现
     * @param filenames 要恢复的文件名列表
     * @return 恢复的文件内容映射
     */
    public Map<String, String> restoreFiles(List<String> filenames) {
        Map<String, String> restoredFiles = new HashMap<>();
        int totalTokens = 0;
        
        // 按时间戳排序，优先恢复最近访问的文件
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(fileCache.entrySet());
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // 恢复文件，控制Token总数
        for (Map.Entry<String, String> entry : sortedEntries) {
            String filename = entry.getKey();
            String content = entry.getValue();
            
            // 检查是否在请求的文件列表中
            if (filenames.contains(filename)) {
                int fileTokens = estimateTokens(content);
                
                // 检查单个文件Token限制
                if (fileTokens <= MAX_SINGLE_FILE_TOKENS) {
                    // 检查总Token限制
                    if (totalTokens + fileTokens <= MAX_FILE_RESTORE_TOKENS) {
                        restoredFiles.put(filename, content);
                        totalTokens += fileTokens;
                    } else {
                        // 超过总Token限制，停止恢复
                        break;
                    }
                }
            }
        }
        
        System.out.println("tengu_post_compact_file_restore_success: Restored " + restoredFiles.size() + " files");
        return restoredFiles;
    }
    
    /**
     * 缓存文件内容以供后续恢复
     * @param filename 文件名
     * @param content 文件内容
     */
    public void cacheFile(String filename, String content) {
        if (filename != null && content != null) {
            fileCache.put(filename, content);
        }
    }
    
    /**
     * 估算Token数量 - 更准确的实现
     * @param text 文本内容
     * @return 估算的Token数量
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 更准确的Token估算：基于字符和单词的混合估算
        // 英文：每个单词约1个token，每个字符约0.25个token
        // 中文：每个字符约0.6个token
        int charCount = text.length();
        int wordCount = text.split("\\s+").length;
        
        // 简单的中英文判断
        boolean isChinese = text.matches(".*[\\u4e00-\\u9fa5]+.*");
        
        if (isChinese) {
            return (int) (charCount * 0.6);
        } else {
            // 英文估算：单词数 + 字符数 * 0.25
            return wordCount + (int) (charCount * 0.25);
        }
    }
    
    /**
     * 获取最大Token限制
     * @return 最大Token限制
     */
    private int getMaxTokenLimit() {
        return MAX_TOKEN_LIMIT; // 与Claude Code中的CU2常量一致
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
        fileCache.clear();
        currentTokenUsage = 0;
    }
}