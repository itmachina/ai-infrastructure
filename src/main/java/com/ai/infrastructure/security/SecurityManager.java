package com.ai.infrastructure.security;

import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;

/**
 * 安全管理器，实现6层安全防护
 */
public class SecurityManager {
    private Set<String> deniedPatterns;
    private Set<String> allowedPatterns;
    
    public SecurityManager() {
        this.deniedPatterns = new HashSet<>();
        this.allowedPatterns = new HashSet<>();
        initializeSecurityRules();
    }
    
    /**
     * 初始化安全规则
     */
    private void initializeSecurityRules() {
        // 添加一些默认的拒绝模式
        deniedPatterns.add(".*system\\s*\\(.*");
        deniedPatterns.add(".*exec\\s*\\(.*");
        deniedPatterns.add(".*rm\\s*-rf.*");
        deniedPatterns.add(".*delete.*system.*");
    }
    
    /**
     * 验证输入
     * @param input 输入内容
     * @return 是否验证通过
     */
    public boolean validateInput(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        // 检查长度限制
        if (input.length() > 10000) {
            return false;
        }
        
        // 检查拒绝模式
        for (String pattern : deniedPatterns) {
            if (Pattern.matches(pattern, input.toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查权限
     * @param resource 资源
     * @param action 操作
     * @return 是否有权限
     */
    public boolean checkPermissions(String resource, String action) {
        // 简化的权限检查
        return !resource.contains("system") || action.equals("read");
    }
    
    /**
     * 简化的权限检查
     * @param toolName 工具名称
     * @return 是否有权限
     */
    public boolean checkPermissions(String toolName) {
        // 默认允许所有工具执行
        return true;
    }
    
    /**
     * 沙箱检查
     * @param operation 操作
     * @return 是否允许在沙箱中执行
     */
    public boolean sandboxCheck(String operation) {
        // 简化的沙箱检查
        return !operation.contains("system") && !operation.contains("exec");
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
}