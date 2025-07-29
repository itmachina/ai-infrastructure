package com.ai.infrastructure.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存管理器，实现三层记忆架构
 * 基于Claude Code的AU2函数实现完整的8段式上下文压缩机制
 */
public class MemoryManager {
    private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);
    
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
     * 基于Claude Code的智能压缩策略优化
     * 结合多种因素综合判断是否需要压缩
     * @return 是否需要压缩
     */
    private boolean shouldCompact() {
        double tokenRatio = (double) currentTokenUsage / getMaxTokenLimit();
        
        // 基本阈值检查
        if (tokenRatio > COMPACTION_THRESHOLD) {
            logger.info("Memory compaction triggered: Token ratio {} exceeds threshold {}", tokenRatio, COMPACTION_THRESHOLD);
            return true;
        }
        
        // 如果短期记忆项数过多，也需要压缩
        if (shortTermMemory.size() > MAX_SHORT_TERM_ITEMS) {
            logger.info("Memory compaction triggered: Short term memory size {} exceeds limit {}", shortTermMemory.size(), MAX_SHORT_TERM_ITEMS);
            return true;
        }
        
        // 如果有大量重复内容，也需要压缩
        if (hasHighRedundancy()) {
            logger.info("Memory compaction triggered: High redundancy detected in short term memory");
            return true;
        }
        
        // 检查是否有长时间未压缩的内容
        if (hasStaleContent()) {
            logger.info("Memory compaction triggered: Stale content detected, triggering compaction");
            return true;
        }
        
        // 检查是否需要基于时间的压缩（例如，超过5分钟未压缩）
        if (needsTimeBasedCompaction()) {
            logger.info("Memory compaction triggered: Time-based compaction triggered");
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否有长时间未压缩的内容
     * @return 是否有长时间未压缩的内容
     */
    private boolean hasStaleContent() {
        if (shortTermMemory.isEmpty()) {
            return false;
        }
        
        // 检查最旧的消息是否超过一定时间未压缩
        MemoryItem oldestItem = shortTermMemory.get(0);
        long currentTime = System.currentTimeMillis();
        long timeSinceOldest = currentTime - oldestItem.getTimestamp();
        
        // 如果最旧的消息超过10分钟，则认为是长时间未压缩的内容
        return timeSinceOldest > 10 * 60 * 1000;
    }
    
    /**
     * 检查是否需要基于时间的压缩
     * @return 是否需要基于时间的压缩
     */
    private boolean needsTimeBasedCompaction() {
        if (shortTermMemory.isEmpty()) {
            return false;
        }
        
        // 检查最近一次压缩的时间
        if (!mediumTermMemory.isEmpty()) {
            CompressedMemory latestCompression = mediumTermMemory.get(mediumTermMemory.size() - 1);
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCompression = currentTime - latestCompression.getTimestamp();
            
            // 如果距离上次压缩超过5分钟，且当前有足够多的消息，则触发压缩
            return timeSinceLastCompression > 5 * 60 * 1000 && shortTermMemory.size() > 5;
        }
        
        // 如果从未压缩过，且有足够多的消息，则触发压缩
        return shortTermMemory.size() > 8;
    }
    
    /**
     * 检查是否有高冗余内容
     * @return 是否有高冗余
     */
    private boolean hasHighRedundancy() {
        if (shortTermMemory.size() < 5) {
            return false;
        }
        
        // 检查最近几条消息的相似性
        int similarCount = 0;
        int totalCount = Math.min(5, shortTermMemory.size());
        
        for (int i = shortTermMemory.size() - totalCount; i < shortTermMemory.size() - 1; i++) {
            MemoryItem current = shortTermMemory.get(i);
            MemoryItem next = shortTermMemory.get(i + 1);
            
            if (isSimilar(current, next)) {
                similarCount++;
            }
        }
        
        // 如果超过60%的消息相似，则认为有高冗余
        return (double) similarCount / (totalCount - 1) > 0.6;
    }
    
    /**
     * 检查两个记忆项是否相似
     * @param item1 记忆项1
     * @param item2 记忆项2
     * @return 是否相似
     */
    private boolean isSimilar(MemoryItem item1, MemoryItem item2) {
        String input1 = item1.getInput() != null ? item1.getInput() : "";
        String input2 = item2.getInput() != null ? item2.getInput() : "";
        String output1 = item1.getOutput() != null ? item1.getOutput() : "";
        String output2 = item2.getOutput() != null ? item2.getOutput() : "";
        
        // 计算相似度（简单实现）
        double inputSimilarity = calculateSimilarity(input1, input2);
        double outputSimilarity = calculateSimilarity(output1, output2);
        
        // 如果输入或输出的相似度超过80%，则认为相似
        return inputSimilarity > 0.8 || outputSimilarity > 0.8;
    }
    
    /**
     * 计算两个字符串的相似度（使用编辑距离算法）
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 相似度（0-1之间）
     */
    private double calculateSimilarity(String str1, String str2) {
        if (str1.isEmpty() && str2.isEmpty()) {
            return 1.0;
        }
        
        if (str1.isEmpty() || str2.isEmpty()) {
            return 0.0;
        }
        
        // 使用编辑距离算法计算相似度
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int editDistance = calculateEditDistance(str1, str2);
        return 1.0 - (double) editDistance / maxLength;
    }
    
    /**
     * 计算两个字符串之间的编辑距离（Levenshtein距离）
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 编辑距离
     */
    private int calculateEditDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        
        // 创建动态规划表
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        // 初始化边界条件
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        // 填充动态规划表
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // 字符相同，不需要操作
                } else {
                    // 取三种操作的最小值：插入、删除、替换
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * 压缩内存 - 基于Claude Code的AU2函数实现
     * 优化版本，提高压缩效率
     */
    private void compactMemory() {
        int size = shortTermMemory.size();
        if (size > 5) {
            // 记录压缩开始
            long startTime = System.currentTimeMillis();
            logger.info("Starting memory compaction with {} messages", size);
            
            // 计算压缩前的Token总数
            int tokensBeforeCompaction = currentTokenUsage;
            
            // 使用8段式结构化压缩算法
            CompressedMemory compressed = perform8SegmentCompression();
            mediumTermMemory.add(compressed);
            
            // 计算压缩后的Token使用量
            int compressedTokens = estimateCompressedTokens(compressed);
            
            // 记录压缩事件
            long endTime = System.currentTimeMillis();
            logger.info("Compacted {} messages in {}ms", size, (endTime - startTime));
            logger.info("Memory compaction stats: Before={} tokens, After={} tokens", tokensBeforeCompaction, compressedTokens);
            
            // 智能清理短期记忆中的旧数据
            int keepCount = determineKeepCount();
            int itemsToRemove = size - keepCount;
            
            if (itemsToRemove > 0) {
                // 批量更新Token使用量
                int tokensToRemove = 0;
                for (int i = 0; i < itemsToRemove; i++) {
                    MemoryItem item = shortTermMemory.get(i);
                    tokensToRemove += estimateTokens(item.getInput()) + estimateTokens(item.getOutput());
                }
                
                // 批量移除旧数据
                shortTermMemory.subList(0, itemsToRemove).clear();
                currentTokenUsage -= tokensToRemove;
                
                // 更新Token使用量为压缩后的值
                currentTokenUsage = compressedTokens + (currentTokenUsage - compressedTokens);
                
                logger.info("Removed {} old messages, freed {} tokens", itemsToRemove, tokensToRemove);
                
                // 计算压缩比率
                if (tokensBeforeCompaction > 0) {
                    double compressionRatio = (double) (tokensBeforeCompaction - compressedTokens) / tokensBeforeCompaction;
                    logger.info("Compression ratio: {:.2f}%", compressionRatio * 100);
                }
            }
        }
    }
    
    /**
     * 估算压缩内存的Token数量
     * @param compressed 压缩内存对象
     * @return 估算的Token数量
     */
    private int estimateCompressedTokens(CompressedMemory compressed) {
        int totalTokens = 0;
        
        // 估算每个段落的Token数量
        totalTokens += estimateTokens(compressed.getBackgroundContext());
        totalTokens += estimateTokens(compressed.getKeyDecisions());
        totalTokens += estimateTokens(compressed.getToolUsage());
        totalTokens += estimateTokens(compressed.getUserIntent());
        totalTokens += estimateTokens(compressed.getExecutionResults());
        totalTokens += estimateTokens(compressed.getErrorsAndSolutions());
        totalTokens += estimateTokens(compressed.getOpenIssues());
        totalTokens += estimateTokens(compressed.getFuturePlans());
        
        return totalTokens;
    }
    
    /**
     * 确定需要保留的消息数量
     * 基于Claude Code的智能保留策略优化
     * @return 需要保留的消息数量
     */
    private int determineKeepCount() {
        // 基于当前上下文的重要性决定保留多少消息
        int baseKeepCount = 2;
        
        // 检查是否有重要的上下文需要保留
        int importantContextCount = countImportantContext();
        baseKeepCount += importantContextCount;
        
        // 如果有未完成的任务，保留更多上下文
        if (hasPendingTasks()) {
            baseKeepCount += 2;
        }
        
        // 如果有错误需要处理，保留更多上下文
        if (hasRecentErrors()) {
            baseKeepCount += 2;
        }
        
        // 如果有正在进行的工作，保留更多上下文
        if (hasOngoingWork()) {
            baseKeepCount += 1;
        }
        
        // 限制最大保留数量（不超过短期记忆的30%）
        int maxKeepCount = Math.max(5, shortTermMemory.size() / 3);
        return Math.min(baseKeepCount, Math.min(maxKeepCount, shortTermMemory.size()));
    }
    
    /**
     * 计算重要上下文的数量
     * @return 重要上下文数量
     */
    private int countImportantContext() {
        int count = 0;
        int checkCount = Math.min(8, shortTermMemory.size());
        int startIndex = shortTermMemory.size() - checkCount;
        
        for (int i = startIndex; i < shortTermMemory.size(); i++) {
            MemoryItem item = shortTermMemory.get(i);
            String input = item.getInput() != null ? item.getInput().toLowerCase() : "";
            String output = item.getOutput() != null ? item.getOutput().toLowerCase() : "";
            
            // 检查是否包含重要上下文关键词
            if (input.contains("重要") || input.contains("关键") || input.contains("核心") ||
                output.contains("重要") || output.contains("关键") || output.contains("核心")) {
                count++;
            }
            
            // 检查是否包含决策相关关键词
            if (input.contains("决定") || input.contains("决策") || input.contains("选择") ||
                output.contains("决定") || output.contains("决策") || output.contains("选择")) {
                count++;
            }
        }
        
        return Math.min(count, 3); // 最多计算3个重要上下文
    }
    
    /**
     * 检查是否有正在进行的工作
     * @return 是否有正在进行的工作
     */
    private boolean hasOngoingWork() {
        // 检查最近的消息中是否包含正在进行的工作的关键词
        int checkCount = Math.min(4, shortTermMemory.size());
        int startIndex = shortTermMemory.size() - checkCount;
        
        for (int i = startIndex; i < shortTermMemory.size(); i++) {
            MemoryItem item = shortTermMemory.get(i);
            String input = item.getInput() != null ? item.getInput().toLowerCase() : "";
            String output = item.getOutput() != null ? item.getOutput().toLowerCase() : "";
            
            if (input.contains("正在") || input.contains("进行") || input.contains("处理") ||
                output.contains("正在") || output.contains("进行") || output.contains("处理")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否有待处理的任务
     * @return 是否有待处理的任务
     */
    private boolean hasPendingTasks() {
        // 检查最近的消息中是否包含待处理任务的关键词
        int checkCount = Math.min(5, shortTermMemory.size());
        int startIndex = shortTermMemory.size() - checkCount;
        
        for (int i = startIndex; i < shortTermMemory.size(); i++) {
            MemoryItem item = shortTermMemory.get(i);
            String input = item.getInput() != null ? item.getInput().toLowerCase() : "";
            if (input.contains("待办") || input.contains("todo") || input.contains("任务") || input.contains("task")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否有最近的错误
     * @return 是否有最近的错误
     */
    private boolean hasRecentErrors() {
        // 检查最近的消息中是否包含错误信息
        int checkCount = Math.min(3, shortTermMemory.size());
        int startIndex = shortTermMemory.size() - checkCount;
        
        for (int i = startIndex; i < shortTermMemory.size(); i++) {
            MemoryItem item = shortTermMemory.get(i);
            String output = item.getOutput() != null ? item.getOutput().toLowerCase() : "";
            if (output.contains("error") || output.contains("exception") || output.contains("错误")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 8段式结构化压缩算法 - 基于Claude Code的AU2函数完整实现
     * 优化版本，提高性能和准确性
     * @return 压缩后的内存对象
     */
    private CompressedMemory perform8SegmentCompression() {
        // 使用更高效的StringBuilder初始容量
        int initialCapacity = Math.max(100, shortTermMemory.size() * 50);
        
        // 1. Background Context: 项目类型和技术栈
        StringBuilder backgroundContext = new StringBuilder(initialCapacity / 8);
        
        // 2. Key Decisions: 重要的技术选择和原因
        StringBuilder keyDecisions = new StringBuilder(initialCapacity / 8);
        
        // 3. Tool Usage Log: 主要使用的工具类型和文件操作历史
        StringBuilder toolUsage = new StringBuilder(initialCapacity / 8);
        
        // 4. User Intent Evolution: 需求的变化过程和优先级调整
        StringBuilder userIntent = new StringBuilder(initialCapacity / 8);
        
        // 5. Execution Results: 成功完成的任务和生成的代码
        StringBuilder executionResults = new StringBuilder(initialCapacity / 8);
        
        // 6. Errors and Solutions: 遇到的问题类型和错误处理方法
        StringBuilder errorsAndSolutions = new StringBuilder(initialCapacity / 8);
        
        // 7. Open Issues: 当前待解决的问题和已知的限制
        StringBuilder openIssues = new StringBuilder(initialCapacity / 8);
        
        // 8. Future Plans: 下一步行动计划和长期目标规划
        StringBuilder futurePlans = new StringBuilder(initialCapacity / 8);
        
        // 预编译正则表达式以提高性能
        // 背景上下文关键词
        String[] contextKeywords = {"项目", "应用", "系统", "架构", "技术栈", "环境", "框架", "库", "language", "framework", "stack"};
        
        // 关键决策关键词
        String[] decisionKeywords = {"决定", "选择", "决策", "采用", "使用", "方案", "设计", "架构", "decision", "choose", "select", "design"};
        
        // 工具使用关键词
        String[] toolKeywords = {"工具", "执行", "运行", "调用", "read", "write", "edit", "grep", "bash", "search", "tool", "execute", "run"};
        
        // 用户意图关键词
        String[] intentKeywords = {"需要", "要求", "希望", "目标", "目的", "意图", "需求", "功能", "需求", "want", "need", "require", "goal"};
        
        // 执行结果关键词
        String[] resultKeywords = {"完成", "成功", "实现", "生成", "创建", "完成", "验证", "测试", "success", "complete", "done", "finish"};
        
        // 错误和解决方案关键词
        String[] errorKeywords = {"错误", "异常", "问题", "失败", "error", "exception", "fail", "bug", "解决", "修复", "处理", "fix", "resolve"};
        
        // 开放问题关键词
        String[] issueKeywords = {"问题", "待解决", "限制", "约束", "困难", "挑战", "issue", "problem", "limitation", "constraint"};
        
        // 未来计划关键词
        String[] planKeywords = {"计划", "下一步", "后续", "未来", "长期", "目标", "计划", "下一步", "plan", "next", "future", "goal"};
        
        // 分析所有短期记忆项
        int size = shortTermMemory.size();
        int startIndex = Math.max(0, size - 15); // 分析最近的15条记录以提高准确性
        
        // 用于跟踪已添加的内容，避免重复
        Set<String> addedContext = new HashSet<>();
        Set<String> addedDecisions = new HashSet<>();
        Set<String> addedTools = new HashSet<>();
        Set<String> addedIntents = new HashSet<>();
        Set<String> addedResults = new HashSet<>();
        Set<String> addedErrors = new HashSet<>();
        Set<String> addedIssues = new HashSet<>();
        Set<String> addedPlans = new HashSet<>();
        
        for (int i = startIndex; i < size; i++) {
            MemoryItem item = shortTermMemory.get(i);
            String input = item.getInput() != null ? item.getInput() : "";
            String output = item.getOutput() != null ? item.getOutput() : "";
            
            // 分析用户消息
            if (!input.isEmpty()) {
                // 限制单个消息的长度以防止过长，但保留更多内容
                String trimmedInput = input.length() > 800 ? input.substring(0, 800) : input;
                
                // 背景上下文
                if (containsAnyKeyword(input, contextKeywords) && !addedContext.contains(trimmedInput)) {
                    backgroundContext.append(trimmedInput).append("; ");
                    addedContext.add(trimmedInput);
                }
                
                // 关键决策
                if (containsAnyKeyword(input, decisionKeywords) && !addedDecisions.contains(trimmedInput)) {
                    keyDecisions.append(trimmedInput).append("; ");
                    addedDecisions.add(trimmedInput);
                }
                
                // 工具使用
                if (containsAnyKeyword(input, toolKeywords) && !addedTools.contains(trimmedInput)) {
                    toolUsage.append(trimmedInput).append("; ");
                    addedTools.add(trimmedInput);
                }
                
                // 用户意图
                if (containsAnyKeyword(input, intentKeywords) && !addedIntents.contains(trimmedInput)) {
                    userIntent.append(trimmedInput).append("; ");
                    addedIntents.add(trimmedInput);
                }
                
                // 开放问题
                if (containsAnyKeyword(input, issueKeywords) && !addedIssues.contains(trimmedInput)) {
                    openIssues.append(trimmedInput).append("; ");
                    addedIssues.add(trimmedInput);
                }
                
                // 未来计划
                if (containsAnyKeyword(input, planKeywords) && !addedPlans.contains(trimmedInput)) {
                    futurePlans.append(trimmedInput).append("; ");
                    addedPlans.add(trimmedInput);
                }
            }
            
            // 分析输出消息
            if (!output.isEmpty()) {
                // 限制单个消息的长度以防止过长
                String trimmedOutput = output.length() > 800 ? output.substring(0, 800) : output;
                
                // 执行结果
                if (containsAnyKeyword(output, resultKeywords) && !addedResults.contains(trimmedOutput)) {
                    executionResults.append(trimmedOutput).append("; ");
                    addedResults.add(trimmedOutput);
                }
                
                // 错误和解决方案
                if (containsAnyKeyword(output, errorKeywords) && !addedErrors.contains(trimmedOutput)) {
                    errorsAndSolutions.append(trimmedOutput).append("; ");
                    addedErrors.add(trimmedOutput);
                }
                
                // 关键决策（从输出中也可以提取）
                if (containsAnyKeyword(output, decisionKeywords) && !addedDecisions.contains(trimmedOutput)) {
                    keyDecisions.append(trimmedOutput).append("; ");
                    addedDecisions.add(trimmedOutput);
                }
                
                // 工具使用（从输出中也可以提取）
                if (containsAnyKeyword(output, toolKeywords) && !addedTools.contains(trimmedOutput)) {
                    toolUsage.append(trimmedOutput).append("; ");
                    addedTools.add(trimmedOutput);
                }
                
                // 未来计划（从输出中也可以提取）
                if (containsAnyKeyword(output, planKeywords) && !addedPlans.contains(trimmedOutput)) {
                    futurePlans.append(trimmedOutput).append("; ");
                    addedPlans.add(trimmedOutput);
                }
            }
        }
        
        // 创建压缩内存对象
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
     * 检查字符串是否包含任何关键词
     * @param text 文本
     * @param keywords 关键词数组
     * @return 是否包含关键词
     */
    private boolean containsAnyKeyword(String text, String[] keywords) {
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
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
        
        logger.info("Restored {} files", restoredFiles.size());
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
     * 结合Claude Code的Token估算策略和更精确的算法
     * @param text 文本内容
     * @return 估算的Token数量
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 更精确的Token估算方法
        int charCount = text.length();
        
        // 检查是否包含中文字符
        boolean hasChinese = text.matches(".*[\\u4e00-\\u9fa5]+.*");
        
        // 检查是否包含英文单词
        boolean hasEnglish = text.matches(".*[a-zA-Z]+.*");
        
        if (hasChinese && hasEnglish) {
            // 混合文本：分别计算中英文部分
            int chineseCharCount = 0;
            int englishWordCount = 0;
            
            StringBuilder englishPart = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c >= 0x4e00 && c <= 0x9fa5) {
                    chineseCharCount++;
                } else {
                    englishPart.append(c);
                }
            }
            
            // 计算英文单词数
            String englishText = englishPart.toString().trim();
            if (!englishText.isEmpty()) {
                String[] words = englishText.split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty() && word.matches("[a-zA-Z]+")) {
                        englishWordCount++;
                    }
                }
            }
            
            // 混合文本的Token估算：中文字符数 * 0.6 + 英文单词数 * 1.2
            return (int) (chineseCharCount * 0.6 + englishWordCount * 1.2);
        } else if (hasChinese) {
            // 纯中文文本：每个字符约0.6个token
            return (int) (charCount * 0.6);
        } else if (hasEnglish) {
            // 纯英文文本：使用更精确的单词和子词估算
            String[] words = text.trim().split("\\s+");
            int wordCount = 0;
            int subwordCount = 0;
            
            for (String word : words) {
                if (!word.isEmpty() && word.matches("[a-zA-Z]+")) {
                    wordCount++;
                    // 估算子词数量（基于单词长度）
                    if (word.length() > 4) {
                        subwordCount += word.length() / 4;
                    }
                }
            }
            
            // 英文文本的Token估算：单词数 + 子词数 * 0.3
            return wordCount + (int) (subwordCount * 0.3);
        } else {
            // 其他字符（数字、符号等）：每个字符约0.3个token
            return (int) (charCount * 0.3);
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
    
    /**
     * 获取内存使用统计信息
     * @return 内存使用统计信息
     */
    public String getMemoryStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Memory Usage Statistics:\n");
        stats.append("  Short-term memory items: ").append(shortTermMemory.size()).append("\n");
        stats.append("  Medium-term memory items: ").append(mediumTermMemory.size()).append("\n");
        stats.append("  Long-term memory items: ").append(longTermMemory.size()).append("\n");
        stats.append("  File cache items: ").append(fileCache.size()).append("\n");
        stats.append("  Current token usage: ").append(currentTokenUsage).append("\n");
        stats.append("  Token usage ratio: ").append(String.format("%.2f", (double) currentTokenUsage / getMaxTokenLimit() * 100)).append("%\n");
        
        // 计算平均每个短期记忆项的Token数
        if (!shortTermMemory.isEmpty()) {
            int totalShortTermTokens = 0;
            for (MemoryItem item : shortTermMemory) {
                totalShortTermTokens += estimateTokens(item.getInput()) + estimateTokens(item.getOutput());
            }
            stats.append("  Average tokens per short-term item: ").append(totalShortTermTokens / shortTermMemory.size()).append("\n");
        }
        
        return stats.toString();
    }
}