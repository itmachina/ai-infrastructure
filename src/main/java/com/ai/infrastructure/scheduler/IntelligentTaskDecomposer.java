package com.ai.infrastructure.scheduler;

import com.ai.infrastructure.agent.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;

/**
 * 智能任务分解器
 * 基于Claude Code分层多Agent架构实现复杂任务的智能分解和依赖分析
 */
public class IntelligentTaskDecomposer {
    private static final Logger logger = LoggerFactory.getLogger(IntelligentTaskDecomposer.class);
    
    // 任务复杂度关键词映射
    private static final Map<String, Double> COMPLEXITY_KEYWORDS = Map.ofEntries(
        Map.entry("分析", 0.3),
        Map.entry("设计", 0.5),
        Map.entry("开发", 0.7),
        Map.entry("实现", 0.6),
        Map.entry("测试", 0.4),
        Map.entry("部署", 0.3),
        Map.entry("优化", 0.4),
        Map.entry("集成", 0.8),
        Map.entry("重构", 0.6),
        Map.entry("研究", 0.5),
        Map.entry("调研", 0.5),
        Map.entry("创建", 0.6),
        Map.entry("构建", 0.7),
        Map.entry("维护", 0.4),
        Map.entry("监控", 0.2),
        Map.entry("报告", 0.2),
        Map.entry("文档", 0.1),
        Map.entry("计划", 0.3),
        Map.entry("评估", 0.4),
        Map.entry("审查", 0.3)
    );
    
    // Agent类型能力映射
    private static final Map<AgentType, Set<String>> AGENT_CAPABILITIES = Map.of(
        AgentType.I2A, Set.of("交互", "界面", "用户", "展示", "沟通", "汇报", "演示"),
        AgentType.UH1, Set.of("处理", "解析", "响应", "转换", "格式化", "验证"),
        AgentType.KN5, Set.of("知识", "推理", "学习", "分析", "决策", "评估", "优化")
    );
    
    /**
     * 任务分解请求
     */
    public static class TaskDecompositionRequest {
        private final String taskId;
        private final String description;
        private final TaskPriority priority;
        private final Optional<Date> deadline;
        
        public TaskDecompositionRequest(String taskId, String description, TaskPriority priority, Optional<Date> deadline) {
            this.taskId = taskId;
            this.description = description;
            this.priority = priority;
            this.deadline = deadline;
        }
        
        public String getTaskId() { return taskId; }
        public String getDescription() { return description; }
        public TaskPriority getPriority() { return priority; }
        public Optional<Date> getDeadline() { return deadline; }
    }
    
    /**
     * 任务分解结果
     */
    public static class TaskDecompositionResult {
        private final String taskId;
        private final List<TaskStep> steps;
        private final double complexity;
        private final long estimatedDuration;
        private final Map<String, AgentType> agentAssignments;
        private final List<TaskDependency> dependencies;
        
        public TaskDecompositionResult(String taskId, List<TaskStep> steps, double complexity, 
                                     long estimatedDuration, Map<String, AgentType> agentAssignments,
                                     List<TaskDependency> dependencies) {
            this.taskId = taskId;
            this.steps = steps;
            this.complexity = complexity;
            this.estimatedDuration = estimatedDuration;
            this.agentAssignments = agentAssignments;
            this.dependencies = dependencies;
        }
        
        public String getTaskId() { return taskId; }
        public List<TaskStep> getSteps() { return steps; }
        public double getComplexity() { return complexity; }
        public long getEstimatedDuration() { return estimatedDuration; }
        public Map<String, AgentType> getAgentAssignments() { return agentAssignments; }
        public List<TaskDependency> getDependencies() { return dependencies; }
    }
    
    /**
     * 任务步骤
     */
    public static class TaskStep {
        private final String stepId;
        private final String description;
        private final AgentType agentType;
        private final long estimatedDuration;
        private final Set<String> dependencies;
        private final TaskPriority priority;
        private final Map<String, Object> metadata;
        
        public TaskStep(String stepId, String description, AgentType agentType, 
                       long estimatedDuration, Set<String> dependencies, 
                       TaskPriority priority, Map<String, Object> metadata) {
            this.stepId = stepId;
            this.description = description;
            this.agentType = agentType;
            this.estimatedDuration = estimatedDuration;
            this.dependencies = dependencies;
            this.priority = priority;
            this.metadata = metadata;
        }
        
        public String getStepId() { return stepId; }
        public String getDescription() { return description; }
        public AgentType getAgentType() { return agentType; }
        public long getEstimatedDuration() { return estimatedDuration; }
        public Set<String> getDependencies() { return dependencies; }
        public TaskPriority getPriority() { return priority; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * 任务依赖关系
     */
    public static class TaskDependency {
        private final String fromStepId;
        private final String toStepId;
        private final DependencyType type;
        
        public TaskDependency(String fromStepId, String toStepId, DependencyType type) {
            this.fromStepId = fromStepId;
            this.toStepId = toStepId;
            this.type = type;
        }
        
        public String getFromStepId() { return fromStepId; }
        public String getToStepId() { return toStepId; }
        public DependencyType getType() { return type; }
    }
    
    /**
     * 依赖关系类型
     */
    public enum DependencyType {
        SEQUENCE,      // 顺序依赖
        PARALLEL,      // 并行执行
        CONDITIONAL,   // 条件依赖
        RESOURCE       // 资源依赖
    }
    
    /**
     * 任务优先级
     */
    public enum TaskPriority {
        CRITICAL(1000),
        HIGH(500),
        MEDIUM(100),
        LOW(10);
        
        private final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
        
        public int getValue() { return value; }
    }
    
    // 使用基础设施AgentType，避免类型冲突
    
    /**
     * 分解任务
     */
    public TaskDecompositionResult decomposeTask(TaskDecompositionRequest request) {
        logger.info("Starting task decomposition for task: {}", request.getTaskId());
        
        // 1. 分析任务复杂度
        double complexity = analyzeTaskComplexity(request.getDescription());
        
        // 2. 提取依赖关系
        List<TaskDependency> dependencies = extractDependencies(request.getDescription());
        
        // 3. 分析技能需求
        Map<AgentType, Double> skillRequirements = analyzeSkillRequirements(request.getDescription());
        
        // 4. 选择分解策略
        DecompositionStrategy strategy = selectDecompositionStrategy(complexity, request.getDescription());
        
        // 5. 执行任务分解
        List<TaskStep> steps = executeDecomposition(request, strategy, skillRequirements);
        
        // 6. 优化执行顺序
        steps = optimizeExecutionOrder(steps, dependencies);
        
        // 7. 分配Agent类型
        Map<String, AgentType> agentAssignments = assignAgents(steps, skillRequirements);
        
        // 8. 估算执行时间
        long estimatedDuration = calculateEstimatedDuration(steps);
        
        logger.info("Task decomposition completed for task: {}, complexity: {}, steps: {}", 
                   request.getTaskId(), complexity, steps.size());
        
        return new TaskDecompositionResult(
            request.getTaskId(),
            steps,
            complexity,
            estimatedDuration,
            agentAssignments,
            dependencies
        );
    }
    
    /**
     * 分析任务复杂度
     */
    private double analyzeTaskComplexity(String taskDescription) {
        double totalComplexity = 0.0;
        int keywordCount = 0;
        
        // 基于关键词的复杂度分析
        for (Map.Entry<String, Double> entry : COMPLEXITY_KEYWORDS.entrySet()) {
            String keyword = entry.getKey();
            double complexity = entry.getValue();
            
            // 使用正则表达式匹配关键词
            Pattern pattern = Pattern.compile(keyword);
            Matcher matcher = pattern.matcher(taskDescription);
            
            while (matcher.find()) {
                totalComplexity += complexity;
                keywordCount++;
            }
        }
        
        // 考虑任务长度因素
        double lengthFactor = Math.min(taskDescription.length() / 100.0, 2.0);
        
        // 考虑任务步骤数量（基于标点符号和连接词）
        int stepCount = estimateStepCount(taskDescription);
        double stepFactor = Math.min(stepCount / 5.0, 3.0);
        
        // 综合复杂度计算
        double baseComplexity = keywordCount > 0 ? totalComplexity / keywordCount : 0.3;
        double finalComplexity = baseComplexity * (1 + lengthFactor * 0.2 + stepFactor * 0.3);
        
        return Math.min(finalComplexity, 1.0);
    }
    
    /**
     * 提取依赖关系
     */
    private List<TaskDependency> extractDependencies(String taskDescription) {
        List<TaskDependency> dependencies = new ArrayList<>();
        
        // 基于关键词提取顺序依赖
        List<String> sequenceKeywords = List.of("然后", "接着", "之后", "然后", "其次", "随后", "接着");
        List<String> parallelKeywords = List.of("同时", "并行", "一并", "一起");
        
        String lowerDesc = taskDescription.toLowerCase();
        
        // 分析句子结构提取依赖
        String[] sentences = taskDescription.split("[。；；！？]");
        
        for (int i = 0; i < sentences.length - 1; i++) {
            String currentSentence = sentences[i].trim();
            String nextSentence = sentences[i + 1].trim();
            
            if (!currentSentence.isEmpty() && !nextSentence.isEmpty()) {
                // 检查是否为顺序依赖
                boolean hasSequenceKeyword = sequenceKeywords.stream()
                    .anyMatch(keyword -> nextSentence.contains(keyword));
                
                if (hasSequenceKeyword) {
                    dependencies.add(new TaskDependency(
                        "step_" + i, "step_" + (i + 1), DependencyType.SEQUENCE
                    ));
                }
            }
        }
        
        return dependencies;
    }
    
    /**
     * 分析技能需求
     */
    private Map<AgentType, Double> analyzeSkillRequirements(String taskDescription) {
        Map<AgentType, Double> skillRequirements = new HashMap<>();
        
        // 初始化各Agent类型的需求分数
        for (AgentType agentType : AgentType.values()) {
            skillRequirements.put(agentType, 0.0);
        }
        
        // 基于能力关键词匹配
        for (Map.Entry<AgentType, Set<String>> entry : AGENT_CAPABILITIES.entrySet()) {
            AgentType agentType = entry.getKey();
            Set<String> capabilities = entry.getValue();
            
            double matchScore = 0.0;
            for (String capability : capabilities) {
                if (taskDescription.contains(capability)) {
                    matchScore += 1.0;
                }
            }
            
            skillRequirements.put(agentType, matchScore);
        }
        
        // 归一化分数
        double totalScore = skillRequirements.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalScore > 0) {
            skillRequirements.replaceAll((k, v) -> v / totalScore);
        }
        
        return skillRequirements;
    }
    
    /**
     * 选择分解策略
     */
    private DecompositionStrategy selectDecompositionStrategy(double complexity, String taskDescription) {
        if (complexity < 0.3) {
            return DecompositionStrategy.SIMPLE;
        } else if (complexity < 0.6) {
            return DecompositionStrategy.MODULAR;
        } else if (complexity < 0.8) {
            return DecompositionStrategy.HIERARCHICAL;
        } else {
            return DecompositionStrategy.COMPLEX;
        }
    }
    
    /**
     * 执行任务分解
     */
    private List<TaskStep> executeDecomposition(TaskDecompositionRequest request, 
                                               DecompositionStrategy strategy,
                                               Map<AgentType, Double> skillRequirements) {
        List<TaskStep> steps = new ArrayList<>();
        String[] sentences = request.getDescription().split("[。；；！？]");
        
        switch (strategy) {
            case SIMPLE:
                steps = createSimpleSteps(request, sentences, skillRequirements);
                break;
            case MODULAR:
                steps = createModularSteps(request, sentences, skillRequirements);
                break;
            case HIERARCHICAL:
                steps = createHierarchicalSteps(request, skillRequirements);
                break;
            case COMPLEX:
                steps = createComplexSteps(request, skillRequirements);
                break;
        }
        
        return steps;
    }
    
    /**
     * 创建简单步骤
     */
    private List<TaskStep> createSimpleSteps(TaskDecompositionRequest request, 
                                           String[] sentences,
                                           Map<AgentType, Double> skillRequirements) {
        List<TaskStep> steps = new ArrayList<>();
        AgentType primaryAgent = determinePrimaryAgent(skillRequirements);
        
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                TaskStep step = new TaskStep(
                    "step_" + i,
                    sentence,
                    primaryAgent,
                    5000, // 5秒估算
                    Set.of(),
                    request.getPriority(),
                    Map.of("originalSentence", sentence, "stepIndex", i)
                );
                steps.add(step);
            }
        }
        
        return steps;
    }
    
    /**
     * 创建模块化步骤
     */
    private List<TaskStep> createModularSteps(TaskDecompositionRequest request, 
                                            String[] sentences,
                                            Map<AgentType, Double> skillRequirements) {
        List<TaskStep> steps = new ArrayList<>();
        
        // 按功能模块分组
        Map<String, List<String>> modules = groupByFunctionalModule(sentences);
        
        int stepIndex = 0;
        for (Map.Entry<String, List<String>> entry : modules.entrySet()) {
            String moduleName = entry.getKey();
            List<String> moduleSentences = entry.getValue();
            
            // 确定模块的主要Agent类型
            String moduleText = String.join(" ", moduleSentences);
            Map<AgentType, Double> moduleSkillRequirements = analyzeSkillRequirements(moduleText);
            AgentType moduleAgent = determinePrimaryAgent(moduleSkillRequirements);
            
            TaskStep step = new TaskStep(
                "step_" + stepIndex++,
                "处理" + moduleName + "模块: " + moduleText,
                moduleAgent,
                moduleSentences.size() * 3000, // 每个句子3秒
                Set.of(),
                request.getPriority(),
                Map.of("moduleName", moduleName, "sentenceCount", moduleSentences.size())
            );
            steps.add(step);
        }
        
        return steps;
    }
    
    /**
     * 创建层次化步骤
     */
    private List<TaskStep> createHierarchicalSteps(TaskDecompositionRequest request,
                                                 Map<AgentType, Double> skillRequirements) {
        List<TaskStep> steps = new ArrayList<>();
        
        // 创建规划阶段
        TaskStep planningStep = new TaskStep(
            "planning",
            "制定任务执行计划",
            AgentType.KN5,
            10000,
            Set.of(),
            TaskPriority.HIGH,
            Map.of("phase", "planning", "type", "preparation")
        );
        steps.add(planningStep);
        
        // 创建分析阶段
        TaskStep analysisStep = new TaskStep(
            "analysis",
            "分析任务需求和约束条件",
            AgentType.KN5,
            8000,
            Set.of("planning"),
            TaskPriority.HIGH,
            Map.of("phase", "analysis", "type", "preparation")
        );
        steps.add(analysisStep);
        
        // 创建执行阶段
        TaskStep executionStep = new TaskStep(
            "execution",
            "执行核心任务逻辑",
            AgentType.UH1,
            15000,
            Set.of("analysis"),
            TaskPriority.HIGH,
            Map.of("phase", "execution", "type", "core")
        );
        steps.add(executionStep);
        
        // 创建验证阶段
        TaskStep validationStep = new TaskStep(
            "validation",
            "验证任务执行结果",
            AgentType.UH1,
            5000,
            Set.of("execution"),
            TaskPriority.MEDIUM,
            Map.of("phase", "validation", "type", "verification")
        );
        steps.add(validationStep);
        
        // 创建报告阶段
        TaskStep reportingStep = new TaskStep(
            "reporting",
            "生成任务执行报告",
            AgentType.I2A,
            3000,
            Set.of("validation"),
            TaskPriority.LOW,
            Map.of("phase", "reporting", "type", "communication")
        );
        steps.add(reportingStep);
        
        return steps;
    }
    
    /**
     * 创建复杂步骤
     */
    private List<TaskStep> createComplexSteps(TaskDecompositionRequest request,
                                            Map<AgentType, Double> skillRequirements) {
        List<TaskStep> steps = new ArrayList<>();
        
        // 创建多个层次的步骤
        steps.addAll(createHierarchicalSteps(request, skillRequirements));
        
        // 添加专门的子任务处理步骤
        TaskStep subtaskPlanning = new TaskStep(
            "subtask_planning",
            "规划和分解子任务",
            AgentType.KN5,
            12000,
            Set.of("analysis"),
            TaskPriority.HIGH,
            Map.of("phase", "subtask", "type", "planning")
        );
        steps.add(subtaskPlanning);
        
        TaskStep subtaskExecution = new TaskStep(
            "subtask_execution",
            "并行执行子任务",
            AgentType.UH1,
            20000,
            Set.of("subtask_planning"),
            TaskPriority.HIGH,
            Map.of("phase", "subtask", "type", "execution")
        );
        steps.add(subtaskExecution);
        
        TaskStep resultIntegration = new TaskStep(
            "result_integration",
            "整合子任务执行结果",
            AgentType.KN5,
            8000,
            Set.of("subtask_execution"),
            TaskPriority.HIGH,
            Map.of("phase", "subtask", "type", "integration")
        );
        steps.add(resultIntegration);
        
        return steps;
    }
    
    /**
     * 优化执行顺序
     */
    private List<TaskStep> optimizeExecutionOrder(List<TaskStep> steps, List<TaskDependency> dependencies) {
        // 简单的拓扑排序实现
        List<TaskStep> optimizedSteps = new ArrayList<>();
        Set<String> processedSteps = new HashSet<>();
        
        // 首先添加没有依赖的步骤
        for (TaskStep step : steps) {
            if (step.getDependencies().isEmpty()) {
                optimizedSteps.add(step);
                processedSteps.add(step.getStepId());
            }
        }
        
        // 然后处理有依赖的步骤
        boolean progress = true;
        while (progress && processedSteps.size() < steps.size()) {
            progress = false;
            
            for (TaskStep step : steps) {
                if (!processedSteps.contains(step.getStepId())) {
                    // 检查所有依赖是否已处理
                    boolean allDependenciesProcessed = true;
                    for (String dependency : step.getDependencies()) {
                        if (!processedSteps.contains(dependency)) {
                            allDependenciesProcessed = false;
                            break;
                        }
                    }
                    
                    if (allDependenciesProcessed) {
                        optimizedSteps.add(step);
                        processedSteps.add(step.getStepId());
                        progress = true;
                    }
                }
            }
        }
        
        // 如果仍有未处理的步骤，按原始顺序添加
        for (TaskStep step : steps) {
            if (!processedSteps.contains(step.getStepId())) {
                optimizedSteps.add(step);
            }
        }
        
        return optimizedSteps;
    }
    
    /**
     * 分配Agent类型
     */
    private Map<String, AgentType> assignAgents(List<TaskStep> steps, Map<AgentType, Double> skillRequirements) {
        Map<String, AgentType> assignments = new HashMap<>();
        
        for (TaskStep step : steps) {
            // 如果步骤已指定Agent类型，直接使用
            if (step.getAgentType() != null) {
                assignments.put(step.getStepId(), step.getAgentType());
            } else {
                // 否则基于技能需求分配
                AgentType assignedAgent = determinePrimaryAgent(skillRequirements);
                assignments.put(step.getStepId(), assignedAgent);
            }
        }
        
        return assignments;
    }
    
    /**
     * 确定主要Agent类型
     */
    private AgentType determinePrimaryAgent(Map<AgentType, Double> skillRequirements) {
        return skillRequirements.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(AgentType.UH1); // 默认使用UH1
    }
    
    /**
     * 估算执行时间
     */
    private long calculateEstimatedDuration(List<TaskStep> steps) {
        // 简单地将所有步骤的时间相加
        // 在实际实现中，可以考虑并行执行的时间优化
        return steps.stream()
            .mapToLong(TaskStep::getEstimatedDuration)
            .sum();
    }
    
    /**
     * 估算步骤数量
     */
    private int estimateStepCount(String taskDescription) {
        // 基于句子分割符估算步骤数量
        String[] sentences = taskDescription.split("[。；；！？]");
        int sentenceCount = (int) Arrays.stream(sentences).filter(s -> !s.trim().isEmpty()).count();
        
        // 基于连接词估算步骤数量
        String[] connectors = {"首先", "然后", "接着", "之后", "其次", "随后", "最后", "第一步", "第二步", "第三步"};
        int connectorCount = 0;
        for (String connector : connectors) {
            connectorCount += taskDescription.split(connector).length - 1;
        }
        
        return Math.max(sentenceCount, connectorCount + 1);
    }
    
    /**
     * 按功能模块分组
     */
    private Map<String, List<String>> groupByFunctionalModule(String[] sentences) {
        Map<String, List<String>> modules = new HashMap<>();
        
        // 简单的模块分组逻辑
        Map<String, String> moduleKeywords = Map.of(
            "设计", "设计",
            "开发", "开发",
            "测试", "测试",
            "部署", "部署",
            "分析", "分析",
            "文档", "文档"
        );
        
        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (!trimmedSentence.isEmpty()) {
                String moduleName = "其他";
                
                for (Map.Entry<String, String> entry : moduleKeywords.entrySet()) {
                    if (trimmedSentence.contains(entry.getKey())) {
                        moduleName = entry.getValue();
                        break;
                    }
                }
                
                modules.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(trimmedSentence);
            }
        }
        
        return modules;
    }
    
    /**
     * 分解策略枚举
     */
    private enum DecompositionStrategy {
        SIMPLE,      // 简单分解
        MODULAR,     // 模块化分解
        HIERARCHICAL, // 层次化分解
        COMPLEX      // 复杂分解
    }
}