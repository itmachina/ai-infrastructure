package com.ai.infrastructure.agent.unified.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent性能追踪器
 * 追踪Agent的性能指标和运行表现
 */
public class AgentPerformanceTracker {
    private static final Logger logger = LoggerFactory.getLogger(AgentPerformanceTracker.class);
    
    private final String agentId;
    private final AgentStateManager stateManager;
    
    // 性能指标
    private final AtomicLong totalTasksProcessed;
    private final AtomicLong successfulTasks;
    private final AtomicLong failedTasks;
    private final AtomicLong totalExecutionTime;
    private final AtomicLong minExecutionTime;
    private final AtomicLong maxExecutionTime;
    
    // 滑动窗口统计
    private final Map<String, AtomicLong> rollingMetrics;
    private final AtomicReference<double[]> executionTimeHistory;
    private final int historySize = 100;
    private int historyIndex = 0;
    
    // 实时指标
    private final Map<String, Object> realTimeMetrics;
    
    public AgentPerformanceTracker(String agentId, AgentStateManager stateManager) {
        this.agentId = agentId;
        this.stateManager = stateManager;
        
        this.totalTasksProcessed = new AtomicLong(0);
        this.successfulTasks = new AtomicLong(0);
        this.failedTasks = new AtomicLong(0);
        this.totalExecutionTime = new AtomicLong(0);
        this.minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        this.maxExecutionTime = new AtomicLong(0);
        
        this.rollingMetrics = new ConcurrentHashMap<>();
        this.executionTimeHistory = new AtomicReference<>(new double[historySize]);
        this.realTimeMetrics = new ConcurrentHashMap<>();
        
        initializeRollingMetrics();
        initializeRealTimeMetrics();
    }
    
    private void initializeRollingMetrics() {
        rollingMetrics.put("tasks_last_5min", new AtomicLong(0));
        rollingMetrics.put("tasks_last_hour", new AtomicLong(0));
        rollingMetrics.put("tasks_last_day", new AtomicLong(0));
        rollingMetrics.put("errors_last_5min", new AtomicLong(0));
        rollingMetrics.put("errors_last_hour", new AtomicLong(0));
        rollingMetrics.put("errors_last_day", new AtomicLong(0));
    }
    
    private void initializeRealTimeMetrics() {
        realTimeMetrics.put("throughput", 0.0);
        realTimeMetrics.put("utilization", 0.0);
        realTimeMetrics.put("error_rate", 0.0);
        realTimeMetrics.put("avg_response_time", 0.0);
        realTimeMetrics.put("last_update_time", System.currentTimeMillis());
    }
    
    /**
     * 记录任务完成
     */
    public void recordTaskCompletion(boolean success, long executionTime) {
        totalTasksProcessed.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        
        if (success) {
            successfulTasks.incrementAndGet();
        } else {
            failedTasks.incrementAndGet();
        }
        
        updateExecutionTimeStats(executionTime);
        updateRollingMetrics(success, 1);
        
        updateRealTimeMetrics();
    }
    
    /**
     * 更新执行时间统计
     */
    private void updateExecutionTimeStats(long executionTime) {
        minExecutionTime.updateAndGet(current -> Math.min(current, executionTime));
        maxExecutionTime.updateAndGet(current -> Math.max(current, executionTime));
        
        // 更新滑动窗口
        double[] history = executionTimeHistory.get();
        history[historyIndex] = executionTime;
        historyIndex = (historyIndex + 1) % historySize;
    }
    
    /**
     * 更新滑动窗口指标
     */
    private void updateRollingMetrics(boolean success, int count) {
        // 简化的滑动窗口更新
        // 实际实现应该基于时间窗口
        if (success) {
            rollingMetrics.get("tasks_last_5min").addAndGet(count);
            rollingMetrics.get("tasks_last_hour").addAndGet(count);
            rollingMetrics.get("tasks_last_day").addAndGet(count);
        } else {
            rollingMetrics.get("errors_last_5min").addAndGet(count);
            rollingMetrics.get("errors_last_hour").addAndGet(count);
            rollingMetrics.get("errors_last_day").addAndGet(count);
        }
    }
    
    /**
     * 更新实时指标
     */
    private void updateRealTimeMetrics() {
        long totalTasks = totalTasksProcessed.get();
        long totalTime = totalExecutionTime.get();
        long errors = failedTasks.get();
        
        double avgTime = totalTasks > 0 ? (double) totalTime / totalTasks : 0.0;
        double errorRate = totalTasks > 0 ? (double) errors / totalTasks * 100 : 0.0;
        
        // 计算当前利用率（基于任务执行状态）
        double utilization = stateManager.isRunning() ? 1.0 : 0.0;
        
        realTimeMetrics.put("throughput", (double) totalTasks);
        realTimeMetrics.put("utilization", utilization);
        realTimeMetrics.put("error_rate", errorRate);
        realTimeMetrics.put("avg_response_time", avgTime);
        realTimeMetrics.put("last_update_time", System.currentTimeMillis());
    }
    
    /**
     * 获取当前性能指标
     */
    public Map<String, Object> getCurrentMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>(realTimeMetrics);
        
        long totalTasks = totalTasksProcessed.get();
        long successes = successfulTasks.get();
        long errors = failedTasks.get();
        long totalTime = totalExecutionTime.get();
        
        metrics.put("total_tasks", totalTasks);
        metrics.put("successful_tasks", successes);
        metrics.put("failed_tasks", errors);
        metrics.put("success_rate", totalTasks > 0 ? (double) successes / totalTasks * 100 : 0.0);
        metrics.put("error_rate", totalTasks > 0 ? (double) errors / totalTasks * 100 : 0.0);
        metrics.put("total_execution_time", totalTime);
        metrics.put("avg_execution_time", totalTasks > 0 ? (double) totalTime / totalTasks : 0.0);
        metrics.put("min_execution_time", minExecutionTime.get() == Long.MAX_VALUE ? 0L : minExecutionTime.get());
        metrics.put("max_execution_time", maxExecutionTime.get());
        
        return metrics;
    }
    
    /**
     * 获取性能概览
     */
    public String getPerformanceOverview() {
        Map<String, Object> metrics = getCurrentMetrics();
        
        StringBuilder overview = new StringBuilder();
        overview.append("=== 性能概览 ===\n");
        overview.append("Agent: ").append(agentId).append("\n");
        overview.append("总任务数: ").append(metrics.get("total_tasks")).append("\n");
        overview.append("成功任务: ").append(metrics.get("successful_tasks")).append("\n");
        overview.append("失败任务: ").append(metrics.get("failed_tasks")).append("\n");
        overview.append("成功率: ").append(String.format("%.2f", metrics.get("success_rate"))).append("%\n");
        overview.append("总执行时间: ").append(metrics.get("total_execution_time")).append("ms\n");
        overview.append("平均响应时间: ").append(String.format("%.2f", metrics.get("avg_execution_time"))).append("ms\n");
        overview.append("最短时间: ").append(metrics.get("min_execution_time")).append("ms\n");
        overview.append("最长时间: ").append(metrics.get("max_execution_time")).append("ms\n");
        overview.append("吞吐量: ").append(metrics.get("throughput")).append(" tasks\n");
        overview.append("利用率: ").append(String.format("%.2f", metrics.get("utilization"))).append("\n");
        
        return overview.toString();
    }
    
    /**
     * 获取性能趋势分析
     */
    public Map<String, Object> getPerformanceTrends() {
        Map<String, Object> trends = new ConcurrentHashMap<>();
        
        double[] history = executionTimeHistory.get();
        if (history == null || history.length == 0) {
            return trends;
        }
        
        // 计算趋势
        int validCount = 0;
        double sum = 0.0;
        
        for (double value : history) {
            if (value > 0) {
                sum += value;
                validCount++;
            }
        }
        
        if (validCount > 0) {
            double average = sum / validCount;
            double variance = calculateVariance(history, average, validCount);
            
            trends.put("cov", Math.sqrt(variance) / average); // 变异系数
            trends.put("trend", calculateLinearTrend(history));
        }
        
        return trends;
    }
    
    /**
     * 计算方差
     */
    private double calculateVariance(double[] data, double mean, int count) {
        double variance = 0.0;
        int valid = 0;
        
        for (double value : data) {
            if (value > 0) {
                variance += Math.pow(value - mean, 2);
                valid++;
            }
        }
        
        return valid > 0 ? variance / valid : 0.0;
    }
    
    /**
     * 计算线性趋势
     */
    private double calculateLinearTrend(double[] data) {
        int n = 0;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < data.length; i++) {
            if (data[i] > 0) {
                n++;
                sumX += i;
                sumY += data[i];
                sumXY += i * data[i];
                sumX2 += i * i;
            }
        }
        
        if (n < 2) {
            return 0.0;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }
    
    /**
     * 更新单个指标
     */
    public void updateMetric(String metric, Object value) {
        realTimeMetrics.put(metric, value);
        realTimeMetrics.put("last_update_time", System.currentTimeMillis());
    }
    
    /**
     * 生成性能报告
     */
    public String generatePerformanceReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== 详细性能报告 ===\n");
        report.append("Agent ID: ").append(agentId).append("\n");
        report.append("生成时间: ").append(System.currentTimeMillis()).append("\n\n");
        
        // 基础统计
        Map<String, Object> metrics = getCurrentMetrics();
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            report.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        
        report.append("\n").append(getPerformanceOverview()).append("\n");
        
        // 趋势信息
        Map<String, Object> trends = getPerformanceTrends();
        if (!trends.isEmpty()) {
            report.append("\n性能趋势:\n");
            for (Map.Entry<String, Object> entry : trends.entrySet()) {
                report.append("  ").append(entry.getKey()).append(" = ")
                      .append(entry.getValue()).append("\n");
            }
        }
        
        return report.toString();
    }
    
    /**
     * 获取性能警报
     */
    public String getPerformanceAlerts() {
        StringBuilder alerts = new StringBuilder();
        
        Map<String, Object> metrics = getCurrentMetrics();
        
        // 高错误率警报
        double errorRate = (double) metrics.getOrDefault("error_rate", 0.0);
        if (errorRate > 10.0) {
            alerts.append("警告: 错误率过高 - ").append(String.format("%.1f", errorRate)).append("%\n");
        }
        
        // 长时间执行警报
        double avgTime = (double) metrics.getOrDefault("avg_execution_time", 0.0);
        if (avgTime > 5000.0) { // 5秒
            alerts.append("警告: 平均执行时间过长 - ").append(String.format("%.0f", avgTime)).append("ms\n");
        }
        
        return alerts.toString();
    }
    
    /**
     * 重置性能追踪器
     */
    public void reset() {
        totalTasksProcessed.set(0);
        successfulTasks.set(0);
        failedTasks.set(0);
        totalExecutionTime.set(0);
        minExecutionTime.set(Long.MAX_VALUE);
        maxExecutionTime.set(0);
        
        rollingMetrics.clear();
        initializeRollingMetrics();
        initializeRealTimeMetrics();
        
        logger.debug("AgentPerformanceTracker reset");
    }
    
    /**
     * 获取基准性能信息
     */
    public Map<String, Object> getBenchmarkInfo() {
        Map<String, Object> benchmark = new ConcurrentHashMap<>();
        
        benchmark.put("baseline_avg_time", 1000.0); // 1秒基线
        benchmark.put("baseline_error_rate", 5.0); // 5%错误率基线
        benchmark.put("current_performance", calculateCurrentPerformanceScore());
        
        return benchmark;
    }
    
    /**
     * 计算当前性能分数
     */
    private double calculateCurrentPerformanceScore() {
        Map<String, Object> metrics = getCurrentMetrics();
        double avgTime = (double) metrics.getOrDefault("avg_execution_time", 0.0);
        double errorRate = (double) metrics.getOrDefault("error_rate", 0.0);
        
        double timeScore = Math.max(0, 100 - (avgTime / 1000.0) * 20); // 每1秒扣20分
        double errorScore = Math.max(0, 100 - errorRate * 10); // 每1%错误扣10分
        
        return (timeScore * 0.7) + (errorScore * 0.3);
    }
}