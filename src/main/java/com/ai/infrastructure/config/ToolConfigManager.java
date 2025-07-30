package com.ai.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 工具配置管理器
 * 统一管理所有工具的配置参数
 */
public class ToolConfigManager {
    private static ToolConfigManager instance;
    private JsonObject config;
    
    private ToolConfigManager() {
        loadConfig();
    }
    
    public static synchronized ToolConfigManager getInstance() {
        if (instance == null) {
            instance = new ToolConfigManager();
        }
        return instance;
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try {
            // 从classpath加载配置文件
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("tool-config.json");
            if (inputStream == null) {
                throw new RuntimeException("Cannot find tool-config.json in classpath");
            }
            
            // 读取配置文件内容
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            
            // 解析JSON配置
            config = JsonParser.parseString(content.toString()).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tool configuration", e);
        }
    }
    
    /**
     * 获取ReadTool配置
     */
    public JsonObject getReadToolConfig() {
        return config.getAsJsonObject("readTool");
    }
    
    /**
     * 获取MemoryManager配置
     */
    public JsonObject getMemoryManagerConfig() {
        return config.getAsJsonObject("memoryManager");
    }
    
    /**
     * 获取SecurityManager配置
     */
    public JsonObject getSecurityManagerConfig() {
        return config.getAsJsonObject("securityManager");
    }
    
    /**
     * 获取MainAgent配置
     */
    public JsonObject getMainAgentConfig() {
        return config.getAsJsonObject("mainAgent");
    }
    
    /**
     * 获取ToolEngine配置
     */
    public JsonObject getToolEngineConfig() {
        return config.getAsJsonObject("toolEngine");
    }
    
    /**
     * 获取WebSearchTool配置
     */
    public JsonObject getWebSearchToolConfig() {
        return config.getAsJsonObject("webSearchTool");
    }
    
    /**
     * 获取ReadTool的最大HTML内容长度
     */
    public int getReadToolMaxHtmlContentLength() {
        return getReadToolConfig().get("maxHtmlContentLength").getAsInt();
    }
    
    /**
     * 获取ReadTool的最大文本内容长度
     */
    public int getReadToolMaxTextContentLength() {
        return getReadToolConfig().get("maxTextContentLength").getAsInt();
    }
    
    /**
     * 获取ReadTool的最大PDF内容长度
     */
    public int getReadToolMaxPdfContentLength() {
        return getReadToolConfig().get("maxPdfContentLength").getAsInt();
    }
    
    /**
     * 获取ReadTool的最大Excel行数
     */
    public int getReadToolMaxExcelRows() {
        return getReadToolConfig().get("maxExcelRows").getAsInt();
    }
    
    /**
     * 获取ReadTool的截断消息
     */
    public String getReadToolTruncateMessage() {
        return getReadToolConfig().get("truncateMessage").getAsString();
    }
    
    /**
     * 获取ReadTool的HTML截断消息
     */
    public String getReadToolHtmlTruncateMessage() {
        return getReadToolConfig().get("htmlTruncateMessage").getAsString();
    }
    
    /**
     * 获取ReadTool的Excel截断消息
     */
    public String getReadToolExcelTruncateMessage() {
        return getReadToolConfig().get("excelTruncateMessage").getAsString();
    }
    
    /**
     * 获取ReadTool错误信息中的最大原始HTML长度
     */
    public int getReadToolMaxRawHtmlForError() {
        return getReadToolConfig().get("maxRawHtmlForError").getAsInt();
    }
    
    /**
     * 获取MemoryManager的最大输入长度
     */
    public int getMemoryManagerMaxInputLength() {
        return getMemoryManagerConfig().get("maxInputLength").getAsInt();
    }
    
    /**
     * 获取MemoryManager的最大输出长度
     */
    public int getMemoryManagerMaxOutputLength() {
        return getMemoryManagerConfig().get("maxOutputLength").getAsInt();
    }
    
    /**
     * 获取MemoryManager的最小单词长度过滤值
     */
    public int getMemoryManagerMinWordLengthForFiltering() {
        return getMemoryManagerConfig().get("minWordLengthForFiltering").getAsInt();
    }
    
    /**
     * 获取SecurityManager的最大输入长度
     */
    public int getSecurityManagerMaxInputLength() {
        return getSecurityManagerConfig().get("maxInputLength").getAsInt();
    }
    
    /**
     * 获取MainAgent的最大任务长度
     */
    public int getMainAgentMaxTaskLength() {
        return getMainAgentConfig().get("maxTaskLength").getAsInt();
    }
    
    /**
     * 获取ToolEngine的最大任务长度
     */
    public int getToolEngineMaxTaskLength() {
        return getToolEngineConfig().get("maxTaskLength").getAsInt();
    }
    
    /**
     * 获取WebSearchTool的最大结果数
     */
    public int getWebSearchToolMaxResults() {
        return getWebSearchToolConfig().get("maxResults").getAsInt();
    }
    
    /**
     * 获取BashTool的最大命令长度
     */
    public int getBashToolMaxCommandLength() {
        return getBashToolConfig().get("maxCommandLength").getAsInt();
    }
    
    /**
     * 获取BashTool的最大输出长度
     */
    public int getBashToolMaxOutputLength() {
        return getBashToolConfig().get("maxOutputLength").getAsInt();
    }
    
    /**
     * 获取BashTool的最大错误输出长度
     */
    public int getBashToolMaxErrorOutputLength() {
        return getBashToolConfig().get("maxErrorOutputLength").getAsInt();
    }
    
    /**
     * 获取BashTool的默认超时时间
     */
    public int getBashToolDefaultTimeout() {
        return getBashToolConfig().get("defaultTimeout").getAsInt();
    }
    
    /**
     * 获取BashTool是否强制白名单
     */
    public boolean getBashToolEnforceWhitelist() {
        return getBashToolConfig().get("enforceWhitelist").getAsBoolean();
    }
    
    /**
     * 获取BashTool配置
     */
    public JsonObject getBashToolConfig() {
        return config.getAsJsonObject("bashTool");
    }
}