package com.ai.infrastructure.memory;

import com.ai.infrastructure.agent.AgentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 上下文压缩管理器
 * 基于Claude Code的wU2函数实现，支持智能上下文压缩
 */
public class ContextCompressor {
    private static final Logger logger = LoggerFactory.getLogger(ContextCompressor.class);
    
    // 默认压缩阈值
    private static final int DEFAULT_COMPRESSION_THRESHOLD = 40000;
    
    // 压缩统计信息
    private final AtomicInteger compressionCount = new AtomicInteger(0);
    private final AtomicInteger totalCompressedTokens = new AtomicInteger(0);
    private final AtomicInteger totalOriginalTokens = new AtomicInteger(0);
    
    /**
     * 检查并执行上下文压缩
     * @param messages 消息列表
     * @param context Agent执行上下文
     * @return 压缩结果
     */
    public CompressionResult performContextCompression(List<Map<String, String>> messages, Object context) {
        int totalTokens = calculateTokenCount(messages);
        int threshold = getCompressionThreshold();
        
        logger.debug("Checking context compression: totalTokens={}, threshold={}", totalTokens, threshold);
        
        if (totalTokens < threshold) {
            return new CompressionResult(messages, false, 0, 0, 0.0);
        }
        
        try {
            // 执行上下文压缩
            List<Map<String, String>> compressedMessages = compressMessages(messages);
            
            // 更新统计信息
            int compressedTokens = calculateTokenCount(compressedMessages);
            double compressionRatio = (double) compressedTokens / totalTokens;
            
            compressionCount.incrementAndGet();
            totalCompressedTokens.addAndGet(compressedTokens);
            totalOriginalTokens.addAndGet(totalTokens);
            
            logger.info("Context compression completed: original={} tokens, compressed={} tokens, ratio={}", 
                       totalTokens, compressedTokens, String.format("%.2f", compressionRatio));
            
            return new CompressionResult(compressedMessages, true, totalTokens, compressedTokens, compressionRatio);
        } catch (Exception e) {
            logger.error("Error during context compression: {}", e.getMessage(), e);
            // 压缩失败时返回原始消息
            return new CompressionResult(messages, false, totalTokens, totalTokens, 1.0);
        }
    }
    
    /**
     * 压缩消息列表
     * @param messages 原始消息列表
     * @return 压缩后的消息列表
     */
    private List<Map<String, String>> compressMessages(List<Map<String, String>> messages) {
        List<Map<String, String>> compressedMessages = new ArrayList<>();
        
        // 保留系统消息和最新的几条消息
        Map<String, String> systemMessage = null;
        List<Map<String, String>> recentMessages = new ArrayList<>();
        List<Map<String, String>> olderMessages = new ArrayList<>();
        
        // 分类消息
        for (Map<String, String> message : messages) {
            String role = message.get("role");
            if ("system".equals(role)) {
                systemMessage = message;
            } else if (recentMessages.size() < 5) {
                // 保留最新的5条消息
                recentMessages.add(0, message); // 添加到开头以保持顺序
            } else {
                olderMessages.add(message);
            }
        }
        
        // 重新排序recentMessages以保持正确的时间顺序
        List<Map<String, String>> orderedRecentMessages = new ArrayList<>();
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            orderedRecentMessages.add(recentMessages.get(i));
        }
        
        // 添加系统消息（如果存在）
        if (systemMessage != null) {
            compressedMessages.add(systemMessage);
        }
        
        // 添加压缩后的旧消息摘要
        if (!olderMessages.isEmpty()) {
            Map<String, String> summaryMessage = createSummaryMessage(olderMessages);
            compressedMessages.add(summaryMessage);
        }
        
        // 添加最近的消息
        compressedMessages.addAll(orderedRecentMessages);
        
        return compressedMessages;
    }
    
    /**
     * 创建旧消息的摘要
     * @param messages 消息列表
     * @return 摘要消息
     */
    private Map<String, String> createSummaryMessage(List<Map<String, String>> messages) {
        StringBuilder summary = new StringBuilder();
        summary.append("Previous conversation history has been compressed. ");
        summary.append("Number of compressed messages: ").append(messages.size()).append(". ");
        summary.append("Key points from the history: ");
        
        // 提取关键信息
        for (Map<String, String> message : messages) {
            String content = message.get("content");
            if (content != null && content.length() > 0) {
                // 提取每条消息的前100个字符作为摘要
                String excerpt = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                summary.append(excerpt).append(" ");
            }
        }
        
        Map<String, String> summaryMessage = new HashMap<>();
        summaryMessage.put("role", "assistant");
        summaryMessage.put("content", summary.toString().trim());
        
        return summaryMessage;
    }
    
    /**
     * 计算消息列表的token数量
     * @param messages 消息列表
     * @return token数量
     */
    private int calculateTokenCount(List<Map<String, String>> messages) {
        int tokenCount = 0;
        for (Map<String, String> message : messages) {
            String content = message.get("content");
            if (content != null) {
                // 简单的token估算：每个单词约1个token，每个标点符号约0.3个token
                String[] words = content.split("\\s+");
                tokenCount += words.length;
                tokenCount += (int) (content.length() * 0.3);
            }
        }
        return tokenCount;
    }
    
    /**
     * 获取压缩阈值
     * @return 压缩阈值
     */
    private int getCompressionThreshold() {
        // 可以从配置中读取阈值
        return DEFAULT_COMPRESSION_THRESHOLD;
    }
    
    /**
     * 获取压缩统计信息
     * @return 统计信息字符串
     */
    public String getCompressionStats() {
        int count = compressionCount.get();
        int original = totalOriginalTokens.get();
        int compressed = totalCompressedTokens.get();
        double averageRatio = count > 0 ? (double) compressed / original : 0.0;
        
        return String.format("Compression Stats - Count: %d, Original Tokens: %d, Compressed Tokens: %d, Average Ratio: %.2f", 
                           count, original, compressed, averageRatio);
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        compressionCount.set(0);
        totalCompressedTokens.set(0);
        totalOriginalTokens.set(0);
    }
    
    /**
     * 压缩结果类
     */
    public static class CompressionResult {
        private final List<Map<String, String>> messages;
        private final boolean wasCompacted;
        private final int originalTokens;
        private final int compressedTokens;
        private final double compressionRatio;
        
        public CompressionResult(List<Map<String, String>> messages, boolean wasCompacted, 
                               int originalTokens, int compressedTokens, double compressionRatio) {
            this.messages = messages;
            this.wasCompacted = wasCompacted;
            this.originalTokens = originalTokens;
            this.compressedTokens = compressedTokens;
            this.compressionRatio = compressionRatio;
        }
        
        // Getters
        public List<Map<String, String>> getMessages() {
            return messages;
        }
        
        public boolean wasCompacted() {
            return wasCompacted;
        }
        
        public int getOriginalTokens() {
            return originalTokens;
        }
        
        public int getCompressedTokens() {
            return compressedTokens;
        }
        
        public double getCompressionRatio() {
            return compressionRatio;
        }
    }
}