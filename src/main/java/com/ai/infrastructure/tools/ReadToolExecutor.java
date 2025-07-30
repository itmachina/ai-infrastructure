package com.ai.infrastructure.tools;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.ai.infrastructure.config.ToolConfigManager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增强的读取工具执行器
 * 支持读取多种数据格式：JSON、文本、Markdown、PDF、Excel
 * 支持读取网络文档并转换为Markdown格式
 */
public class ReadToolExecutor implements ToolExecutor {
    private final Gson gson;
    private final ToolConfigManager configManager;
    
    public ReadToolExecutor() {
        this.gson = new Gson();
        this.configManager = ToolConfigManager.getInstance();
    }
    
    @Override
    public String execute(String task) {
        try {
            // 解析任务参数
            String filePath = extractFilePath(task);
            
            if (filePath == null || filePath.trim().isEmpty()) {
                System.err.println("tengu_read_error: Invalid file path");
                return "Error: Invalid file path";
            }
            
            System.out.println("tengu_read_start: Starting to read file: " + filePath);
            
            // 检查是否为URL
            if (isUrl(filePath)) {
                // 处理网络文档
                String result = readNetworkDocument(filePath);
                
                // 检查读取结果
                if (result.startsWith("Error:")) {
                    System.err.println("tengu_read_error: " + result);
                    return result;
                }
                
                System.out.println("tengu_read_success: Network document read successfully: " + filePath);
                return "Network document content from " + filePath + ":\n" + result;
            } else {
                // 处理本地文件
                // 检查文件是否存在
                if (!Files.exists(Paths.get(filePath))) {
                    System.err.println("tengu_read_error: File not found: " + filePath);
                    return "Error: File not found: " + filePath;
                }
                
                // 根据文件扩展名确定文件类型并读取
                String fileExtension = getFileExtension(filePath).toLowerCase();
                
                String result;
                switch (fileExtension) {
                    case ".json":
                        result = readJsonFile(filePath);
                        break;
                    case ".txt":
                    case ".md":
                    case ".markdown":
                        result = readTextFile(filePath);
                        break;
                    case ".pdf":
                        result = readPdfFile(filePath);
                        break;
                    case ".xlsx":
                    case ".xls":
                        result = readExcelFile(filePath);
                        break;
                    default:
                        // 默认按文本文件处理
                        result = readTextFile(filePath);
                        break;
                }
                
                // 检查读取结果
                if (result.startsWith("Error:")) {
                    System.err.println("tengu_read_error: " + result);
                    return result;
                }
                
                System.out.println("tengu_read_success: File read successfully: " + filePath);
                return "File content from " + filePath + ":\n" + result;
            }
        } catch (Exception e) {
            System.err.println("tengu_read_exception: Exception during file read: " + e.getMessage());
            return "Error reading file: " + e.getMessage();
        }
    }
    
    /**
     * 提取文件路径
     * @param task 任务描述
     * @return 文件路径
     */
    private String extractFilePath(String task) {
        // 移除前缀
        String filePath = task.trim();
        if (filePath.startsWith("read ")) {
            filePath = filePath.substring(5); // 移除"read "前缀
        } else if (filePath.startsWith("读取 ")) {
            filePath = filePath.substring(3); // 移除"读取 "前缀
        }
        
        // 移除可能的引号
        if (filePath.startsWith("\"") && filePath.endsWith("\"")) {
            filePath = filePath.substring(1, filePath.length() - 1);
        } else if (filePath.startsWith("'") && filePath.endsWith("'")) {
            filePath = filePath.substring(1, filePath.length() - 1);
        }
        
        return filePath.trim();
    }
    
    /**
     * 检查字符串是否为URL
     * @param path 路径字符串
     * @return 是否为URL
     */
    private boolean isUrl(String path) {
        return path.startsWith("http://") || path.startsWith("https://");
    }
    
    /**
     * 获取文件扩展名
     * @param filePath 文件路径
     * @return 文件扩展名
     */
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return ""; // 没有扩展名
        }
        return filePath.substring(lastDotIndex);
    }
    
    /**
     * 读取网络文档并转换为Markdown格式
     * @param url 网络文档URL
     * @return Markdown格式的文档内容
     */
    private String readNetworkDocument(String url) {
        HttpURLConnection connection = null;
        try {
            // 创建HTTP连接
            URL documentUrl = new URL(url);
            connection = (HttpURLConnection) documentUrl.openConnection();
            
            // 设置请求头
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            
            // 设置超时时间
            connection.setConnectTimeout(10000); // 连接超时10秒
            connection.setReadTimeout(30000);    // 读取超时30秒
            
            // 读取响应
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return "Error: Network request failed with response code: " + responseCode;
            }
            
            // 读取响应内容
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                    response.append("\n");
                }
            }
            
            // 获取内容类型
            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = "";
            }
            
            // 根据内容类型处理内容
            if (contentType.contains("application/json")) {
                // JSON内容
                return "JSON content from " + url + ":\n" + formatJsonContent(response.toString());
            } else if (contentType.contains("text/plain")) {
                // 纯文本内容
                return "Text content from " + url + ":\n" + response.toString();
            } else {
                // HTML内容，转换为Markdown
                return "Web document content from " + url + ":\n" + convertHtmlToMarkdown(response.toString());
            }
        } catch (java.net.SocketTimeoutException e) {
            return "Error: Network request timed out. Connection timeout: 10 seconds, Read timeout: 30 seconds.";
        } catch (Exception e) {
            return "Error reading network document: " + e.getMessage();
        } finally {
            // 关闭连接
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 将HTML内容转换为Markdown格式
     * @param html HTML内容
     * @return Markdown格式内容
     */
    private String convertHtmlToMarkdown(String html) {
        try {
            StringBuilder markdown = new StringBuilder();
            
            // 移除HTML标签，保留文本内容和基本格式
            // 移除脚本和样式标签及其内容
            html = html.replaceAll("<script[^>]*>.*?</script>", "");
            html = html.replaceAll("<style[^>]*>.*?</style>", "");
            
            // 移除HTML注释
            html = html.replaceAll("<!--.*?-->", "");
            
            // 处理标题标签
            html = html.replaceAll("<h1[^>]*>(.*?)</h1>", "# $1\n\n");
            html = html.replaceAll("<h2[^>]*>(.*?)</h2>", "## $1\n\n");
            html = html.replaceAll("<h3[^>]*>(.*?)</h3>", "### $1\n\n");
            html = html.replaceAll("<h4[^>]*>(.*?)</h4>", "#### $1\n\n");
            html = html.replaceAll("<h5[^>]*>(.*?)</h5>", "##### $1\n\n");
            html = html.replaceAll("<h6[^>]*>(.*?)</h6>", "###### $1\n\n");
            
            // 处理段落标签
            html = html.replaceAll("<p[^>]*>(.*?)</p>", "$1\n\n");
            
            // 处理换行标签
            html = html.replaceAll("<br[^>]*/?>", "\n");
            html = html.replaceAll("</div>", "\n");
            
            // 处理粗体和斜体
            html = html.replaceAll("<b[^>]*>(.*?)</b>", "**$1**");
            html = html.replaceAll("<strong[^>]*>(.*?)</strong>", "**$1**");
            html = html.replaceAll("<i[^>]*>(.*?)</i>", "*$1*");
            html = html.replaceAll("<em[^>]*>(.*?)</em>", "*$1*");
            
            // 处理链接
            Pattern linkPattern = Pattern.compile("<a[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>");
            Matcher linkMatcher = linkPattern.matcher(html);
            StringBuffer linkBuffer = new StringBuffer();
            while (linkMatcher.find()) {
                String url = linkMatcher.group(1);
                String text = linkMatcher.group(2);
                linkMatcher.appendReplacement(linkBuffer, "[" + text + "](" + url + ")");
            }
            linkMatcher.appendTail(linkBuffer);
            html = linkBuffer.toString();
            
            // 处理列表
            html = html.replaceAll("<ul[^>]*>", "\n");
            html = html.replaceAll("</ul>", "\n");
            html = html.replaceAll("<ol[^>]*>", "\n");
            html = html.replaceAll("</ol>", "\n");
            html = html.replaceAll("<li[^>]*>(.*?)</li>", "- $1\n");
            
            // 处理代码块
            html = html.replaceAll("<pre[^>]*>(.*?)</pre>", "```\n$1\n```\n");
            html = html.replaceAll("<code[^>]*>(.*?)</code>", "`$1`");
            
            // 移除剩余的HTML标签
            html = html.replaceAll("<[^>]*>", "");
            
            // 清理多余的空白行
            html = html.replaceAll("\n\\s*\n\\s*\n", "\n\n");
            
            // 限制输出长度
            if (html.length() > configManager.getReadToolMaxHtmlContentLength()) {
                html = html.substring(0, configManager.getReadToolMaxHtmlContentLength()) + "\n" + configManager.getReadToolHtmlTruncateMessage();
            }
            
            return html.trim();
        } catch (Exception e) {
            return "Error converting HTML to Markdown: " + e.getMessage() + "\nRaw content:\n" + 
                   (html.length() > configManager.getReadToolMaxRawHtmlForError() ? html.substring(0, configManager.getReadToolMaxRawHtmlForError()) + "..." : html);
        }
    }
    
    /**
     * 读取JSON文件
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    private String readJsonFile(String filePath) throws IOException {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            
            // 验证JSON格式
            try {
                JsonParser.parseString(content);
            } catch (JsonSyntaxException e) {
                return "Error: Invalid JSON format in file: " + filePath + "\n" + e.getMessage();
            }
            
            // 格式化JSON输出
            return "JSON file content from " + filePath + ":\n" + formatJsonContent(content);
        } catch (Exception e) {
            return "Error reading JSON file: " + e.getMessage();
        }
    }
    
    /**
     * 格式化JSON内容
     * @param content JSON内容
     * @return 格式化后的JSON内容
     */
    private String formatJsonContent(String content) {
        try {
            JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
            return gson.toJson(jsonObject);
        } catch (Exception e) {
            // 如果不是对象格式，直接返回
            return content;
        }
    }
    
    /**
     * 读取文本文件（包括Markdown）
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    private String readTextFile(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        
        StringBuilder content = new StringBuilder();
        content.append("Text file content from ").append(filePath).append(":\n");
        
        for (int i = 0; i < lines.size(); i++) {
            content.append(lines.get(i)).append("\n");
            
            // 限制输出长度，避免过长内容
            if (content.length() > configManager.getReadToolMaxTextContentLength() && i < lines.size() - 10) {
                content.append("\n").append(configManager.getReadToolTruncateMessage()).append("\n");
                // 添加最后几行
                for (int j = Math.max(lines.size() - 5, i + 1); j < lines.size(); j++) {
                    content.append(lines.get(j)).append("\n");
                }
                break;
            }
        }
        
        return content.toString();
    }
    
    /**
     * 读取PDF文件
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    private String readPdfFile(String filePath) throws IOException {
        try {
            // 使用PDFBox读取PDF文件
            PDDocument document = PDDocument.load(new File(filePath));
            PDFTextStripper pdfStripper = new PDFTextStripper();
            
            String text = pdfStripper.getText(document);
            document.close();
            
            // 限制输出长度
            if (text.length() > configManager.getReadToolMaxPdfContentLength()) {
                text = text.substring(0, configManager.getReadToolMaxPdfContentLength()) + "\n" + configManager.getReadToolTruncateMessage();
            }
            
            return "PDF file content from " + filePath + ":\n" + text;
        } catch (Exception e) {
            return "Error reading PDF file: " + e.getMessage();
        }
    }
    
    /**
     * 读取Excel文件
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    private String readExcelFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = createWorkbook(filePath, fis)) {
            
            StringBuilder content = new StringBuilder();
            content.append("Excel file content from ").append(filePath).append(":\n");
            
            // 读取所有工作表
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("\nSheet ").append(i + 1).append(" (").append(sheet.getSheetName()).append("):\n");
                
                // 读取行数据
                int rowCount = 0;
                for (Row row : sheet) {
                    content.append("Row ").append(row.getRowNum() + 1).append(": ");
                    
                    // 读取单元格数据
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        content.append("| ").append(cellValue).append(" ");
                    }
                    content.append("|\n");
                    
                    rowCount++;
                    
                    // 限制行数，避免过长内容
                    if (rowCount > configManager.getReadToolMaxExcelRows()) {
                        content.append(configManager.getReadToolExcelTruncateMessage()).append("\n");
                        break;
                    }
                }
            }
            
            return content.toString();
        } catch (Exception e) {
            return "Error reading Excel file: " + e.getMessage();
        }
    }
    
    /**
     * 根据文件扩展名创建相应的工作簿
     * @param filePath 文件路径
     * @param fis 文件输入流
     * @return 工作簿
     * @throws IOException IO异常
     */
    private Workbook createWorkbook(String filePath, FileInputStream fis) throws IOException {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else {
            return new HSSFWorkbook(fis);
        }
    }
    
    /**
     * 将单元格值转换为字符串
     * @param cell 单元格
     * @return 单元格值的字符串表示
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}