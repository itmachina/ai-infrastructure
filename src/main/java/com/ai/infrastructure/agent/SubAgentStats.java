package com.ai.infrastructure.agent;

/**
 * 子Agent系统统计信息
 */
public class SubAgentStats {
    private final int activeAgents;
    private final int completedAgents;
    private final int sharedDataItems;
    private final long totalAgentsCreated;

    public SubAgentStats(int activeAgents, int completedAgents, int sharedDataItems, long totalAgentsCreated) {
        this.activeAgents = activeAgents;
        this.completedAgents = completedAgents;
        this.sharedDataItems = sharedDataItems;
        this.totalAgentsCreated = totalAgentsCreated;
    }

    public int getActiveAgents() {
        return activeAgents;
    }

    public int getCompletedAgents() {
        return completedAgents;
    }

    public int getSharedDataItems() {
        return sharedDataItems;
    }

    public long getTotalAgentsCreated() {
        return totalAgentsCreated;
    }

    @Override
    public String toString() {
        return String.format("SubAgentStats{active=%d, completed=%d, shared=%d, totalCreated=%d}",
                activeAgents, completedAgents, sharedDataItems, totalAgentsCreated);
    }
}