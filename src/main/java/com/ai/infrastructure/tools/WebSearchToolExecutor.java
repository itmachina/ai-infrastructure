package com.ai.infrastructure.tools;

import com.ai.infrastructure.config.ToolConfigManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

/**
 * 网页搜索工具执行器
 * 支持必应搜索功能
 */
public class WebSearchToolExecutor implements ToolExecutor {
    private static final String SEARCH_API_URL = "https://www.bing.com/search";
    
    private final Gson gson;
    private final ToolConfigManager configManager;
    
    public WebSearchToolExecutor() {
        this.gson = new Gson();
        this.configManager = ToolConfigManager.getInstance();
    }
    
    @Override
    public String execute(String task) {
        try {
            // 解析搜索查询
            String query = extractSearchQuery(task);
            
            if (query == null || query.trim().isEmpty()) {
                System.err.println("tengu_web_search_error: Invalid search query");
                return "Error: Invalid search query";
            }
            
            System.out.println("tengu_web_search_start: Starting web search for query: " + query);
            
            // 执行搜索
            String searchResults = performWebSearch(query);
            
            // 检查搜索结果
            if (searchResults.startsWith("Error:")) {
                System.err.println("tengu_web_search_error: " + searchResults);
                return searchResults;
            }
            
            System.out.println("tengu_web_search_success: Web search completed successfully");
            return "Web search results for '" + query + "':\n" + searchResults;
        } catch (Exception e) {
            System.err.println("tengu_web_search_exception: Exception during web search: " + e.getMessage());
            return "Error executing web search: " + e.getMessage();
        }
    }
    
    /**
     * 提取搜索查询
     * @param task 任务描述
     * @return 搜索查询
     */
    private String extractSearchQuery(String task) {
        // 移除前缀
        String query = task.trim();
        if (query.startsWith("web_search ")) {
            query = query.substring(11); // 移除"web_search "前缀
        } else if (query.startsWith("网页搜索 ")) {
            query = query.substring(5); // 移除"网页搜索 "前缀
        } else if (query.startsWith("search for ")) {
            query = query.substring(11); // 移除"search for "前缀
        } else if (query.startsWith("搜索 ")) {
            query = query.substring(3); // 移除"搜索 "前缀
        }
        
        return query.trim();
    }
    
    /**
     * 执行网页搜索
     * @param query 搜索查询
     * @return 搜索结果
     * @throws Exception 网络或API错误
     */
    private String performWebSearch(String query) throws Exception {
        // 构建搜索URL
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String url = SEARCH_API_URL + "?q=" + encodedQuery + "&count=" + configManager.getWebSearchToolMaxResults() + "&ensearch=1&FORM=BESBTB"; // 限制返回结果数量
        
        // 创建HTTP连接
        URL searchUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) searchUrl.openConnection();
        
        // 设置请求头
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        
        // 读取响应
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            return "Error: Search request failed with response code: " + responseCode;
        }
        
        // 读取响应内容
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        // 简单解析必应搜索结果页面
        return parseBingSearchResults(response.toString());
    }
    
    /**
     * 解析必应搜索结果
     * @param htmlResponse HTML格式的搜索响应
     * @return 格式化的搜索结果
     */
    private String parseBingSearchResults(String htmlResponse) {
        try {
            StringBuilder formattedResults = new StringBuilder();
            int resultCount = 0;
            
            // 查找搜索结果项
            // 必应搜索结果通常包含在<li class="b_algo">标签中
            int pos = 0;
            while (resultCount < 5 && pos < htmlResponse.length()) {
                // 查找<li class="b_algo">标签
                int liStart = htmlResponse.indexOf("<li class=\"b_algo\"", pos);
                if (liStart == -1) {
                    break;
                }
                
                // 查找对应的</li>标签
                int liEnd = htmlResponse.indexOf("</li>", liStart);
                if (liEnd == -1) {
                    break;
                }
                
                // 提取li标签内的内容
                String liContent = htmlResponse.substring(liStart, liEnd + 5);
                
                // 提取链接和标题
                String url = extractUrlFromBingResult(liContent);
                String title = extractTitleFromBingResult(liContent);
                
                if (url != null && title != null && !url.isEmpty() && !title.isEmpty()) {
                    resultCount++;
                    // 清理标题中的HTML标签
                    title = title.replaceAll("<.*?>", "").trim();
                    formattedResults.append(String.format("%d. %s\n   URL: %s\n\n", 
                            resultCount, title, url));
                }
                
                pos = liEnd + 5;
            }
            
            if (resultCount == 0) {
                return "No search results found or unable to parse results.";
            }
            
            return formattedResults.toString().trim();
        } catch (Exception e) {
            return "Error parsing search results: " + e.getMessage() + 
                   "\nNote: This is a simplified parser. For production use, consider using a proper HTML parser like Jsoup.";
        }
    }
    
    /**
     * 从必应搜索结果中提取URL
     * @param liContent li标签内容
     * @return URL
     */
    public String extractUrlFromBingResult(String liContent) {
        try {
            // 查找 <a target="_blank" href="..." 这样的链接
            int targetBlankIndex = liContent.indexOf("target=\"_blank\"");
            if (targetBlankIndex == -1) {
                return null;
            }
            
            int hrefStart = liContent.indexOf("href=\"", targetBlankIndex);
            if (hrefStart == -1) {
                return null;
            }
            
            hrefStart += 6; // 跳过"href=\""
            int hrefEnd = liContent.indexOf("\"", hrefStart);
            if (hrefEnd == -1) {
                return null;
            }
            
            String url = liContent.substring(hrefStart, hrefEnd);
            return url;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从必应搜索结果中提取标题
     * @param liContent li标签内容
     * @return 标题
     */
    public String extractTitleFromBingResult(String liContent) {
        try {
            // 查找 <a target="_blank" href="..." 这样的链接
            int targetBlankIndex = liContent.indexOf("target=\"_blank\"");
            if (targetBlankIndex == -1) {
                return null;
            }
            
            // 向前查找<a标签的开始位置
            int aStart = liContent.lastIndexOf("<a ", targetBlankIndex);
            if (aStart == -1) {
                return null;
            }
            
            int titleStart = liContent.indexOf(">", aStart);
            if (titleStart == -1) {
                return null;
            }
            
            titleStart += 1; // 跳过">"
            int titleEnd = liContent.indexOf("</a>", titleStart);
            if (titleEnd == -1) {
                return null;
            }
            
            return liContent.substring(titleStart, titleEnd);
        } catch (Exception e) {
            return null;
        }
    }
}