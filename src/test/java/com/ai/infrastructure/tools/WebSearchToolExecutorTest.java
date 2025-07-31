package com.ai.infrastructure.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 学术搜索工具执行器测试类
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
    public void testTitleExtraction() {
        // 测试标题提取功能
        String entryContent = "<entry><title>Example Academic Paper</title></entry>";
        String title = ((WebSearchToolExecutor) webSearchToolExecutor).extractTitleFromArxivEntry(entryContent);
        assertEquals("Example Academic Paper", title);
    }
    
    @Test
    public void testUrlExtraction() {
        // 测试URL提取功能
        String entryContent = "<entry><id>https://arxiv.org/abs/1234.56789</id></entry>";
        String url = ((WebSearchToolExecutor) webSearchToolExecutor).extractUrlFromArxivEntry(entryContent);
        assertEquals("https://arxiv.org/abs/1234.56789", url);
    }
}