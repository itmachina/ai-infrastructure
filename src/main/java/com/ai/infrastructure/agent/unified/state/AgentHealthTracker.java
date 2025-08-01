package com.ai.infrastructure.agent.unified.state;

import com.ai.infrastructure.agent.AgentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent健康追踪器
 * 监控Agent的运行健康状况
 */
public class AgentHealthTracker {
    private static final Logger logger = LoggerFactory.getLogger(AgentHealthTracker.class);
    
    private final AgentStateManager stateManager;
    
    // 健康指标
    private final Map<String, Long> healthMetrics;
    private final AtomicLong totalErrors;
    private final AtomicLong consecutiveErrors;
    private final AtomicLong totalTasks;
    private final AtomicLong successfulTasks;
    
    // 健康阈值
    private static final long ERROR_THRESHOLD = 5;
    private static final long CONSECUTIVE_ERROR_THRESHOLD = 3;
    private static final long MAX_IDLE_TIME = 300000; // 5分钟
    
    public AgentHealthTracker(AgentStateManager stateManager) {
        this.stateManager = stateManager;
        this.healthMetrics = new ConcurrentHashMap<>();
        this.totalErrors = new AtomicLong(0);
        this.consecutiveErrors = new AtomicLong(0);
        this.totalTasks = new AtomicLong(0);
        this.successfulTasks = new AtomicLong(0);
        
        initializeHealthMetrics();
    }
    
    private void initializeHealthMetrics() {
        healthMetrics.put("system_uptime", System.currentTimeMillis());
        healthMetrics.put("last_health_check", System.currentTimeMillis());
        healthMetrics.put("memory_usage", 0L);
        healthMetrics.put("cpu_usage", 0L);
        healthMetrics.put("active_connections", 0L);
    }
    
    /**
     * 获取当前健康状态
     */
    public AgentStateManager.AgentHealthStatus getHealthStatus() {
        performHealthCheck();
        
        long errors = totalErrors.get();
        long consecutive = consecutiveErrors.get();
        long idleTime = stateManager.getCurrentStatusDuration();
        
        if (errors > ERROR_THRESHOLD || consecutive > CONSECUTIVE_ERROR_THRESHOLD) {
            return AgentStateManager.AgentHealthStatus.CRITICAL;
        }
        
        if (idleTime > MAX_IDLE_TIME && totalTasks.get() == 0) {
            return AgentStateManager.AgentHealthStatus.WARNING;
        }
        
        if (stateManager.isError()) {
            return AgentStateManager.AgentHealthStatus.WARNING;
        }
        
        if (consecutive > 0) {
            return AgentStateManager.AgentHealthStatus.WARNING;
        }
        
        return AgentStateManager.AgentHealthStatus.HEALTHY;
    }
    
    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        healthMetrics.put("last_health_check", System.currentTimeMillis());
        
        // 根据当前状态更新指标
        AgentStatus currentStatus = stateManager.getStatus();
        healthMetrics.put("current_status_duration", stateManager.getCurrentStatusDuration());
        
        if (stateManager.getCurrentTask() != null) {
            healthMetrics.put("task_duration", stateManager.getCurrentTaskDuration());
        }
    }
    
    /**
     * 记录任务执行
     */
    public void recordTaskExecution(boolean success) {
        totalTasks.incrementAndGet();
        
        if (success) {
            successfulTasks.incrementAndGet();
            consecutiveErrors.set(0);
        } else {
            totalErrors.incrementAndGet();
            consecutiveErrors.incrementAndGet();
        }
        
        updateHealthMetrics();
    }
    
    /**
     * 记录错误
     */
    public void recordError() {
        totalErrors.incrementAndGet();
        consecutiveErrors.incrementAndGet();
    }
    
    /**
     * 重置错误计数
     */
    public void resetErrorCount() {
        consecutiveErrors.set(0);
    }
    
    /**
     * 获取健康指标
     */
    public Map<String, Long> getHealthMetrics() {
        return new ConcurrentHashMap<>(healthMetrics);
    }
    
    /**
     * 获取健康概览
     */
    public String getHealthOverview() {
        StringBuilder overview = new StringBuilder();
        
        overview.append("=== 健康概览 ===\n");
        overview.append("健康状态: ").append(getHealthStatus()).append("\n");
        overview.append("总任务数: ").append(totalTasks.get()).append("\n");
        overview.append("成功任务: ").append(successfulTasks.get()).append("\n");
        overview.append("总错误数: ").append(totalErrors.get()).append("\n");
        overview.append("连续错误: ").append(consecutiveErrors.get()).append("\n");
        overview.append("成功率: ").append(calculateSuccessRate()).append("%\n");
        
        long systemUptime = System.currentTimeMillis() - healthMetrics.getOrDefault("system_uptime", 0L);
        overview.append("系统运行时间: ").append(systemUptime / 1000).append("秒\n");
        
        overview.append("最后健康检查: ").append(healthMetrics.get("last_health_check")).append("\n");
        
        return overview.toString();
    }
    
    /**
     * 计算成功率
     */
    public double calculateSuccessRate() {
        long total = totalTasks.get();
        if (total == 0) {
            return 100.0;
        }
        return (double) successfulTasks.get() / total * 100;
    }
    
    /**
     * 更新健康指标
     */
    private void updateHealthMetrics() {
        long uptime = System.currentTimeMillis() - healthMetrics.getOrDefault("system_uptime", System.currentTimeMillis());
        healthMetrics.put("uptime", uptime);
    }
    
    /**
     * 重置健康追踪器
     */
    public void reset() {
        totalErrors.set(0);
        consecutiveErrors.set(0);
        totalTasks.set(0);
        successfulTasks.set(0);
        
        healthMetrics.clear();
        initializeHealthMetrics();
        
        logger.debug("AgentHealthTracker reset");
    }
    
    /**
     * 获取详细的健康报告
     */
    public String getDetailedHealthReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== 详细健康报告 ===\n");
        report.append("健康追踪器初始时间: ").append(healthMetrics.get("system_uptime")).append("\n");
        report.append("最后健康检查: ").append(healthMetrics.get("last_health_check")).append("\n");
        
        for (Map.Entry<String, Long> entry : healthMetrics.entrySet()) {
            report.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        
        report.append("\n统计信息:\n");
        report.append("- 成功任务率: ").append(String.format("%.2f", calculateSuccessRate())).append("%\n");
        report.append("- 错误任务数: ").append(totalErrors.get()).append("\n");
        report.append("- 连续错误数: ").append(consecutiveErrors.get()).append("\n");
        report.append("- 健康级别: ").append(getHealthStatus()).append("\n");
        
        return report.toString();
    }
    
    /**
     * 检查是否需要健康告警
     */
    public boolean needsHealthAlert() {
        return totalErrors.get() > ERROR_THRESHOLD || 
               consecutiveErrors.get() > CONSECUTIVE_ERROR_THRESHOLD ||
               getHealthStatus() == AgentStateManager.AgentHealthStatus.CRITICAL;
    }
}