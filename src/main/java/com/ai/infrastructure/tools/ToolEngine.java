package com.ai.infrastructure.tools;

import com.ai.infrastructure.config.ToolConfigManager;
import com.ai.infrastructure.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具引擎，实现6阶段执行流程
 * 基于Claude Code的工具执行机制实现完整的安全检查和执行流程
 */
public class ToolEngine {
    private static final Logger logger = LoggerFactory.getLogger(ToolEngine.class);
    
    private Map<String, ToolExecutor> registeredTools;
    private SecurityManager securityManager;
    private Map<String, Object> executionContext;
    
    // 工具替代强制机制 - 禁止使用的传统命令
    private static final String[] FORBIDDEN_COMMANDS = {"find", "grep", "cat", "head", "tail", "ls"};
    
    // 配置管理器
    private final ToolConfigManager configManager;
    
    public ToolEngine() {
        this.registeredTools = new HashMap<>();
        this.securityManager = new SecurityManager();
        this.executionContext = new HashMap<>();
        this.configManager = ToolConfigManager.getInstance();
        registerDefaultTools();
    }
    
    /**
     * 注册默认工具
     */
    private void registerDefaultTools() {
        registeredTools.put("read", new ReadToolExecutor());
        registeredTools.put("write", new WriteToolExecutor());
        registeredTools.put("search", new SearchToolExecutor());
        registeredTools.put("calculate", new CalculateToolExecutor());
        registeredTools.put("web_search", new WebSearchToolExecutor());
        registeredTools.put("task", new TaskToolExecutor());
        registeredTools.put("bash", new BashToolExecutor());
    }
    
    /**
     * 执行工具 - 实现完整的6阶段执行流程
     * 基于Claude Code的安全执行机制优化
     * @param task 任务描述
     * @return 执行结果
     */
    public String executeTool(String task) {
        logger.debug("Executing tool for task: {}", task);
        
        try {
            // 阶段1: 工具发现与验证
            String toolName = discoverTool(task);
            if (toolName == null || !registeredTools.containsKey(toolName)) {
                // 检查是否尝试使用被禁止的命令
                String forbiddenCommand = checkForbiddenCommands(task);
                if (forbiddenCommand != null) {
                    logger.warn("Forbidden command detected: {}", forbiddenCommand);
                    return "Error: Command '" + forbiddenCommand + "' is not allowed. Please use the appropriate tool instead.";
                }
                logger.warn("Unknown tool for task: {}", task);
                return "Unknown tool for task: " + task;
            }
            
            // 记录工具执行开始
            logger.info("Starting execution of tool '{}' with task: {}", toolName, task);
            
            // 阶段2: 输入验证 (Zod Schema风格验证)
            if (!validateInput(task)) {
                logger.warn("Input validation failed for task: {}", task);
                return "Input validation failed for task: " + task;
            }
            
            // 阶段3: 权限检查与门控
            if (!checkPermissions(toolName, task)) {
                logger.warn("Permission denied for tool: {}", toolName);
                return "Permission denied for tool: " + toolName;
            }
            
            // 阶段4: 取消检查 (AbortController风格)
            if (isCancelled()) {
                logger.info("Task was cancelled");
                return "Task was cancelled";
            }
            
            // 阶段5: 工具执行
            ToolExecutor executor = registeredTools.get(toolName);
            logger.debug("Executing tool: {}", toolName);
            String result = executor.execute(task);
            logger.debug("Tool execution completed: {}", toolName);
            
            // 阶段6: 结果格式化与清理
            String formattedResult = formatResult(result);
            
            // 记录工具执行成功
            logger.info("Tool '{}' executed successfully", toolName);
            
            return formattedResult;
            
        } catch (Exception e) {
            // 记录工具执行错误
            logger.error("Tool execution failed: {}", e.getMessage(), e);
            return "Error executing tool: " + e.getMessage();
        }
    }
    
    /**
     * 使用参数执行工具 - 支持AI决策的参数化工具调用
     * @param toolName 工具名称
     * @param parameters 参数映射
     * @return 执行结果
     */
    public String executeToolWithParameters(String toolName, Map<String, Object> parameters) {
        logger.debug("Executing tool '{}' with parameters: {}", toolName, parameters);
        
        try {
            // 检查工具是否已注册
            if (!registeredTools.containsKey(toolName)) {
                logger.error("Tool '{}' not found", toolName);
                return "Error: Tool '" + toolName + "' not found";
            }
            
            // 验证工具名称安全性
            if (detectCommandInjection(toolName)) {
                logger.error("Tool name contains potential command injection: {}", toolName);
                return "Error: Invalid tool name";
            }
            
            // 参数安全性验证
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();
                
                if (paramValue instanceof String) {
                    String paramStr = (String) paramValue;
                    if (detectCommandInjection(paramStr)) {
                        logger.error("Parameter '{}' contains potential command injection: {}", paramName, paramStr);
                        return "Error: Invalid parameter value for '" + paramName + "'";
                    }
                }
            }
            
            // 构建工具调用任务
            String task = buildTaskFromParameters(toolName, parameters);
            
            // 执行工具
            return executeTool(task);
            
        } catch (Exception e) {
            logger.error("Error executing tool '{}' with parameters: {}", toolName, e.getMessage(), e);
            return "Error executing tool: " + e.getMessage();
        }
    }
    
    /**
     * 根据参数构建任务描述
     */
    private String buildTaskFromParameters(String toolName, Map<String, Object> parameters) {
        StringBuilder taskBuilder = new StringBuilder();
        taskBuilder.append("Execute ").append(toolName);
        
        if (parameters != null && !parameters.isEmpty()) {
            taskBuilder.append(" with parameters: ");
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                taskBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
            }
            // 移除最后的逗号和空格
            if (taskBuilder.length() > 2) {
                taskBuilder.setLength(taskBuilder.length() - 2);
            }
        }
        
        return taskBuilder.toString();
    }
    
    /**
     * 基于Claude Code的工具执行增强实现 - 支持更安全的工具执行
     * @param task 任务描述
     * @param maxRetries 最大重试次数
     * @return 执行结果
     */
    public String executeToolWithRetry(String task, int maxRetries) {
        logger.debug("Executing tool with retry for task: {}, max retries: {}", task, maxRetries);
        
        int retryCount = 0;
        Exception lastException = null;
        
        // 记录任务开始执行
        logger.info("Starting execution of task: {}", task);
        
        while (retryCount <= maxRetries) {
            try {
                // 记录当前尝试次数
                if (retryCount > 0) {
                    logger.info("Retrying task (attempt {} of {}): {}", retryCount + 1, maxRetries + 1, task);
                }
                
                // 执行工具
                String result = executeTool(task);
                
                // 检查结果是否包含错误
                if (result.startsWith("Error:")) {
                    // 记录错误信息
                    logger.warn("Tool execution returned error: {}", result);
                    
                    // 如果是权限错误或安全错误，不重试
                    if (result.contains("Permission denied") || result.contains("command_injection_detected")) {
                        logger.error("Critical error detected, stopping retries: {}", result);
                        return result;
                    }
                    
                    // 如果是其他错误，可以重试
                    if (retryCount < maxRetries) {
                        retryCount++;
                        logger.info("Scheduling retry in {}ms", 1000 * retryCount);
                        Thread.sleep(1000 * retryCount); // 指数退避
                        continue;
                    } else {
                        logger.warn("Max retries reached, giving up on task: {}", task);
                    }
                } else {
                    // 记录成功执行
                    logger.info("Task executed successfully after {} attempts", retryCount + 1);
                }
                
                return result;
            } catch (InterruptedException ie) {
                // 处理中断异常
                logger.warn("Tool execution interrupted: {}", ie.getMessage());
                Thread.currentThread().interrupt();
                return "Error: Tool execution interrupted";
            } catch (Exception e) {
                lastException = e;
                logger.warn("Exception during tool execution (attempt {}): {}", retryCount + 1, e.getMessage(), e);
                
                if (retryCount < maxRetries) {
                    retryCount++;
                    try {
                        logger.info("Scheduling retry in {}ms due to exception", 1000 * retryCount);
                        Thread.sleep(1000 * retryCount); // 指数退避
                    } catch (InterruptedException ie) {
                        logger.warn("Retry interrupted: {}", ie.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    logger.warn("Max retries reached after exception, giving up on task: {}", task);
                    break;
                }
            }
        }
        
        String errorMessage = "Error executing tool after " + maxRetries + " retries: " + 
               (lastException != null ? lastException.getMessage() : "Unknown error");
        logger.error("{}", errorMessage);
        return errorMessage;
    }
    
    /**
     * 发现工具 - 改进的工具发现机制
     * @param task 任务描述
     * @return 工具名称
     */
    private String discoverTool(String task) {
        task = task.toLowerCase().trim();
        
        // 检查是否以工具名称开头
        if (task.startsWith("read ") || task.startsWith("读取 ")) {
            return "read";
        } else if (task.startsWith("write ") || task.startsWith("写入 ")) {
            return "write";
        } else if (task.startsWith("search ") || task.startsWith("搜索 ")) {
            return "search";
        } else if (task.startsWith("calculate ") || task.startsWith("计算 ")) {
            return "calculate";
        } else if (task.startsWith("web_search ") || task.startsWith("网页搜索 ")) {
            return "web_search";
        } else if (task.startsWith("task ") || task.startsWith("任务 ")) {
            return "task";
        }
        
        // 检查是否包含工具关键词
        if (task.contains("read file") || task.contains("读取文件")) {
            return "read";
        } else if (task.contains("write to file") || task.contains("写入文件")) {
            return "write";
        } else if (task.contains("search for") || task.contains("搜索")) {
            return "search";
        } else if (task.contains("calculate") || task.contains("计算") || task.contains("math")) {
            return "calculate";
        } else if (task.contains("web search") || task.contains("网页搜索") || task.contains("internet search")) {
            return "web_search";
        } else if (task.contains("launch task") || task.contains("execute task") || task.contains("run task")) {
            return "task";
        }

        return null;
    }
    
    /**
     * 检查是否使用了被禁止的命令
     * @param task 任务描述
     * @return 被禁止的命令名称，如果没有则返回null
     */
    private String checkForbiddenCommands(String task) {
        String lowerTask = task.toLowerCase();
        for (String forbidden : FORBIDDEN_COMMANDS) {
            if (lowerTask.contains(forbidden)) {
                return forbidden;
            }
        }
        return null;
    }
    
    /**
     * 验证输入 - Zod Schema风格的输入验证
     * @param task 任务描述
     * @return 是否验证通过
     */
    private boolean validateInput(String task) {
        // 基本验证
        if (task == null || task.trim().isEmpty()) {
            return false;
        }
        
        // 长度限制
        if (task.length() > configManager.getToolEngineMaxTaskLength()) {
            return false;
        }
        
        // 基于Claude Code的uJ1函数实现命令注入检测
        if (detectCommandInjection(task)) {
            logger.error("tengu_command_injection_detected: Command injection detected in task: {}", task);
            return false;
        }
        
        // 安全管理器验证
        return securityManager.validateInput(task);
    }
    
    /**
     * 基于Claude Code的uJ1函数实现命令注入检测
     * @param command 命令字符串
     * @return 是否检测到命令注入
     */
    private boolean detectCommandInjection(String command) {
        // 检查命令替换模式
        if (command.contains("$(") && command.contains(")")) {
            return true; // 命令替换 $(...)
        }
        
        if (command.contains("`") && command.lastIndexOf("`") > command.indexOf("`")) {
            return true; // 反引号命令替换
        }
        
        // 检查命令链接模式
        if (command.contains("&&") || command.contains("||") || 
            command.contains(";") || command.contains("|")) {
            // 允许一些安全的管道操作，但检查危险组合
            if (command.contains("$(curl") || command.contains("$(wget") || 
                command.contains("`curl") || command.contains("`wget")) {
                return true; // 网络命令注入
            }
        }
        
        // 检查重定向攻击
        if (command.contains(">") || command.contains(">>") || command.contains("<")) {
            // 检查是否尝试写入敏感文件
            if (command.contains("/etc/") || command.contains("/root/") || 
                command.contains("/home/") || command.contains(".ssh")) {
                return true; // 敏感文件操作
            }
        }
        
        // 检查特殊字符
        if (command.contains("#") && command.contains("test(")) {
            return true; // 注释注入
        }
        
        // 检查危险命令组合
        String[] dangerousPatterns = {
            "rm -rf", "chmod 777", "chown root", "sudo", 
            "wget -O", "curl -o", "nc -e", "netcat -e"
        };
        
        String lowerCommand = command.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerCommand.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查权限 - 改进的权限检查机制
     * @param toolName 工具名称
     * @param task 任务描述
     * @return 是否有权限
     */
    private boolean checkPermissions(String toolName, String task) {
        // 安全管理器权限检查
        if (!securityManager.checkPermissions(toolName)) {
            return false;
        }
        
        // 特定工具的权限检查
        switch (toolName) {
            case "write":
                // 写入操作需要额外确认
                return checkWritePermissions(task);
            case "read":
                // 读取操作检查文件路径
                return checkReadPermissions(task);
            default:
                return true;
        }
    }
    
    /**
     * 检查写入权限
     * @param task 任务描述
     * @return 是否有写入权限
     */
    private boolean checkWritePermissions(String task) {
        // 检查是否尝试写入系统文件
        String lowerTask = task.toLowerCase();
        if (lowerTask.contains("/etc/") || lowerTask.contains("/root/") || 
            lowerTask.contains("/bin/") || lowerTask.contains("/sbin/")) {
            logger.warn("tengu_permission_denied: Attempt to write to system file denied");
            return false;
        }
        
        // 其他写入权限检查可以在这里添加
        return true;
    }
    
    /**
     * 检查读取权限
     * @param task 任务描述
     * @return 是否有读取权限
     */
    private boolean checkReadPermissions(String task) {
        // 可以添加读取权限检查逻辑
        return true;
    }
    
    /**
     * 检查是否已取消
     * @return 是否已取消
     */
    private boolean isCancelled() {
        // 可以实现更复杂的取消检查逻辑
        return false;
    }
    
    /**
     * 格式化结果 - 改进的结果格式化
     * @param result 执行结果
     * @return 格式化后的结果
     */
    private String formatResult(String result) {
        // 清理结果中的敏感信息
        if (result != null) {
            // 移除可能的敏感信息
            result = result.replaceAll("/etc/[^\\s]*", "/etc/[REDACTED]")
                          .replaceAll("/root/[^\\s]*", "/root/[REDACTED]")
                          .replaceAll("/home/[^\\s]*", "/home/[REDACTED]")
                          .replaceAll("/bin/[^\\s]*", "/bin/[REDACTED]")
                          .replaceAll("/sbin/[^\\s]*", "/sbin/[REDACTED]")
                          .replaceAll("\\.ssh/[^\\s]*", ".ssh/[REDACTED]")
                          .replaceAll("password[\\s]*[:=][\\s]*[^\n\r]*", "password: [REDACTED]")
                          .replaceAll("secret[\\s]*[:=][\\s]*[^\n\r]*", "secret: [REDACTED]")
                          .replaceAll("token[\\s]*[:=][\\s]*[^\n\r]*", "token: [REDACTED]")
                          .replaceAll("key[\\s]*[:=][\\s]*[^\n\r]*", "key: [REDACTED]");
        }
        
        return "Tool execution result: " + result;
    }
    
    /**
     * 注册新工具
     * @param name 工具名称
     * @param executor 工具执行器
     */
    public void registerTool(String name, ToolExecutor executor) {
        registeredTools.put(name, executor);
    }
    
    /**
     * 获取已注册的工具列表
     * @return 工具名称列表
     */
    public List<String> getRegisteredTools() {
        return new ArrayList<>(registeredTools.keySet());
    }
    
    /**
     * 基于Claude Code的AU2函数实现 - 设置执行上下文
     * @param key 上下文键
     * @param value 上下文值
     */
    public void setExecutionContext(String key, Object value) {
        executionContext.put(key, value);
        logger.info("tengu_context_updated: Execution context updated with key: {}", key);
    }
    
    /**
     * 基于Claude Code的AU2函数实现 - 获取执行上下文
     * @param key 上下文键
     * @return 上下文值
     */
    public Object getExecutionContext(String key) {
        return executionContext.get(key);
    }
    
    /**
     * 基于Claude Code的AU2函数实现 - 获取所有执行上下文
     * @return 所有上下文的副本
     */
    public Map<String, Object> getAllExecutionContext() {
        return new HashMap<>(executionContext);
    }
    
    /**
     * 基于Claude Code的AU2函数实现 - 清除执行上下文
     */
    public void clearExecutionContext() {
        executionContext.clear();
        logger.info("tengu_context_cleared: Execution context cleared");
    }
    
    /**
     * 基于Claude Code的AU2函数实现 - 上下文感知的工具执行
     * @param task 任务描述
     * @param maxRetries 最大重试次数
     * @return 执行结果
     */
    public String executeToolWithContextAwareness(String task, int maxRetries) {
        // 记录上下文信息
        logger.info("tengu_context_aware_execution: Starting context-aware execution with context: {}", executionContext);
        
        // 根据上下文调整任务
        String adjustedTask = adjustTaskBasedOnContext(task);
        
        // 执行工具
        String result = executeToolWithRetry(adjustedTask, maxRetries);
        
        // 根据结果更新上下文
        updateContextBasedOnResult(result);
        
        return result;
    }
    
    /**
     * 基于Claude Code的AU2函数实现 - 根据上下文调整任务
     * @param task 原始任务
     * @return 调整后的任务
     */
    private String adjustTaskBasedOnContext(String task) {
        // 如果上下文中包含工作目录信息，则添加到任务中
        Object workingDir = executionContext.get("workingDirectory");
        if (workingDir != null && !task.contains("directory:")) {
            task = task + " (in directory: " + workingDir + ")";
        }
        
        // 如果上下文中包含用户偏好，则添加到任务中
        Object userPreference = executionContext.get("userPreference");
        if (userPreference != null) {
            task = task + " (with preference: " + userPreference + ")";
        }
        
        logger.info("tengu_task_adjusted: Task adjusted based on context: {}", task);
        return task;
    }
    
    /**
     * 基于Claude Code的AU2函数实现 - 根据结果更新上下文
     * @param result 执行结果
     */
    private void updateContextBasedOnResult(String result) {
        // 这里可以添加根据执行结果更新上下文的逻辑
        // 例如，如果执行了文件读取操作，可以更新最近访问的文件列表
        if (result != null && result.contains("File content:")) {
            executionContext.put("lastFileAccessed", System.currentTimeMillis());
            logger.info("tengu_context_updated: Updated last file access time in context");
        }
    }
}