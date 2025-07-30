package com.ai.infrastructure.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 网页搜索工具执行器测试类
 */
public class WebSearchToolExecutorTest {

    private WebSearchToolExecutor webSearchToolExecutor;

    @BeforeEach
    public void setUp() {
        webSearchToolExecutor = new WebSearchToolExecutor();
    }

    @Test
    public void testWebSearchToolCreation() {
        // 测试工具执行器创建
        assertNotNull(webSearchToolExecutor);
    }

    @Test
    public void testExtractSearchQuery() {
        // 测试搜索查询提取
        String result = webSearchToolExecutor.execute("web_search 人工智能发展趋势");
        assertNotNull(result);
        // 期望返回错误或搜索结果
        assertTrue(result.contains("Error") || result.contains("Web search results") || result.contains("No search results found"));
    }

    @Test
    public void testInvalidSearchQuery() {
        // 测试无效搜索查询
        String result = webSearchToolExecutor.execute("");
        assertTrue(result.contains("Error") || result.contains("Invalid search query"));
    }
    
    @Test
    public void testUrlExtraction() {
        // 测试URL提取功能
        String liContent = "<li class=\"b_algo\"><h2><a href=\"http://example.com\" target=\"_blank\">Example Title</a></h2></li>";
        String url = webSearchToolExecutor.extractUrlFromBingResult(liContent);
        assertEquals("http://example.com", url);
    }
    
    @Test
    public void testTitleExtraction() {
        // 测试标题提取功能
        String liContent = "<li class=\"b_algo\"><h2><a href=\"http://example.com\" target=\"_blank\">Example Title</a></h2></li>";
        String title = webSearchToolExecutor.extractTitleFromBingResult(liContent);
        assertEquals("Example Title", title);
    }
}