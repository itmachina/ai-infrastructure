package com.ai.infrastructure.agent.unified.coordinator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 协调器性能指标
 * 记录协调操作的统计信息
 */
public class CoordinationMetrics {
    
    /**
     * 将指标转换为Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("registration_count", getRegistrationCount());
        metricsMap.put("unregistration_count", getUnregistrationCount());
        metricsMap.put("collaboration_request_count", getCollaborationRequestCount());
        metricsMap.put("collaboration_success_count", getCollaborationSuccessCount());
        metricsMap.put("collaboration_failure_count", getCollaborationFailureCount());
        metricsMap.put("total_collaboration_duration", getTotalCollaborationDuration());
        metricsMap.put("min_collaboration_duration", getMinCollaborationDuration());
        metricsMap.put("max_collaboration_duration", getMaxCollaborationDuration());
        metricsMap.put("average_collaboration_duration", getAverageCollaborationDuration());
        metricsMap.put("success_rate", getSuccessRate());
        return metricsMap;
    }
    private final AtomicLong registrationCount = new AtomicLong(0);
    private final AtomicLong unregistrationCount = new AtomicLong(0);
    private final AtomicLong collaborationRequestCount = new AtomicLong(0);
    private final AtomicLong collaborationSuccessCount = new AtomicLong(0);
    private final AtomicLong collaborationFailureCount = new AtomicLong(0);
    private final AtomicLong totalCollaborationDuration = new AtomicLong(0);
    private final AtomicLong minCollaborationDuration = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxCollaborationDuration = new AtomicLong(0);
    
    public void incrementRegistrationCount() {
        registrationCount.incrementAndGet();
    }
    
    public void incrementUnregistrationCount() {
        unregistrationCount.incrementAndGet();
    }
    
    public void incrementCollaborationRequestCount() {
        collaborationRequestCount.incrementAndGet();
    }
    
    public void incrementCollaborationSuccessCount() {
        collaborationSuccessCount.incrementAndGet();
    }
    
    public void incrementCollaborationFailureCount() {
        collaborationFailureCount.incrementAndGet();
    }
    
    public void recordCollaborationDuration(long duration) {
        totalCollaborationDuration.addAndGet(duration);
        minCollaborationDuration.updateAndGet(current -> Math.min(current, duration));
        maxCollaborationDuration.updateAndGet(current -> Math.max(current, duration));
    }
    
    public long getRegistrationCount() {
        return registrationCount.get();
    }
    
    public long getUnregistrationCount() {
        return unregistrationCount.get();
    }
    
    public long getCollaborationRequestCount() {
        return collaborationRequestCount.get();
    }
    
    public long getCollaborationSuccessCount() {
        return collaborationSuccessCount.get();
    }
    
    public long getCollaborationFailureCount() {
        return collaborationFailureCount.get();
    }
    
    public long getTotalCollaborationDuration() {
        return totalCollaborationDuration.get();
    }
    
    public long getMinCollaborationDuration() {
        return minCollaborationDuration.get() == Long.MAX_VALUE ? 0 : minCollaborationDuration.get();
    }
    
    public long getMaxCollaborationDuration() {
        return maxCollaborationDuration.get();
    }
    
    public double getAverageCollaborationDuration() {
        long total = totalCollaborationDuration.get();
        long count = collaborationRequestCount.get();
        return count > 0 ? (double) total / count : 0.0;
    }
    
    public double getSuccessRate() {
        long total = collaborationSuccessCount.get() + collaborationFailureCount.get();
        return total > 0 ? (double) collaborationSuccessCount.get() / total * 100 : 100.0;
    }
    
    public String getMetricsReport() {
        return String.format(
            "CoordinationMetrics[registrations=%d, unregistrations=%d, " +
            "collaborations=%d, successes=%d, failures=%d, success_rate=%.2f%%, " +
            "avg_duration=%.2fms, min=%dms, max=%dms]",
            getRegistrationCount(), getUnregistrationCount(),
            getCollaborationRequestCount(), getCollaborationSuccessCount(),
            getCollaborationFailureCount(), getSuccessRate(),
            getAverageCollaborationDuration(), getMinCollaborationDuration(),
            getMaxCollaborationDuration()
        );
    }
    
    public void reset() {
        registrationCount.set(0);
        unregistrationCount.set(0);
        collaborationRequestCount.set(0);
        collaborationSuccessCount.set(0);
        collaborationFailureCount.set(0);
        totalCollaborationDuration.set(0);
        minCollaborationDuration.set(Long.MAX_VALUE);
        maxCollaborationDuration.set(0);
    }
}