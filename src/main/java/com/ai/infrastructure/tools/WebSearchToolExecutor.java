package com.ai.infrastructure.tools;

import com.ai.infrastructure.config.ToolConfigManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 学术搜索工具执行器
 * 支持arXiv学术搜索功能
 */
public class WebSearchToolExecutor implements ToolExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WebSearchToolExecutor.class);
    /**
     * 搜索请求类，包含分页参数、排序选项和搜索域
     */
    private static class SearchRequest {
        private final String query;
        private final int limit;
        private final int offset;
        private final String sortBy;
        private final String sortOrder;
        private final String domain;
        
        public SearchRequest(String query, int limit, int offset, String sortBy, String sortOrder, String domain) {
            this.query = query;
            this.limit = Math.max(1, Math.min(limit, 100)); // 限制在1-100之间
            this.offset = Math.max(0, offset);
            this.sortBy = sortBy != null ? sortBy : "relevance";
            this.sortOrder = sortOrder != null ? sortOrder : "descending";
            this.domain = domain != null ? domain : "all";
        }
        
        public String getQuery() { return query; }
        public int getLimit() { return limit; }
        public int getOffset() { return offset; }
        public String getSortBy() { return sortBy; }
        public String getSortOrder() { return sortOrder; }
        public String getDomain() { return domain; }
    }
    private static final String SEARCH_API_URL = "https://export.arxiv.org/api/query";
    
    // 关键词到领域的映射（中英文混合）
    private static final Map<String, String> KEYWORD_DOMAIN_MAP = new HashMap<>();
    static {
        // Computer Science (cs) - 中英文关键词
        KEYWORD_DOMAIN_MAP.put("machine learning", "cs");
        KEYWORD_DOMAIN_MAP.put("artificial intelligence", "cs");
        KEYWORD_DOMAIN_MAP.put("deep learning", "cs");
        KEYWORD_DOMAIN_MAP.put("neural network", "cs");
        KEYWORD_DOMAIN_MAP.put("algorithm", "cs");
        KEYWORD_DOMAIN_MAP.put("data mining", "cs");
        KEYWORD_DOMAIN_MAP.put("computer vision", "cs");
        KEYWORD_DOMAIN_MAP.put("natural language processing", "cs");
        KEYWORD_DOMAIN_MAP.put("nlp", "cs");
        KEYWORD_DOMAIN_MAP.put("software engineering", "cs");
        KEYWORD_DOMAIN_MAP.put("database", "cs");
        KEYWORD_DOMAIN_MAP.put("cybersecurity", "cs");
        KEYWORD_DOMAIN_MAP.put("cryptography", "cs");
        KEYWORD_DOMAIN_MAP.put("distributed systems", "cs");
        KEYWORD_DOMAIN_MAP.put("cloud computing", "cs");
        KEYWORD_DOMAIN_MAP.put("blockchain", "cs");
        
        // 中文计算机科学关键词
        KEYWORD_DOMAIN_MAP.put("机器学习", "cs");
        KEYWORD_DOMAIN_MAP.put("人工智能", "cs");
        KEYWORD_DOMAIN_MAP.put("深度学习", "cs");
        KEYWORD_DOMAIN_MAP.put("神经网络", "cs");
        KEYWORD_DOMAIN_MAP.put("算法", "cs");
        KEYWORD_DOMAIN_MAP.put("数据挖掘", "cs");
        KEYWORD_DOMAIN_MAP.put("计算机视觉", "cs");
        KEYWORD_DOMAIN_MAP.put("自然语言处理", "cs");
        KEYWORD_DOMAIN_MAP.put("软件工程", "cs");
        KEYWORD_DOMAIN_MAP.put("数据库", "cs");
        KEYWORD_DOMAIN_MAP.put("网络安全", "cs");
        KEYWORD_DOMAIN_MAP.put("密码学", "cs");
        KEYWORD_DOMAIN_MAP.put("分布式系统", "cs");
        KEYWORD_DOMAIN_MAP.put("云计算", "cs");
        KEYWORD_DOMAIN_MAP.put("区块链", "cs");
        KEYWORD_DOMAIN_MAP.put("编程", "cs");
        KEYWORD_DOMAIN_MAP.put("操作系统", "cs");
        KEYWORD_DOMAIN_MAP.put("网络", "cs");
        
        // Mathematics (math) - 中英文关键词
        KEYWORD_DOMAIN_MAP.put("topology", "math");
        KEYWORD_DOMAIN_MAP.put("algebra", "math");
        KEYWORD_DOMAIN_MAP.put("geometry", "math");
        KEYWORD_DOMAIN_MAP.put("calculus", "math");
        KEYWORD_DOMAIN_MAP.put("analysis", "math");
        KEYWORD_DOMAIN_MAP.put("probability", "math");
        KEYWORD_DOMAIN_MAP.put("statistics", "math");
        KEYWORD_DOMAIN_MAP.put("number theory", "math");
        KEYWORD_DOMAIN_MAP.put("discrete mathematics", "math");
        KEYWORD_DOMAIN_MAP.put("linear algebra", "math");
        KEYWORD_DOMAIN_MAP.put("differential equation", "math");
        KEYWORD_DOMAIN_MAP.put("graph theory", "math");
        
        // 中文数学关键词
        KEYWORD_DOMAIN_MAP.put("拓扑学", "math");
        KEYWORD_DOMAIN_MAP.put("代数", "math");
        KEYWORD_DOMAIN_MAP.put("几何", "math");
        KEYWORD_DOMAIN_MAP.put("微积分", "math");
        KEYWORD_DOMAIN_MAP.put("分析", "math");
        KEYWORD_DOMAIN_MAP.put("概率论", "math");
        KEYWORD_DOMAIN_MAP.put("统计学", "math");
        KEYWORD_DOMAIN_MAP.put("数论", "math");
        KEYWORD_DOMAIN_MAP.put("离散数学", "math");
        KEYWORD_DOMAIN_MAP.put("线性代数", "math");
        KEYWORD_DOMAIN_MAP.put("微分方程", "math");
        KEYWORD_DOMAIN_MAP.put("图论", "math");
        KEYWORD_DOMAIN_MAP.put("数学", "math");
        
        // Physics - 中英文关键词
        KEYWORD_DOMAIN_MAP.put("quantum", "physics");
        KEYWORD_DOMAIN_MAP.put("relativity", "physics");
        KEYWORD_DOMAIN_MAP.put("thermodynamics", "physics");
        KEYWORD_DOMAIN_MAP.put("electromagnetism", "physics");
        KEYWORD_DOMAIN_MAP.put("mechanics", "physics");
        KEYWORD_DOMAIN_MAP.put("particle physics", "physics");
        KEYWORD_DOMAIN_MAP.put("condensed matter", "physics");
        KEYWORD_DOMAIN_MAP.put("optics", "physics");
        KEYWORD_DOMAIN_MAP.put("nuclear", "physics");
        KEYWORD_DOMAIN_MAP.put("astrophysics", "physics");
        
        // 中文物理关键词
        KEYWORD_DOMAIN_MAP.put("量子", "physics");
        KEYWORD_DOMAIN_MAP.put("相对论", "physics");
        KEYWORD_DOMAIN_MAP.put("热力学", "physics");
        KEYWORD_DOMAIN_MAP.put("电磁学", "physics");
        KEYWORD_DOMAIN_MAP.put("力学", "physics");
        KEYWORD_DOMAIN_MAP.put("粒子物理", "physics");
        KEYWORD_DOMAIN_MAP.put("凝聚态物理", "physics");
        KEYWORD_DOMAIN_MAP.put("光学", "physics");
        KEYWORD_DOMAIN_MAP.put("核物理", "physics");
        KEYWORD_DOMAIN_MAP.put("天体物理", "physics");
        KEYWORD_DOMAIN_MAP.put("物理", "physics");
        
        // Statistics - 中英文关键词
        KEYWORD_DOMAIN_MAP.put("statistical learning", "stat");
        KEYWORD_DOMAIN_MAP.put("regression", "stat");
        KEYWORD_DOMAIN_MAP.put("hypothesis testing", "stat");
        KEYWORD_DOMAIN_MAP.put("bayesian", "stat");
        KEYWORD_DOMAIN_MAP.put("time series", "stat");
        KEYWORD_DOMAIN_MAP.put("multivariate analysis", "stat");
        
        // 中文统计学关键词
        KEYWORD_DOMAIN_MAP.put("统计学习", "stat");
        KEYWORD_DOMAIN_MAP.put("回归", "stat");
        KEYWORD_DOMAIN_MAP.put("假设检验", "stat");
        KEYWORD_DOMAIN_MAP.put("贝叶斯", "stat");
        KEYWORD_DOMAIN_MAP.put("时间序列", "stat");
        KEYWORD_DOMAIN_MAP.put("多元分析", "stat");
        KEYWORD_DOMAIN_MAP.put("统计", "stat");
        
        // Quantitative Biology - 中英文关键词
        KEYWORD_DOMAIN_MAP.put("bioinformatics", "q-bio");
        KEYWORD_DOMAIN_MAP.put("computational biology", "q-bio");
        KEYWORD_DOMAIN_MAP.put("genomics", "q-bio");
        KEYWORD_DOMAIN_MAP.put("proteomics", "q-bio");
        KEYWORD_DOMAIN_MAP.put("systems biology", "q-bio");
        KEYWORD_DOMAIN_MAP.put("molecular biology", "q-bio");
        
        // 中文生物信息学关键词
        KEYWORD_DOMAIN_MAP.put("生物信息学", "q-bio");
        KEYWORD_DOMAIN_MAP.put("计算生物学", "q-bio");
        KEYWORD_DOMAIN_MAP.put("基因组学", "q-bio");
        KEYWORD_DOMAIN_MAP.put("蛋白质组学", "q-bio");
        KEYWORD_DOMAIN_MAP.put("系统生物学", "q-bio");
        KEYWORD_DOMAIN_MAP.put("分子生物学", "q-bio");
        KEYWORD_DOMAIN_MAP.put("生物", "q-bio");
        
        // Quantitative Finance - 中英文关键词
        KEYWORD_DOMAIN_MAP.put("financial modeling", "q-fin");
        KEYWORD_DOMAIN_MAP.put("risk management", "q-fin");
        KEYWORD_DOMAIN_MAP.put("portfolio theory", "q-fin");
        KEYWORD_DOMAIN_MAP.put("algorithmic trading", "q-fin");
        KEYWORD_DOMAIN_MAP.put("cryptocurrency", "q-fin");
        
        // 中文金融数学关键词
        KEYWORD_DOMAIN_MAP.put("金融建模", "q-fin");
        KEYWORD_DOMAIN_MAP.put("风险管理", "q-fin");
        KEYWORD_DOMAIN_MAP.put("投资组合理论", "q-fin");
        KEYWORD_DOMAIN_MAP.put("算法交易", "q-fin");
        KEYWORD_DOMAIN_MAP.put("加密货币", "q-fin");
        KEYWORD_DOMAIN_MAP.put("金融", "q-fin");
        
        // Electrical Engineering and Systems Science - 中英文关键词
        KEYWORD_DOMAIN_MAP.put("signal processing", "eess");
        KEYWORD_DOMAIN_MAP.put("image processing", "eess");
        KEYWORD_DOMAIN_MAP.put("control systems", "eess");
        KEYWORD_DOMAIN_MAP.put("communication systems", "eess");
        KEYWORD_DOMAIN_MAP.put("power systems", "eess");
        
        // 中文电子工程关键词
        KEYWORD_DOMAIN_MAP.put("信号处理", "eess");
        KEYWORD_DOMAIN_MAP.put("图像处理", "eess");
        KEYWORD_DOMAIN_MAP.put("控制系统", "eess");
        KEYWORD_DOMAIN_MAP.put("通信系统", "eess");
        KEYWORD_DOMAIN_MAP.put("电力系统", "eess");
        KEYWORD_DOMAIN_MAP.put("电子", "eess");
        
        // Economics - 中英文关键词
        KEYWORD_DOMAIN_MAP.put("econometrics", "econ");
        KEYWORD_DOMAIN_MAP.put("game theory", "econ");
        KEYWORD_DOMAIN_MAP.put("behavioral economics", "econ");
        KEYWORD_DOMAIN_MAP.put("microeconomics", "econ");
        KEYWORD_DOMAIN_MAP.put("macroeconomics", "econ");
        
        // 中文经济学关键词
        KEYWORD_DOMAIN_MAP.put("计量经济学", "econ");
        KEYWORD_DOMAIN_MAP.put("博弈论", "econ");
        KEYWORD_DOMAIN_MAP.put("行为经济学", "econ");
        KEYWORD_DOMAIN_MAP.put("微观经济学", "econ");
        KEYWORD_DOMAIN_MAP.put("宏观经济学", "econ");
        KEYWORD_DOMAIN_MAP.put("经济", "econ");
    }
    
    private final Gson gson;
    private final ToolConfigManager configManager;
    
    public WebSearchToolExecutor() {
        this.gson = new Gson();
        this.configManager = ToolConfigManager.getInstance();
    }
    
    @Override
    public String execute(String task) {
        try {
            // 解析搜索查询和分页参数
            SearchRequest request = parseSearchRequest(task);
            
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                logger.error("tengu_web_search_error: Invalid search query");
                return "Error: Invalid search query";
            }
            
            logger.info("tengu_web_search_start: Starting web search for query: {} (domain: {}, limit: {}, offset: {}, sortBy: {}, sortOrder: {})", 
                    request.getQuery(), request.getDomain(), request.getLimit(), request.getOffset(), 
                    request.getSortBy(), request.getSortOrder());
            
            // 执行搜索
            String searchResults = performWebSearch(request);
            
            // 检查搜索结果
            if (searchResults.startsWith("Error:")) {
                logger.error("tengu_web_search_error: {}", searchResults);
                return searchResults;
            }
            logger.info("web search result:{}", searchResults);
            logger.info("tengu_web_search_success: Web search completed successfully");
            String domainInfo = "all".equals(request.getDomain()) ? "all domains" : request.getDomain() + " domain";
            return "Web search results for '" + request.getQuery() + "' in " + domainInfo + 
                   " (showing " + Math.min(request.getLimit(), getResultCount(searchResults)) + 
                   " results, sorted by " + request.getSortBy() + " " + request.getSortOrder() + "):\n" + searchResults;
        } catch (Exception e) {
            logger.error("tengu_web_search_exception: Exception during web search: {}", e.getMessage(), e);
            return "Error executing web search: " + e.getMessage();
        }
    }
    
    /**
     * 根据搜索内容检测最适合的领域
     * @param query 搜索查询
     * @return 检测到的领域，如果没有匹配则返回"all"
     */
    private String detectDomainFromQuery(String query) {
        String lowerQuery = query.toLowerCase().trim();
        
        // 关键词匹配计数
        Map<String, Integer> domainScores = new HashMap<>();
        for (Map.Entry<String, String> entry : KEYWORD_DOMAIN_MAP.entrySet()) {
            String keyword = entry.getKey();
            String domain = entry.getValue();
            
            if (lowerQuery.contains(keyword)) {
                domainScores.put(domain, domainScores.getOrDefault(domain, 0) + 1);
            }
        }
        
        // 返回匹配次数最多的领域
        if (!domainScores.isEmpty()) {
            return domainScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("all");
        }
        
        return "all";
    }
    
    /**
     * 解析搜索请求，包括分页参数、排序参数和搜索域
     * @param task 任务描述
     * @return 搜索请求对象
     */
    private SearchRequest parseSearchRequest(String task) {
        String query = task.trim();
        int limit = configManager.getWebSearchToolMaxResults(); // 默认从配置读取
        int offset = 0;
        String sortBy = "relevance";
        String sortOrder = "descending";
        String domain = "all";
        
        // 解析搜索域格式: "query cs" 或 "query domain:cs" - 领域在搜索词末尾
        boolean domainSpecified = false;
        String[] domainPrefixes = {"cs", "math", "physics", "stat", "q-bio", "q-fin", "eess", "econ"};
        
        // 检查是否在末尾有领域标识
        for (String prefix : domainPrefixes) {
            // 格式1: "query cs"
            if (query.endsWith(" " + prefix)) {
                domain = prefix;
                query = query.substring(0, query.length() - prefix.length() - 1).trim();
                domainSpecified = true;
                break;
            }
            // 格式2: "query domain:cs"
            if (query.endsWith(" domain:" + prefix)) {
                domain = prefix;
                query = query.substring(0, query.length() - (" domain:" + prefix).length()).trim();
                domainSpecified = true;
                break;
            }
        }
        
        // 如果没有手动指定领域，使用自动检测
        if (!domainSpecified) {
            String detectedDomain = detectDomainFromQuery(query);
            if (!"all".equals(detectedDomain)) {
                domain = detectedDomain;
            }
        }
        
        // 解析排序参数格式: "query sortBy:lastUpdatedDate sortOrder:descending"
        if (query.contains(" sortBy:") || query.contains(" sortOrder:")) {
            String[] sortParts = query.split(" sortBy:");
            if (sortParts.length > 1) {
                query = sortParts[0].trim();
                String[] orderParts = sortParts[1].split(" sortOrder:");
                sortBy = orderParts[0].trim();
                if (orderParts.length > 1) {
                    sortOrder = orderParts[1].trim();
                }
            }
        }
        
        // 解析分页参数格式: "query limit:10 offset:0"
        String[] parts = query.split(" limit:");
        if (parts.length > 1) {
            query = parts[0].trim();
            String[] limitOffset = parts[1].split(" offset:");
            try {
                limit = Integer.parseInt(limitOffset[0].trim());
                if (limitOffset.length > 1) {
                    String remaining = limitOffset[1];
                    
                    // 解析偏移量
                    String[] offsetParts = remaining.split(" ");
                    offset = Integer.parseInt(offsetParts[0].trim());
                    
                    // 处理剩余参数
                    if (remaining.contains("sortBy:")) {
                        String sortStr = remaining.substring(remaining.indexOf("sortBy:"));
                        String[] sortArr = sortStr.split(" ");
                        if (sortArr.length > 0 && sortArr[0].startsWith("sortBy:")) {
                            sortBy = sortArr[0].substring(7);
                        }
                        if (sortStr.contains("sortOrder:")) {
                            String orderStr = sortStr.substring(sortStr.indexOf("sortOrder:"));
                            String[] orderArr = orderStr.split(" ");
                            if (orderArr.length > 0 && orderArr[0].startsWith("sortOrder:")) {
                                sortOrder = orderArr[0].substring(10);
                            }
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
        
        // 解析简化的分页格式: "query page:1 size:10"
        if (query.contains(" page:") || query.contains(" size:")) {
            String[] pageParts = query.split(" page:");
            if (pageParts.length > 1) {
                query = pageParts[0].trim();
                String[] sizeParts = pageParts[1].split(" size:");
                try {
                    int page = Integer.parseInt(sizeParts[0].trim());
                    int pageSize = sizeParts.length > 1 ? Integer.parseInt(sizeParts[1].trim()) : limit;
                    limit = pageSize;
                    offset = (page - 1) * pageSize;
                } catch (NumberFormatException e) {
                    // 使用默认值
                }
            }
        }
        
        // 移除前缀
        if (query.startsWith("web_search ")) {
            query = query.substring(11);
        } else if (query.startsWith("网页搜索 ")) {
            query = query.substring(5);
        } else if (query.startsWith("search for ")) {
            query = query.substring(11);
        } else if (query.startsWith("搜索 ")) {
            query = query.substring(3);
        }
        
        return new SearchRequest(query.trim(), limit, offset, sortBy, sortOrder, domain);
    }
    
    /**
     * 执行网页搜索
     * @param request 搜索请求对象
     * @return 搜索结果
     * @throws Exception 网络或API错误
     */
    private String performWebSearch(SearchRequest request) throws Exception {
        // 构建搜索URL - 使用arxiv API的搜索参数
        String encodedQuery = URLEncoder.encode(request.getQuery(), StandardCharsets.UTF_8.toString());

        // 根据搜索域构建搜索查询
        String searchQuery;
        if ("all".equals(request.getDomain())) {
            searchQuery = "all:" + encodedQuery;
        } else {
            searchQuery = encodedQuery + "+" + request.getDomain();
        }


        String url = SEARCH_API_URL + "?search_query=" + searchQuery +
                    "&start=" + request.getOffset() + "&max_results=" + request.getLimit() +
                    "&sortBy=" + request.getSortBy() + "&sortOrder=" + request.getSortOrder();
        logger.info("performing web search url: {}" + url);
        // 创建HTTP连接
        URL searchUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) searchUrl.openConnection();
        
        // 设置请求头
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "application/xml");
        // 自动跟随重定向
        connection.setInstanceFollowRedirects(true);
        
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
        
        // 解析arxiv搜索结果XML
        return parseArxivSearchResults(response.toString(), request.getLimit());
    }
    
    /**
     * 解析arxiv搜索结果XML
     * @param xmlResponse XML格式的搜索响应
     * @param maxResults 最大结果数
     * @return 格式化的搜索结果
     */
    private String parseArxivSearchResults(String xmlResponse, int maxResults) {
        try {
            StringBuilder formattedResults = new StringBuilder();
            int resultCount = 0;
            
            // 查找entry标签
            int pos = 0;
            while (resultCount < maxResults && pos < xmlResponse.length()) {
                // 查找<entry>标签
                int entryStart = xmlResponse.indexOf("<entry>", pos);
                if (entryStart == -1) {
                    break;
                }
                
                // 查找对应的</entry>标签
                int entryEnd = xmlResponse.indexOf("</entry>", entryStart);
                if (entryEnd == -1) {
                    break;
                }
                
                // 提取entry标签内的内容
                String entryContent = xmlResponse.substring(entryStart, entryEnd + 8);
                
                // 提取标题、作者、摘要和链接
                String title = extractTitleFromArxivEntry(entryContent);
                String authors = extractAuthorsFromArxivEntry(entryContent);
                String summary = extractSummaryFromArxivEntry(entryContent);
                String url = extractUrlFromArxivEntry(entryContent);
                String published = extractPublishedDateFromArxivEntry(entryContent);
                
                if (title != null && !title.isEmpty() && url != null && !url.isEmpty()) {
                    resultCount++;
                    formattedResults.append(String.format("%d. %s\n   Authors: %s\n   Published: %s\n   Summary: %s\n   URL: %s\n\n", 
                            resultCount, title, authors, published, summary, url));
                }
                
                pos = entryEnd + 8;
            }
            
            if (resultCount == 0) {
                return "No search results found or unable to parse results.";
            }
            
            return formattedResults.toString().trim();
        } catch (Exception e) {
            return "Error parsing search results: " + e.getMessage();
        }
    }
    
    /**
     * 获取搜索结果数量
     * @param searchResults 搜索结果字符串
     * @return 结果数量
     */
    private int getResultCount(String searchResults) {
        if (searchResults == null || searchResults.isEmpty()) {
            return 0;
        }
        return searchResults.split("\\n\\n").length;
    }
    
    /**
     * 从arxiv条目中提取标题
     * @param entryContent 条目内容
     * @return 标题
     */
    public String extractTitleFromArxivEntry(String entryContent) {
        try {
            int titleStart = entryContent.indexOf("<title>");
            if (titleStart == -1) {
                return null;
            }
            
            titleStart += 7; // 跳过"<title>"
            int titleEnd = entryContent.indexOf("</title>", titleStart);
            if (titleEnd == -1) {
                return null;
            }
            
            return entryContent.substring(titleStart, titleEnd).trim();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从arxiv条目中提取作者
     * @param entryContent 条目内容
     * @return 作者列表
     */
    private String extractAuthorsFromArxivEntry(String entryContent) {
        try {
            StringBuilder authors = new StringBuilder();
            int pos = 0;
            
            while (pos < entryContent.length()) {
                int authorStart = entryContent.indexOf("<author>", pos);
                if (authorStart == -1) {
                    break;
                }
                
                int nameStart = entryContent.indexOf("<name>", authorStart);
                if (nameStart == -1) {
                    pos = authorStart + 8;
                    continue;
                }
                
                nameStart += 6; // 跳过"<name>"
                int nameEnd = entryContent.indexOf("</name>", nameStart);
                if (nameEnd == -1) {
                    pos = authorStart + 8;
                    continue;
                }
                
                String name = entryContent.substring(nameStart, nameEnd).trim();
                if (authors.length() > 0) {
                    authors.append(", ");
                }
                authors.append(name);
                
                pos = nameEnd + 7; // 跳过"</name>"
            }
            
            return authors.length() > 0 ? authors.toString() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * 从arxiv条目中提取摘要
     * @param entryContent 条目内容
     * @return 摘要
     */
    private String extractSummaryFromArxivEntry(String entryContent) {
        try {
            int summaryStart = entryContent.indexOf("<summary>");
            if (summaryStart == -1) {
                return "No summary available";
            }
            
            summaryStart += 9; // 跳过"<summary>"
            int summaryEnd = entryContent.indexOf("</summary>", summaryStart);
            if (summaryEnd == -1) {
                return "No summary available";
            }
            
            String summary = entryContent.substring(summaryStart, summaryEnd).trim();
            // 限制摘要长度
            if (summary.length() > 200) {
                summary = summary.substring(0, 197) + "...";
            }
            
            return summary;
        } catch (Exception e) {
            return "No summary available";
        }
    }
    
    /**
     * 从arxiv条目中提取URL
     * @param entryContent 条目内容
     * @return URL
     */
    public String extractUrlFromArxivEntry(String entryContent) {
        try {
            // 查找包含arxiv.org/abs/的链接
            int idStart = entryContent.indexOf("<id>");
            if (idStart == -1) {
                return null;
            }
            
            idStart += 4; // 跳过"<id>"
            int idEnd = entryContent.indexOf("</id>", idStart);
            if (idEnd == -1) {
                return null;
            }
            
            return entryContent.substring(idStart, idEnd).trim();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从arxiv条目中提取发布日期
     * @param entryContent 条目内容
     * @return 发布日期
     */
    private String extractPublishedDateFromArxivEntry(String entryContent) {
        try {
            int publishedStart = entryContent.indexOf("<published>");
            if (publishedStart == -1) {
                return "Unknown";
            }
            
            publishedStart += 11; // 跳过"<published>"
            int publishedEnd = entryContent.indexOf("</published>", publishedStart);
            if (publishedEnd == -1) {
                return "Unknown";
            }
            
            String published = entryContent.substring(publishedStart, publishedEnd).trim();
            // 提取日期部分
            if (published.contains("T")) {
                published = published.substring(0, published.indexOf("T"));
            }
            
            return published;
        } catch (Exception e) {
            return "Unknown";
        }
    }
}