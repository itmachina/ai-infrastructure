package com.ai.infrastructure.tools;

import com.ai.infrastructure.security.SecurityManager;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * 工具引擎，实现6阶段执行流程
 * 基于Claude Code的工具执行机制实现完整的安全检查和执行流程
 */
public class ToolEngine {
    private Map<String, ToolExecutor> registeredTools;
    private SecurityManager securityManager;
    
    // 工具替代强制机制 - 禁止使用的传统命令
    private static final String[] FORBIDDEN_COMMANDS = {"find", "grep", "cat", "head", "tail", "ls"};
    
    public ToolEngine() {
        this.registeredTools = new HashMap<>();
        this.securityManager = new SecurityManager();
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
    }
    
    /**
     * 执行工具 - 实现完整的6阶段执行流程
     * @param task 任务描述
     * @return 执行结果
     */
    public String executeTool(String task) {
        try {
            // 阶段1: 工具发现与验证
            String toolName = discoverTool(task);
            if (toolName == null || !registeredTools.containsKey(toolName)) {
                // 检查是否尝试使用被禁止的命令
                String forbiddenCommand = checkForbiddenCommands(task);
                if (forbiddenCommand != null) {
                    return "Error: Command '" + forbiddenCommand + "' is not allowed. Please use the appropriate tool instead.";
                }
                return "Unknown tool for task: " + task;
            }
            
            // 阶段2: 输入验证 (Zod Schema风格验证)
            if (!validateInput(task)) {
                return "Input validation failed for task: " + task;
            }
            
            // 阶段3: 权限检查与门控
            if (!checkPermissions(toolName, task)) {
                return "Permission denied for tool: " + toolName;
            }
            
            // 阶段4: 取消检查 (AbortController风格)
            if (isCancelled()) {
                return "Task was cancelled";
            }
            
            // 阶段5: 工具执行
            ToolExecutor executor = registeredTools.get(toolName);
            String result = executor.execute(task);
            
            // 阶段6: 结果格式化与清理
            return formatResult(result);
            
        } catch (Exception e) {
            // 记录工具执行错误
            System.err.println("tengu_tool_execution_error: Tool execution failed - " + e.getMessage());
            return "Error executing tool: " + e.getMessage();
        }
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
        if (task.length() > 10000) {
            return false;
        }
        
        // 基于Claude Code的uJ1函数实现命令注入检测
        if (detectCommandInjection(task)) {
            System.err.println("tengu_command_injection_detected: Command injection detected in task: " + task);
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
            "wget http", "curl http", "nc ", "netcat"
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
            System.err.println("tengu_permission_denied: Attempt to write to system file denied");
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
}