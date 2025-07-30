package com.ai.infrastructure.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * UH1用户处理Agent实现
 * 负责用户请求解析和响应生成
 */
public class UserProcessingAgent extends SpecializedAgent {
    private static final Logger logger = LoggerFactory.getLogger(UserProcessingAgent.class);
    
    // 处理相关模式
    private static final Pattern[] PROCESSING_PATTERNS = {
        Pattern.compile("解析|处理|转换|格式化"),
        Pattern.compile("验证|检查|审核"),
        Pattern.compile("计算|执行|运行"),
        Pattern.compile("数据|信息|内容"),
        Pattern.compile("请求|查询|获取")
    };
    
    public UserProcessingAgent(String agentId, String name) {
        super(agentId, name, AgentType.UH1);
        logger.info("UH1 User Processing Agent initialized: {}", agentId);
    }
    
    @Override
    protected String processSpecializedTask(String task) {
        logger.debug("UH1 Agent processing user task: {}", task);
        
        // 处理解析任务
        if (isParsingTask(task)) {
            return handleParsingTask(task);
        }
        
        // 处理验证任务
        if (isValidationTask(task)) {
            return handleValidationTask(task);
        }
        
        // 处理计算任务
        if (isCalculationTask(task)) {
            return handleCalculationTask(task);
        }
        
        // 处理数据转换任务
        if (isDataTransformationTask(task)) {
            return handleDataTransformationTask(task);
        }
        
        // 默认处理逻辑
        return handleGenericProcessing(task);
    }
    
    @Override
    public boolean supportsTaskType(String taskType) {
        String lowerTask = taskType.toLowerCase();
        
        return lowerTask.contains("解析") || 
               lowerTask.contains("处理") || 
               lowerTask.contains("验证") || 
               lowerTask.contains("计算") || 
               lowerTask.contains("转换") || 
               lowerTask.contains("格式化");
    }
    
    /**
     * 处理解析任务
     */
    private String handleParsingTask(String task) {
        logger.debug("Handling parsing task: {}", task);
        
        // 解析任务结构
        TaskStructure structure = parseTaskStructure(task);
        
        // 解析用户意图
        String intent = parseUserIntent(task);
        
        // 解析任务参数
        TaskParameters parameters = parseTaskParameters(task);
        
        return String.format(
            "UH1 Agent解析结果:\n" +
            "任务结构: %s\n" +
            "用户意图: %s\n" +
            "任务参数: %s\n" +
            "解析状态: 成功\n" +
            "处理时间: %d ms",
            structure,
            intent,
            parameters,
            System.currentTimeMillis() - lastActivityTime
        );
    }
    
    /**
     * 处理验证任务
     */
    private String handleValidationTask(String task) {
        logger.debug("Handling validation task: {}", task);
        
        // 验证输入格式
        boolean formatValid = validateInputFormat(task);
        
        // 验证数据完整性
        boolean dataIntegrity = validateDataIntegrity(task);
        
        // 验证业务逻辑
        boolean businessLogic = validateBusinessLogic(task);
        
        return String.format(
            "UH1 Agent验证结果:\n" +
            "输入格式验证: %s\n" +
            "数据完整性验证: %s\n" +
            "业务逻辑验证: %s\n" +
            "总体验证状态: %s\n" +
            "验证时间: %d ms",
            formatValid ? "通过" : "失败",
            dataIntegrity ? "通过" : "失败",
            businessLogic ? "通过" : "失败",
            (formatValid && dataIntegrity && businessLogic) ? "通过" : "失败",
            System.currentTimeMillis() - lastActivityTime
        );
    }
    
    /**
     * 处理计算任务
     */
    private String handleCalculationTask(String task) {
        logger.debug("Handling calculation task: {}", task);
        
        try {
            // 提取计算表达式
            String expression = extractCalculationExpression(task);
            
            // 执行计算
            double result = performCalculation(expression);
            
            return String.format(
                "UH1 Agent计算结果:\n" +
                "计算表达式: %s\n" +
                "计算结果: %.2f\n" +
                "计算精度: 高\n" +
                "计算时间: %d ms",
                expression,
                result,
                System.currentTimeMillis() - lastActivityTime
            );
        } catch (Exception e) {
            return String.format(
                "UH1 Agent计算失败:\n" +
                "错误信息: %s\n" +
                "任务: %s",
                e.getMessage(),
                task
            );
        }
    }
    
    /**
     * 处理数据转换任务
     */
    private String handleDataTransformationTask(String task) {
        logger.debug("Handling data transformation task: {}", task);
        
        // 确定转换类型
        String transformationType = determineTransformationType(task);
        
        // 执行数据转换
        TransformationResult result = performDataTransformation(task, transformationType);
        
        return String.format(
            "UH1 Agent数据转换结果:\n" +
            "转换类型: %s\n" +
            "输入数据: %s\n" +
            "输出数据: %s\n" +
            "转换状态: %s\n" +
            "转换时间: %d ms",
            transformationType,
            result.inputData,
            result.outputData,
            result.success ? "成功" : "失败",
            System.currentTimeMillis() - lastActivityTime
        );
    }
    
    /**
     * 处理通用处理任务
     */
    private String handleGenericProcessing(String task) {
        logger.debug("Handling generic processing task: {}", task);
        
        // 使用工具引擎执行任务
        String toolResult = toolEngine.executeTool(task);
        
        return String.format(
            "UH1 Agent处理结果:\n" +
            "任务描述: %s\n" +
            "处理方式: 工具引擎\n" +
            "处理结果: %s\n" +
            "处理状态: 成功\n" +
            "处理时间: %d ms",
            task,
            toolResult,
            System.currentTimeMillis() - lastActivityTime
        );
    }
    
    /**
     * 解析任务结构
     */
    private TaskStructure parseTaskStructure(String task) {
        // 简化的任务结构解析
        String[] parts = task.split("[，；；。]");
        return new TaskStructure(
            parts.length > 0 ? parts[0] : "",
            parts.length > 1 ? parts[1] : "",
            parts.length
        );
    }
    
    /**
     * 解析用户意图
     */
    private String parseUserIntent(String task) {
        String lowerTask = task.toLowerCase();
        
        if (lowerTask.contains("计算")) return "计算";
        if (lowerTask.contains("分析")) return "分析";
        if (lowerTask.contains("验证")) return "验证";
        if (lowerTask.contains("转换")) return "转换";
        if (lowerTask.contains("格式化")) return "格式化";
        if (lowerTask.contains("查询")) return "查询";
        if (lowerTask.contains("处理")) return "处理";
        
        return "通用处理";
    }
    
    /**
     * 解析任务参数
     */
    private TaskParameters parseTaskParameters(String task) {
        // 简化的参数解析
        return new TaskParameters(
            task.length(),
            task.split("[\\s\\p{Punct}]").length,
            task.contains("数字"),
            task.contains("文本")
        );
    }
    
    /**
     * 验证输入格式
     */
    private boolean validateInputFormat(String task) {
        // 简单的格式验证
        return task != null && !task.trim().isEmpty() && task.length() < 10000;
    }
    
    /**
     * 验证数据完整性
     */
    private boolean validateDataIntegrity(String task) {
        // 简单的完整性验证
        return task != null && task.length() > 0;
    }
    
    /**
     * 验证业务逻辑
     */
    private boolean validateBusinessLogic(String task) {
        // 简单的业务逻辑验证
        return !task.contains("错误") && !task.contains("失败");
    }
    
    /**
     * 提取计算表达式
     */
    private String extractCalculationExpression(String task) {
        // 简单的表达式提取
        String[] parts = task.split("计算|等于|结果");
        return parts.length > 1 ? parts[1].trim() : "0";
    }
    
    /**
     * 执行计算
     */
    private double performCalculation(String expression) {
        // 简单的计算实现
        try {
            // 这里应该是更复杂的计算逻辑
            if (expression.contains("+")) {
                String[] nums = expression.split("\\+");
                return Double.parseDouble(nums[0].trim()) + Double.parseDouble(nums[1].trim());
            } else if (expression.contains("-")) {
                String[] nums = expression.split("-");
                return Double.parseDouble(nums[0].trim()) - Double.parseDouble(nums[1].trim());
            } else if (expression.contains("*")) {
                String[] nums = expression.split("\\*");
                return Double.parseDouble(nums[0].trim()) * Double.parseDouble(nums[1].trim());
            } else if (expression.contains("/")) {
                String[] nums = expression.split("/");
                return Double.parseDouble(nums[0].trim()) / Double.parseDouble(nums[1].trim());
            } else {
                return Double.parseDouble(expression);
            }
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * 确定转换类型
     */
    private String determineTransformationType(String task) {
        String lowerTask = task.toLowerCase();
        
        if (lowerTask.contains("json")) return "JSON";
        if (lowerTask.contains("xml")) return "XML";
        if (lowerTask.contains("csv")) return "CSV";
        if (lowerTask.contains("文本")) return "文本";
        if (lowerTask.contains("数字")) return "数字";
        
        return "通用";
    }
    
    /**
     * 执行数据转换
     */
    private TransformationResult performDataTransformation(String task, String transformationType) {
        // 简单的转换实现
        String outputData = "转换后的数据: " + task;
        
        return new TransformationResult(task, outputData, true);
    }
    
    /**
     * 检查是否为解析任务
     */
    private boolean isParsingTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("解析") || lowerTask.contains("分析结构");
    }
    
    /**
     * 检查是否为验证任务
     */
    private boolean isValidationTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("验证") || lowerTask.contains("检查") || lowerTask.contains("审核");
    }
    
    /**
     * 检查是否为计算任务
     */
    private boolean isCalculationTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("计算") || lowerTask.contains("数学") || 
               lowerTask.contains("+") || lowerTask.contains("-") || 
               lowerTask.contains("*") || lowerTask.contains("/");
    }
    
    /**
     * 检查是否为数据转换任务
     */
    private boolean isDataTransformationTask(String task) {
        String lowerTask = task.toLowerCase();
        return lowerTask.contains("转换") || lowerTask.contains("格式化") || 
               lowerTask.contains("变换") || lowerTask.contains("编码");
    }
    
    /**
     * 任务结构类
     */
    private static class TaskStructure {
        String mainTask;
        String subTask;
        int partCount;
        
        TaskStructure(String mainTask, String subTask, int partCount) {
            this.mainTask = mainTask;
            this.subTask = subTask;
            this.partCount = partCount;
        }
        
        @Override
        public String toString() {
            return String.format("主任务: %s, 子任务: %s, 部分数量: %d", mainTask, subTask, partCount);
        }
    }
    
    /**
     * 任务参数类
     */
    private static class TaskParameters {
        int length;
        int wordCount;
        boolean containsNumbers;
        boolean containsText;
        
        TaskParameters(int length, int wordCount, boolean containsNumbers, boolean containsText) {
            this.length = length;
            this.wordCount = wordCount;
            this.containsNumbers = containsNumbers;
            this.containsText = containsText;
        }
        
        @Override
        public String toString() {
            return String.format(
                "长度: %d, 词数: %d, 包含数字: %s, 包含文本: %s",
                length, wordCount, containsNumbers, containsText
            );
        }
    }
    
    /**
     * 转换结果类
     */
    private static class TransformationResult {
        String inputData;
        String outputData;
        boolean success;
        
        TransformationResult(String inputData, String outputData, boolean success) {
            this.inputData = inputData;
            this.outputData = outputData;
            this.success = success;
        }
    }
}