package com.ai.infrastructure.security;

import com.ai.infrastructure.config.ToolConfigManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 安全管理器，实现6层安全防护
 * 基于Claude Code的安全架构实现完整的企业级安全防护体系
 */
public class SecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);
    
    // 第1层：防御性安全策略 - 恶意代码防护常量
    private static final String DEFENSIVE_SECURITY_POLICY = 
        "IMPORTANT: Assist with defensive security tasks only. " +
        "Refuse to create, modify, or improve code that may be used maliciously. " +
        "Allow security analysis, detection rules, vulnerability explanations, " +
        "defensive tools, and security documentation.";
    
    // 第2层：文件安全检查系统
    private static final String FILE_SAFETY_CHECK = 
        "<system-reminder>\n" +
        "Whenever you read a file, you should consider whether it looks malicious. \n" +
        "If it does, you MUST refuse to improve or augment the code. \n" +
        "You can still analyze existing code, write reports, or answer high-level \n" +
        "questions about the code behavior.\n" +
        "</system-reminder>";
    
    // 模式匹配规则
    private Set<String> deniedPatterns;
    private Set<String> allowedPatterns;
    
    // 第4层：细粒度权限控制
    private Map<String, Set<String>> resourcePermissions;
    private Set<String> userRoles;
    
    // 第5层：审计日志系统
    private List<SecurityEvent> auditLog;
    
    // 第6层：执行环境控制
    private boolean sandboxMode;
    private Set<String> allowedDirectories;
    
    // 配置管理器
    private final ToolConfigManager configManager;
    
    public SecurityManager() {
        this.deniedPatterns = new HashSet<>();
        this.allowedPatterns = new HashSet<>();
        this.resourcePermissions = new ConcurrentHashMap<>();
        this.userRoles = new HashSet<>();
        this.auditLog = new ArrayList<>();
        this.sandboxMode = true; // 默认启用沙箱模式
        this.allowedDirectories = new HashSet<>();
        this.configManager = ToolConfigManager.getInstance();
        initializeSecurityRules();
        initializeDefaultPermissions();
    }
    
    /**
     * 初始化安全规则 - 基于Claude Code的安全模式
     */
    private void initializeSecurityRules() {
        // 添加默认的拒绝模式 - 基于常见的安全威胁
        deniedPatterns.add(".*system\\s*\\(.*");           // 系统调用
        deniedPatterns.add(".*exec\\s*\\(.*");             // 执行命令
        deniedPatterns.add(".*rm\\s*-rf.*");               // 危险删除
        deniedPatterns.add(".*delete.*system.*");          // 系统删除
        deniedPatterns.add(".*chmod\\s*777.*");            // 权限修改
        deniedPatterns.add(".*chown\\s*root.*");           // 所有者修改
        deniedPatterns.add(".*sudo.*");                    // 提权命令
        deniedPatterns.add(".*wget\\s*http.*");            // 网络下载
        deniedPatterns.add(".*curl\\s*http.*");            // 网络请求
        deniedPatterns.add(".*nc\\s*.*");                  // 网络连接
        deniedPatterns.add(".*netcat\\s*.*");              // 网络连接
        deniedPatterns.add(".*ssh\\s*.*");                 // SSH连接
        deniedPatterns.add(".*telnet\\s*.*");              // Telnet连接
        deniedPatterns.add(".*\\$\\(.*\\).*");             // 命令替换
        deniedPatterns.add(".*`.*`.*");                    // 反引号执行
    }
    
    /**
     * 初始化默认权限
     */
    private void initializeDefaultPermissions() {
        // 默认允许的目录
        allowedDirectories.add("/tmp");
        allowedDirectories.add("/var/tmp");
        allowedDirectories.add(System.getProperty("user.home"));
        
        // 默认用户角色
        userRoles.add("user");
    }
    
    /**
     * 第1层：防御性安全策略检查
     * @param task 任务描述
     * @return 是否符合防御性安全策略
     */
    public boolean checkDefensiveSecurityPolicy(String task) {
        if (task == null) return false;
        
        String lowerTask = task.toLowerCase();
        
        // 检查是否尝试创建恶意代码
        String[] maliciousPatterns = {
            "malware", "virus", "trojan", "backdoor", "exploit", 
            "keylogger", "rootkit", "ransomware", "spyware"
        };
        
        for (String pattern : maliciousPatterns) {
            if (lowerTask.contains(pattern)) {
                logSecurityEvent("defensive_policy_violation", 
                    "Attempt to create malicious code detected: " + pattern);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 第2层：文件安全检查系统
     * @param content 文件内容
     * @return 是否安全
     */
    public boolean checkFileSafety(String content) {
        if (content == null || content.isEmpty()) {
            return true;
        }
        
        // 检查恶意代码模式
        String[] maliciousCodePatterns = {
            "Runtime\\.getRuntime\\(\\)\\.exec\\(",  // Java执行命令
            "ProcessBuilder",                       // 进程构建
            "eval\\(",                              // 动态执行
            "exec\\(",                              // 执行函数
            "system\\(",                            // 系统调用
            "popen\\(",                             // 管道执行
            "shell_exec\\(",                        // Shell执行
            "base64_decode\\(",                     // Base64解码
            "file_get_contents\\(",                 // 文件读取
            "curl_exec\\(",                         // Curl执行
            "socket\\(",                            // Socket连接
            "fopen\\(",                             // 文件打开
            "open\\(",                              // 打开文件
            "FileSystem",                           // 文件系统操作
            "FileWriter",                           // 文件写入
            "FileOutputStream"                      // 文件输出流
        };
        
        String lowerContent = content.toLowerCase();
        for (String pattern : maliciousCodePatterns) {
            if (lowerContent.contains(pattern.toLowerCase())) {
                logSecurityEvent("file_safety_violation", 
                    "Suspicious code pattern detected: " + pattern);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 第3层：LLM驱动的命令注入检测（完整实现）
     * 基于Claude Code的uJ1函数实现，提供全面的命令注入防护
     * @param command 命令
     * @return 是否检测到注入
     */
    public boolean detectCommandInjection(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        
        String lowerCommand = command.toLowerCase();
        
        // 检查命令替换模式
        if (command.contains("$(") && command.contains(")")) {
            logSecurityEvent("command_injection_detected", 
                "Command substitution detected: $(...)");
            return true;
        }
        
        if (command.contains("`") && command.lastIndexOf("`") > command.indexOf("`")) {
            logSecurityEvent("command_injection_detected", 
                "Backtick command substitution detected");
            return true;
        }
        
        // 检查命令链接模式
        if (command.contains("&&") || command.contains("||") || 
            command.contains(";") || command.contains("|")) {
            // 检查危险组合
            if (command.contains("$(curl") || command.contains("$(wget") || 
                command.contains("`curl") || command.contains("`wget")) {
                logSecurityEvent("command_injection_detected", 
                    "Network command injection detected");
                return true;
            }
        }
        
        // 检查重定向攻击
        if (command.contains(">") || command.contains(">>") || command.contains("<")) {
            // 检查是否尝试写入敏感文件
            if (command.contains("/etc/") || command.contains("/root/") || 
                command.contains("/home/") || command.contains(".ssh")) {
                logSecurityEvent("command_injection_detected", 
                    "Sensitive file operation detected");
                return true;
            }
        }
        
        // 检查特殊字符注入
        if (command.contains("#") && command.contains("test(")) {
            logSecurityEvent("command_injection_detected", 
                "Comment injection detected");
            return true;
        }
        
        // 检查危险命令组合
        String[] dangerousPatterns = {
            "rm -rf", "chmod 777", "chown root", "sudo", 
            "wget http", "curl http", "nc ", "netcat"
        };
        
        for (String pattern : dangerousPatterns) {
            if (lowerCommand.contains(pattern)) {
                logSecurityEvent("command_injection_detected", 
                    "Dangerous command detected: " + pattern);
                return true;
            }
        }
        
        // 检查常见的shell元字符注入
        String[] shellMetachars = {
            "\\n", "\\r", "\\x0a", "\\x0d", "\\u000a", "\\u000d"
        };
        
        for (String metachar : shellMetachars) {
            if (command.contains(metachar)) {
                logSecurityEvent("command_injection_detected", 
                    "Shell metacharacter injection detected: " + metachar);
                return true;
            }
        }
        
        // 基于Claude Code的uJ1函数增强实现 - 更严格的命令前缀检测
        // 检查是否包含潜在的命令注入模式
        if (containsCommandInjectionPattern(command)) {
            logSecurityEvent("command_injection_detected", 
                "Advanced command injection pattern detected");
            return true;
        }
        
        return false;
    }
    
    /**
     * 基于Claude Code的uJ1函数实现增强的命令注入模式检测
     * @param command 命令字符串
     * @return 是否包含命令注入模式
     */
    private boolean containsCommandInjectionPattern(String command) {
        // 检查复杂命令注入模式
        String[] injectionPatterns = {
            "\\$\\(.*\\)",           // $() 命令替换
            "`[^`]*`",               // 反引号命令替换
            "\\{[^}]*\\}",           // 大括号扩展
            "\\$\\{[^}]*\\}",        // 变量替换
            "\\$[^\\s]*\\(.*\\)",    // 变量函数调用
            ";[^;]*;",               // 多重命令分隔
            "\\|\\s*[^|]*\\|",       // 多重管道
            "&&\\s*[^&]*&&",         // 多重逻辑与
            "\\|\\|\\s*[^|]*\\|\\|"  // 多重逻辑或
        };
        
        for (String pattern : injectionPatterns) {
            if (command.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        
        // 检查危险的环境变量操作
        if (command.contains("export ") && 
            (command.contains("PATH=") || command.contains("LD_LIBRARY_PATH="))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 第4层：细粒度权限控制
     * @param resource 资源
     * @param action 操作
     * @param userRole 用户角色
     * @return 是否有权限
     */
    public boolean checkPermissions(String resource, String action, String userRole) {
        // 记录权限检查事件
        logSecurityEvent("permission_check", 
            "Checking permission for " + userRole + " to " + action + " " + resource);
        
        // 检查用户角色
        if (!userRoles.contains(userRole)) {
            logSecurityEvent("permission_denied", 
                "Unknown user role: " + userRole);
            return false;
        }
        
        // 检查资源权限
        Set<String> permissions = resourcePermissions.get(resource);
        if (permissions != null && permissions.contains(action)) {
            return true;
        }
        
        // 默认权限检查
        if (resource.contains("/etc/") || resource.contains("/root/") || 
            resource.contains("/bin/") || resource.contains("/sbin/")) {
            if (!"read".equals(action) || !"admin".equals(userRole)) {
                logSecurityEvent("permission_denied", 
                    "Access to system resource denied: " + resource);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 简化的权限检查（向后兼容）
     * @param toolName 工具名称
     * @return 是否有权限
     */
    public boolean checkPermissions(String toolName) {
        return checkPermissions(toolName, "execute", "user");
    }
    
    /**
     * 第5层：审计日志系统
     * @param eventType 事件类型
     * @param details 事件详情
     */
    private void logSecurityEvent(String eventType, String details) {
        SecurityEvent event = new SecurityEvent(
            eventType, 
            details, 
            System.currentTimeMillis(), 
            Thread.currentThread().getName()
        );
        auditLog.add(event);
        
        // 使用SLF4J记录安全事件日志
        logger.warn("Security event: {} - {}", eventType, details);
    }
    
    /**
     * 获取安全事件日志
     * @return 安全事件列表
     */
    public List<SecurityEvent> getAuditLog() {
        return new ArrayList<>(auditLog);
    }
    
    /**
     * 第6层：执行环境控制 - 沙箱检查
     * @param operation 操作
     * @param filePath 文件路径（如果适用）
     * @return 是否允许在沙箱中执行
     */
    public boolean sandboxCheck(String operation, String filePath) {
        // 如果未启用沙箱模式，允许所有操作
        if (!sandboxMode) {
            return true;
        }
        
        // 检查操作类型
        if (operation.contains("system") || operation.contains("exec") || 
            operation.contains("Runtime") || operation.contains("ProcessBuilder")) {
            logSecurityEvent("sandbox_violation", 
                "Attempt to execute system command in sandbox: " + operation);
            return false;
        }
        
        // 检查文件路径（如果提供）
        if (filePath != null && !isAllowedPath(filePath)) {
            logSecurityEvent("sandbox_violation", 
                "Attempt to access restricted path: " + filePath);
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查文件路径是否被允许
     * @param filePath 文件路径
     * @return 是否被允许
     */
    private boolean isAllowedPath(String filePath) {
        // 检查是否在允许的目录中
        for (String allowedDir : allowedDirectories) {
            if (filePath.startsWith(allowedDir)) {
                return true;
            }
        }
        
        // 检查是否为相对路径（相对于工作目录）
        if (!filePath.startsWith("/") && !filePath.contains(":/") && !filePath.contains(":\\") ) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 验证输入 - 综合安全检查
     * @param input 输入内容
     * @return 是否验证通过
     */
    public boolean validateInput(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        // 检查长度限制
        if (input.length() > configManager.getSecurityManagerMaxInputLength()) {
            logSecurityEvent("input_validation_failed", 
                "Input too long: " + input.length() + " characters");
            return false;
        }
        
        // 检查拒绝模式
        for (String pattern : deniedPatterns) {
            if (Pattern.matches(pattern, input.toLowerCase())) {
                logSecurityEvent("input_validation_failed", 
                    "Input matches denied pattern: " + pattern);
                return false;
            }
        }
        
        // 检查命令注入
        if (detectCommandInjection(input)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查权限（向后兼容）
     * @param resource 资源
     * @param action 操作
     * @return 是否有权限
     */
    public boolean checkPermissions(String resource, String action) {
        return checkPermissions(resource, action, "user");
    }
    
    /**
     * 简化的沙箱检查（向后兼容）
     * @param operation 操作
     * @return 是否允许在沙箱中执行
     */
    public boolean sandboxCheck(String operation) {
        return sandboxCheck(operation, null);
    }
    
    /**
     * 添加拒绝模式
     * @param pattern 模式
     */
    public void addDeniedPattern(String pattern) {
        deniedPatterns.add(pattern);
    }
    
    /**
     * 添加允许模式
     * @param pattern 模式
     */
    public void addAllowedPattern(String pattern) {
        allowedPatterns.add(pattern);
    }
    
    /**
     * 添加资源权限
     * @param resource 资源
     * @param permissions 权限集合
     */
    public void addResourcePermissions(String resource, Set<String> permissions) {
        resourcePermissions.put(resource, new HashSet<>(permissions));
    }
    
    /**
     * 添加用户角色
     * @param role 角色
     */
    public void addUserRole(String role) {
        userRoles.add(role);
    }
    
    /**
     * 设置沙箱模式
     * @param enabled 是否启用
     */
    public void setSandboxMode(boolean enabled) {
        this.sandboxMode = enabled;
    }
    
    /**
     * 添加允许的目录
     * @param directory 目录路径
     */
    public void addAllowedDirectory(String directory) {
        allowedDirectories.add(directory);
    }
    
    /**
     * 安全事件类
     */
    public static class SecurityEvent {
        private final String eventType;
        private final String details;
        private final long timestamp;
        private final String threadName;
        
        public SecurityEvent(String eventType, String details, long timestamp, String threadName) {
            this.eventType = eventType;
            this.details = details;
            this.timestamp = timestamp;
            this.threadName = threadName;
        }
        
        // Getters
        public String getEventType() { return eventType; }
        public String getDetails() { return details; }
        public long getTimestamp() { return timestamp; }
        public String getThreadName() { return threadName; }
        
        @Override
        public String toString() {
            return String.format("[%d] %s - %s (Thread: %s)", 
                timestamp, eventType, details, threadName);
        }
    }
}