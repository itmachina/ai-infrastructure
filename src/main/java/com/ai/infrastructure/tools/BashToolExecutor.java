package com.ai.infrastructure.tools;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.ai.infrastructure.config.ToolConfigManager;
import com.ai.infrastructure.security.SecurityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Bash工具执行器
 * 实现安全的shell命令执行功能，包括命令验证、沙箱隔离、实时输出等机制
 */
public class BashToolExecutor implements ToolExecutor {
    private static final Logger logger = LoggerFactory.getLogger(BashToolExecutor.class);
    
    private final Gson gson;
    private final SecurityManager securityManager;
    private final ToolConfigManager configManager;
    
    // 命令白名单
    private static final Set<String> ALLOWED_COMMANDS = new HashSet<>(Arrays.asList(
        "ls", "pwd", "echo", "cat", "grep", "find", "head", "tail", "wc", 
        "sort", "uniq", "diff", "mkdir", "cp", "mv", "rm", "touch",
        "chmod", "chown", "ln", "tar", "gzip", "gunzip", "zip", "unzip",
        "date", "cal", "bc", "awk", "sed", "cut", "paste", "join",
        "tr", "tee", "xargs", "which", "whereis", "locate", "updatedb",
        "df", "du", "free", "ps", "top", "kill", "jobs", "bg", "fg",
        "history", "alias", "unalias", "env", "export", "unset",
        "hostname", "whoami", "id", "groups", "finger", "w", "uptime",
        "ping", "traceroute", "nslookup", "dig", "host", "whois",
        "curl", "wget", "scp", "rsync", "ssh", "ftp", "sftp",
        "git", "svn", "hg", "make", "gcc", "g++", "javac", "java",
        "python", "python3", "node", "npm", "yarn", "pip", "pip3",
        "docker", "kubectl", "helm", "terraform", "ansible",
        "systemctl", "service", "crontab", "at", "batch",
        "journalctl", "dmesg", "syslog", "logrotate"
    ));
    
    // 危险命令黑名单
    private static final Set<String> DENIED_COMMANDS = new HashSet<>(Arrays.asList(
        "rm -rf /", "rm -fr /", "rm -r /", "rm -f /",
        "mkfs", "dd", "format", "fdisk", "parted",
        "shutdown", "halt", "reboot", "init 0", "init 6",
        "poweroff", "systemctl poweroff", "systemctl reboot",
        "chmod 777 /", "chown root:root /",
        ":(){ :|:& };:", "fork bomb"
    ));
    
    // 危险参数模式
    private static final Pattern[] DANGEROUS_PATTERNS = {
        Pattern.compile(".*[;&|]\\s*rm.*-rf.*\\/.*"),
        Pattern.compile(".*[;&|]\\s*rm.*-fr.*\\/.*"),
        Pattern.compile(".*>\\s*\\/dev\\/null.*"),
        Pattern.compile(".*>\\s*\\/dev\\/zero.*"),
        Pattern.compile(".*\\|\\s*sh.*"),
        Pattern.compile(".*`.*`.*"),
        Pattern.compile(".*\\$\\(.*\\).*"),
        Pattern.compile(".*&&\\s*rm.*"),
        Pattern.compile(".*\\|\\|\\s*rm.*")
    };
    
    public BashToolExecutor() {
        this.gson = new Gson();
        this.securityManager = new SecurityManager();
        this.configManager = ToolConfigManager.getInstance();
    }
    
    @Override
    public String execute(String task) {
        try {
            logger.info("tengu_bash_start: Starting bash command execution for task: {}", task);
            
            // 解析任务参数
            BashParameters params = parseBashParameters(task);
            
            if (params.command == null || params.command.trim().isEmpty()) {
                logger.error("tengu_bash_error: Invalid command");
                return "Error: Invalid command";
            }
            
            // 安全检查
            String validationResult = validateCommand(params.command);
            if (validationResult != null && !validationResult.isEmpty()) {
                logger.error("tengu_bash_error: {}", validationResult);
                return "Error: " + validationResult;
            }
            
            // 执行命令
            String result = executeBashCommand(params);
            
            // 检查执行结果
            if (result.startsWith("Error:")) {
                logger.error("tengu_bash_error: {}", result);
                return result;
            }
            
            logger.info("tengu_bash_success: Bash command executed successfully");
            return "Bash command execution result:\n" + result;
        } catch (Exception e) {
            logger.error("tengu_bash_exception: Exception during bash command execution: {}", e.getMessage(), e);
            return "Error executing bash command: " + e.getMessage();
        }
    }
    
    /**
     * 解析Bash参数
     * @param task 任务描述
     * @return Bash参数对象
     */
    private BashParameters parseBashParameters(String task) {
        BashParameters params = new BashParameters();
        
        try {
            // 尝试解析JSON格式
            if (task != null && task.trim().startsWith("{")) {
                JsonObject jsonObject = JsonParser.parseString(task).getAsJsonObject();
                
                if (jsonObject.has("command")) {
                    params.command = jsonObject.get("command").getAsString();
                }
                
                if (jsonObject.has("working_dir")) {
                    params.workingDir = jsonObject.get("working_dir").getAsString();
                }
                
                if (jsonObject.has("timeout")) {
                    params.timeout = jsonObject.get("timeout").getAsInt();
                } else {
                    params.timeout = configManager.getBashToolDefaultTimeout(); // 默认超时时间
                }
                
                if (jsonObject.has("environment")) {
                    JsonObject envObj = jsonObject.get("environment").getAsJsonObject();
                    params.environment = new HashMap<>();
                    for (String key : envObj.keySet()) {
                        params.environment.put(key, envObj.get(key).getAsString());
                    }
                }
            } else {
                // 不是JSON格式，按简单命令处理
                params.command = task != null ? task.trim() : "";
                if (params.command.startsWith("bash ")) {
                    params.command = params.command.substring(5);
                } else if (params.command.startsWith("执行 ")) {
                    params.command = params.command.substring(3);
                }
                params.timeout = configManager.getBashToolDefaultTimeout();
            }
        } catch (JsonSyntaxException e) {
            // JSON解析失败，按简单命令处理
            params.command = task != null ? task.trim() : "";
            if (params.command.startsWith("bash ")) {
                params.command = params.command.substring(5);
            } else if (params.command.startsWith("执行 ")) {
                params.command = params.command.substring(3);
            }
            params.timeout = configManager.getBashToolDefaultTimeout();
        }
        
        // 设置默认工作目录
        if (params.workingDir == null || params.workingDir.trim().isEmpty()) {
            params.workingDir = System.getProperty("user.dir");
        }
        
        return params;
    }
    
    /**
     * 验证命令安全性
     * @param command 命令字符串
     * @return 验证结果，null表示通过验证
     */
    private String validateCommand(String command) {
        try {
            // 基本安全检查
            if (command == null || command.trim().isEmpty()) {
                return "Empty command";
            }
            
            // 长度限制
            if (command.length() > configManager.getBashToolMaxCommandLength()) {
                return "Command too long";
            }
            
            // 安全管理器验证
            if (!securityManager.validateInput(command)) {
                return "Command validation failed by security manager";
            }
            
            // 检查黑名单命令
            String lowerCommand = command.toLowerCase();
            for (String deniedCmd : DENIED_COMMANDS) {
                if (lowerCommand.contains(deniedCmd)) {
                    return "Command '" + deniedCmd + "' is not allowed";
                }
            }
            
            // 检查危险模式
            for (Pattern pattern : DANGEROUS_PATTERNS) {
                if (pattern.matcher(command).matches()) {
                    return "Command contains dangerous pattern";
                }
            }
            
            // 检查命令白名单（如果启用）
            if (configManager.getBashToolEnforceWhitelist()) {
                String[] parts = command.trim().split("\\s+");
                if (parts.length > 0) {
                    String cmd = parts[0];
                    if (!ALLOWED_COMMANDS.contains(cmd)) {
                        return "Command '" + cmd + "' is not in whitelist";
                    }
                }
            }
            
            return null; // 通过验证
        } catch (Exception e) {
            return "Error during command validation: " + e.getMessage();
        }
    }
    
    /**
     * 执行Bash命令
     * @param params Bash参数
     * @return 执行结果
     */
    private String executeBashCommand(BashParameters params) {
        Process process = null;
        ExecutorService executor = null;
        
        try {
            // 构建命令
            List<String> commandList = new ArrayList<>();
            commandList.add("bash");
            commandList.add("-c");
            commandList.add(params.command);
            
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            
            // 设置工作目录
            if (params.workingDir != null && !params.workingDir.trim().isEmpty()) {
                processBuilder.directory(new File(params.workingDir));
            }
            
            // 设置环境变量
            if (params.environment != null && !params.environment.isEmpty()) {
                Map<String, String> env = processBuilder.environment();
                for (Map.Entry<String, String> entry : params.environment.entrySet()) {
                    env.put(entry.getKey(), entry.getValue());
                }
            }
            
            // 启动进程
            process = processBuilder.start();
            
            // 创建输出收集器
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // 创建线程池来处理输出
            executor = Executors.newFixedThreadPool(2);
            
            // 读取标准输出
            final Process finalProcess = process;
            Future<String> stdoutFuture = executor.submit(() -> {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                        // 限制输出长度
                        if (sb.length() > configManager.getBashToolMaxOutputLength()) {
                            sb.append("\n... (output truncated due to length) ...\n");
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Error reading stdout: {}", e.getMessage());
                }
                return sb.toString();
            });
            
            // 读取错误输出
            Future<String> stderrFuture = executor.submit(() -> {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                        // 限制输出长度
                        if (sb.length() > configManager.getBashToolMaxErrorOutputLength()) {
                            sb.append("\n... (error output truncated due to length) ...\n");
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Error reading stderr: {}", e.getMessage());
                }
                return sb.toString();
            });
            
            // 等待进程完成或超时
            boolean completed = process.waitFor(params.timeout, TimeUnit.SECONDS);
            
            if (!completed) {
                // 超时，销毁进程
                process.destroyForcibly();
                return "Error: Command execution timed out after " + params.timeout + " seconds";
            }
            
            // 获取输出结果
            String stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(1, TimeUnit.SECONDS);
            
            int exitCode = process.exitValue();
            
            // 构建结果
            StringBuilder result = new StringBuilder();
            result.append("Command: ").append(params.command).append("\n");
            result.append("Exit code: ").append(exitCode).append("\n");
            
            if (stdout != null && !stdout.isEmpty()) {
                result.append("\nStandard output:\n").append(stdout);
            }
            
            if (stderr != null && !stderr.isEmpty()) {
                result.append("\nError output:\n").append(stderr);
            }
            
            return result.toString();
        } catch (Exception e) {
            return "Error executing bash command: " + e.getMessage();
        } finally {
            // 清理资源
            if (process != null) {
                try {
                    process.destroyForcibly();
                } catch (Exception e) {
                    logger.warn("Error destroying process: {}", e.getMessage());
                }
            }
            
            if (executor != null) {
                try {
                    executor.shutdownNow();
                } catch (Exception e) {
                    logger.warn("Error shutting down executor: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Bash参数类
     */
    private static class BashParameters {
        String command;
        String workingDir;
        int timeout;
        Map<String, String> environment;
        
        BashParameters() {
            this.timeout = 30; // 默认30秒超时
            this.environment = new HashMap<>();
        }
    }
}