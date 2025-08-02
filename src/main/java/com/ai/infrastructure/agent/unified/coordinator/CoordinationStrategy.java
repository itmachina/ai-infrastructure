package com.ai.infrastructure.agent.unified.coordinator;

import com.ai.infrastructure.agent.unified.BaseUnifiedAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 协作策略接口
 */
public interface CoordinationStrategy {
    CompletableFuture<String> execute(String initiatorAgentId,
                                      String[] partnerAgentIds,
                                      String task,
                                      AgentCoordinator coordinator,
                                      ExecutorService executor);
    String getStrategyName();
}

/**
 * 并行协作策略
 */
class ParallelCoordinationStrategy implements CoordinationStrategy {
    private static final Logger logger = LoggerFactory.getLogger(ParallelCoordinationStrategy.class);

    @Override
    public CompletableFuture<String> execute(String initiatorAgentId,
                                          String[] partnerAgentIds,
                                          String task,
                                          AgentCoordinator coordinator,
                                          ExecutorService executor) {
        logger.info("Executing parallel coordination for task: {}", task);
        List<CompletableFuture<String>> futures = Arrays.stream(partnerAgentIds)
            .map(partnerId -> CompletableFuture.supplyAsync(() -> {
                BaseUnifiedAgent partner = coordinator.getRegisteredAgents().get(partnerId);
                if (partner != null) {
                    return partner.executeTask(task).join();
                }
                return "Partner agent not found: " + partnerId;
            }, executor))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                StringBuilder result = new StringBuilder("Parallel execution results:\n");
                for (int i = 0; i < partnerAgentIds.length; i++) {
                    try {
                        result.append(partnerAgentIds[i]).append(": ").append(futures.get(i).get()).append("\n");
                    } catch (Exception e) {
                        result.append(partnerAgentIds[i]).append(": ERROR - ").append(e.getMessage()).append("\n");
                    }
                }
                return result.toString();
            });
    }

    @Override
    public String getStrategyName() {
        return "PARALLEL";
    }
}

/**
 * 顺序协作策略
 */
class SequentialCoordinationStrategy implements CoordinationStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SequentialCoordinationStrategy.class);

    @Override
    public CompletableFuture<String> execute(String initiatorAgentId,
                                          String[] partnerAgentIds,
                                          String task,
                                          AgentCoordinator coordinator,
                                          ExecutorService executor) {
        logger.info("Executing sequential coordination for task: {}", task);
        CompletableFuture<String> chain = CompletableFuture.completedFuture(task);
        for (String partnerId : partnerAgentIds) {
            chain = chain.thenCompose(previousResult -> CompletableFuture.supplyAsync(() -> {
                BaseUnifiedAgent partner = coordinator.getRegisteredAgents().get(partnerId);
                if (partner != null) {
                    return partner.executeTask(previousResult).join();
                }
                return "Partner agent not found: " + partnerId;
            }, executor));
        }
        return chain;
    }

    @Override
    public String getStrategyName() {
        return "SEQUENTIAL";
    }
}

/**
 * 自适应协作策略
 */
class AdaptiveCoordinationStrategy implements CoordinationStrategy {
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveCoordinationStrategy.class);

    @Override
    public CompletableFuture<String> execute(String initiatorAgentId,
                                          String[] partnerAgentIds,
                                          String task,
                                          AgentCoordinator coordinator,
                                          ExecutorService executor) {
        logger.info("Executing adaptive coordination for task: {}", task);
        int complexity = calculateTaskComplexity(task);
        CoordinationStrategy strategy = (complexity > 5) ? new SequentialCoordinationStrategy() : new ParallelCoordinationStrategy();
        logger.debug("Selected strategy {} for complexity {}", strategy.getStrategyName(), complexity);
        return strategy.execute(initiatorAgentId, partnerAgentIds, task, coordinator, executor);
    }

    @Override
    public String getStrategyName() {
        return "ADAPTIVE";
    }

    private int calculateTaskComplexity(String task) {
        return task.length() / 10; // Simplified complexity calculation
    }
}

/**
 * 流水线协作策略
 */
class PipelineCoordinationStrategy implements CoordinationStrategy {
    private static final Logger logger = LoggerFactory.getLogger(PipelineCoordinationStrategy.class);

    @Override
    public CompletableFuture<String> execute(String initiatorAgentId,
                                          String[] partnerAgentIds,
                                          String task,
                                          AgentCoordinator coordinator,
                                          ExecutorService executor) {
        logger.info("Executing pipeline coordination for task: {}", task);
        CompletableFuture<String> pipeline = CompletableFuture.completedFuture(task);
        for (String partnerId : partnerAgentIds) {
            pipeline = pipeline.thenCompose(data -> CompletableFuture.supplyAsync(() -> {
                BaseUnifiedAgent partner = coordinator.getRegisteredAgents().get(partnerId);
                if (partner != null) {
                    return partner.executeTask(data).join();
                }
                return "Partner agent not found: " + partnerId;
            }, executor));
        }
        return pipeline;
    }

    @Override
    public String getStrategyName() {
        return "PIPELINE";
    }
}

/**
 * 负载均衡协作策略
 */
class LoadBalancedCoordinationStrategy implements CoordinationStrategy {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancedCoordinationStrategy.class);

    @Override
    public CompletableFuture<String> execute(String initiatorAgentId,
                                          String[] partnerAgentIds,
                                          String task,
                                          AgentCoordinator coordinator,
                                          ExecutorService executor) {
        logger.info("Executing load balanced coordination for task: {}", task);
        List<String> orderedAgents = orderAgentsByLoad(partnerAgentIds, coordinator);
        return new ParallelCoordinationStrategy().execute(initiatorAgentId, orderedAgents.toArray(new String[0]), task, coordinator, executor);
    }

    @Override
    public String getStrategyName() {
        return "LOAD_BALANCED";
    }

    private List<String> orderAgentsByLoad(String[] agentIds, AgentCoordinator coordinator) {
        Map<String, Double> loadScores = coordinator.getAgentLoadScores();
        return Arrays.stream(agentIds)
                .sorted((id1, id2) -> Double.compare(loadScores.getOrDefault(id1, 0.0), loadScores.getOrDefault(id2, 0.0)))
                .collect(Collectors.toList());
    }
}