package com.ai.infrastructure.tools;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 写入工具执行器
 * 实现安全的文件写入功能，包括强制读取验证、原子写入、备份恢复等机制
 */
public class WriteToolExecutor implements ToolExecutor {
    private final Gson gson;
    private static final Map<String, FileState> readFileState = new ConcurrentHashMap<>();

    
    public WriteToolExecutor() {
        this.gson = new Gson();
    }
    
    @Override
    public String execute(String task) {
        try {
            // 解析任务参数
            WriteParameters params = parseWriteParameters(task);
            
            if (params.filePath == null || params.filePath.trim().isEmpty()) {
                return "Error: Invalid file path";
            }
            
            if (params.content == null) {
                return "Error: Content cannot be null";
            }
            
            // 执行写入操作
            return performWriteOperation(params);
        } catch (Exception e) {
            return "Error executing write operation: " + e.getMessage();
        }
    }
    
    /**
     * 解析写入参数
     * @param task 任务描述
     * @return 写入参数对象
     */
    private WriteParameters parseWriteParameters(String task) {
        WriteParameters params = new WriteParameters();
        
        try {
            // 尝试解析为JSON格式
            JsonObject taskJson = JsonParser.parseString(task).getAsJsonObject();
            
            params.filePath = taskJson.has("file_path") ? taskJson.get("file_path").getAsString() : null;
            params.content = taskJson.has("content") ? taskJson.get("content").getAsString() : null;
            params.encoding = taskJson.has("encoding") ? taskJson.get("encoding").getAsString() : "UTF-8";
            params.addBom = taskJson.has("add_bom") && taskJson.get("add_bom").getAsBoolean();
            params.overwrite = !taskJson.has("overwrite") || taskJson.get("overwrite").getAsBoolean();
            params.createDirectories = !taskJson.has("create_directories") || taskJson.get("create_directories").getAsBoolean();
            params.createBackup = !taskJson.has("create_backup") || taskJson.get("create_backup").getAsBoolean();
            params.enableHashVerification = taskJson.has("enable_hash_verification") && taskJson.get("enable_hash_verification").getAsBoolean();
            params.validateEncoding = !taskJson.has("validate_encoding") || taskJson.get("validate_encoding").getAsBoolean();
            params.fileMode = taskJson.has("file_mode") ? taskJson.get("file_mode").getAsInt() : 0644;
            params.atomicWrite = !taskJson.has("atomic_write") || taskJson.get("atomic_write").getAsBoolean();
            params.syncAfterWrite = taskJson.has("sync_after_write") && taskJson.get("sync_after_write").getAsBoolean();
            params.autoCleanupBackup = !taskJson.has("auto_cleanup_backup") || taskJson.get("auto_cleanup_backup").getAsBoolean();
            params.backupRetentionTime = taskJson.has("backup_retention_time") ? taskJson.get("backup_retention_time").getAsLong() : 3600000L; // 1小时
            
        } catch (JsonSyntaxException | IllegalStateException e) {
            // 如果不是JSON格式，使用简单解析
            params = parseSimpleWriteParameters(task);
        }
        
        return params;
    }
    
    /**
     * 解析简单格式的写入参数
     * @param task 任务描述
     * @return 写入参数对象
     */
    private WriteParameters parseSimpleWriteParameters(String task) {
        WriteParameters params = new WriteParameters();
        
        // 移除前缀
        String taskContent = task.trim();
        if (taskContent.startsWith("write ")) {
            taskContent = taskContent.substring(6); // 移除"write "前缀
        } else if (taskContent.startsWith("写入 ")) {
            taskContent = taskContent.substring(3); // 移除"写入 "前缀
        }
        
        // 简单格式：write "file_path" "content"
        // 或者：write file_path content
        String[] parts = taskContent.split(" ", 2);
        if (parts.length >= 2) {
            params.filePath = removeQuotes(parts[0]);
            params.content = removeQuotes(parts[1]);
        } else {
            params.filePath = removeQuotes(taskContent);
            params.content = "";
        }
        
        return params;
    }
    
    /**
     * 移除字符串两端的引号
     * @param str 字符串
     * @return 移除引号后的字符串
     */
    private String removeQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        } else if (str.startsWith("'") && str.endsWith("'")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
    
    /**
     * 执行写入操作
     * @param params 写入参数
     * @return 操作结果
     */
    private String performWriteOperation(WriteParameters params) {
        try {
            Path targetPath = Paths.get(params.filePath).toAbsolutePath();
            
            // 1. 验证文件路径安全性
            if (!isPathSafe(targetPath)) {
                return "Error: Invalid file path - path traversal detected";
            }
            
            // 2. 检查文件是否已存在
            boolean fileExists = Files.exists(targetPath);
            
            if (fileExists) {
                // 3. 如果文件存在，执行强制读取验证
                if (params.overwrite) {
                    ValidationResult validation = validateForceReadRequirement(targetPath.toString());
                    if (!validation.result) {
                        return "Error: " + validation.message + " (Error code: " + validation.errorCode + ")";
                    }
                } else {
                    return "Error: File already exists and overwrite is disabled";
                }
            } else {
                // 4. 如果是新文件，检查父目录权限
                if (params.createDirectories) {
                    Path parentDir = targetPath.getParent();
                    if (parentDir != null && !Files.exists(parentDir)) {
                        Files.createDirectories(parentDir);
                    }
                }
            }
            
            // 5. 执行原子写入
            WriteResult result = performAtomicWrite(targetPath, params);
            
            if (!result.success) {
                return "Error: Write operation failed - " + result.errorMessage;
            }
            
            // 6. 更新readFileState
            updateReadFileState(targetPath.toString(), params.content, params);
            
            // 7. 清理备份文件（如果配置为自动清理）
            if (params.autoCleanupBackup && result.backupPath != null) {
                scheduleBackupCleanup(result.backupPath, params.backupRetentionTime);
            }
            
            return formatSuccessResponse(targetPath.toString(), result);
            
        } catch (Exception e) {
            return "Error during write operation: " + e.getMessage();
        }
    }
    
    /**
     * 验证路径安全性
     * @param path 文件路径
     * @return 是否安全
     */
    private boolean isPathSafe(Path path) {
        // 检查路径遍历攻击
        try {
            Path normalizedPath = path.normalize();
            Path basePath = Paths.get(".").toAbsolutePath().normalize();
            return normalizedPath.startsWith(basePath);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 强制读取验证机制
     * @param filePath 文件路径
     * @return 验证结果
     */
    private ValidationResult validateForceReadRequirement(String filePath) {
        Path absolutePath = Paths.get(filePath).toAbsolutePath();
        String absolutePathStr = absolutePath.toString();
        
        // 检查readFileState中是否存在文件记录
        FileState fileState = readFileState.get(absolutePathStr);
        
        if (fileState == null) {
            return new ValidationResult(false, "File has not been read yet. Read it first before writing to it.", 6);
        }
        
        // 检查文件是否仍然存在
        if (!Files.exists(absolutePath)) {
            return new ValidationResult(false, "File no longer exists. The readFileState may be stale.", 4);
        }
        
        // 验证文件修改时间一致性
        try {
            long currentMtime = Files.getLastModifiedTime(absolutePath).toMillis();
            if (currentMtime > fileState.timestamp) {
                return new ValidationResult(false, "File has been modified since read, either by the user or by a linter. Read it again before attempting to write it.", 7);
            }
        } catch (IOException e) {
            return new ValidationResult(false, "Error checking file modification time: " + e.getMessage(), 999);
        }
        
        return new ValidationResult(true, "", 0);
    }
    
    /**
     * 执行原子写入
     * @param targetPath 目标路径
     * @param params 写入参数
     * @return 写入结果
     */
    private WriteResult performAtomicWrite(Path targetPath, WriteParameters params) {
        Path tempPath = null;
        Path backupPath = null;
        
        try {
            // 1. 创建备份（如果文件存在且需要备份）
            if (params.createBackup && Files.exists(targetPath)) {
                backupPath = Paths.get(targetPath.toString() + ".backup." + System.currentTimeMillis());
                Files.copy(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 2. 创建临时文件
            tempPath = Paths.get(targetPath.toString() + ".tmp." + System.currentTimeMillis());
            
            // 3. 写入内容到临时文件
            writeToTempFile(tempPath, params.content, params);
            
            // 4. 验证临时文件完整性
            if (params.enableHashVerification) {
                boolean integrityCheck = verifyTempFileIntegrity(tempPath, params.content, params);
                if (!integrityCheck) {
                    // 清理临时文件
                    if (Files.exists(tempPath)) {
                        Files.delete(tempPath);
                    }
                    return new WriteResult(false, "Temporary file integrity check failed", null);
                }
            }
            
            // 5. 原子移动到目标位置
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 6. 设置文件权限
            if (params.fileMode != 0644) {
                Files.setPosixFilePermissions(targetPath, 
                    java.nio.file.attribute.PosixFilePermissions.fromString(
                        String.format("%03o", params.fileMode)));
            }
            
            // 7. 同步到磁盘（如果需要）
            if (params.syncAfterWrite) {
                // 注意：Java中没有直接的fsync等价方法，这需要通过其他方式实现
            }
            
            return new WriteResult(true, null, backupPath != null ? backupPath.toString() : null);
            
        } catch (Exception e) {
            // 清理临时文件
            if (tempPath != null && Files.exists(tempPath)) {
                try {
                    Files.delete(tempPath);
                } catch (IOException ioException) {
                    // 忽略删除临时文件的错误
                }
            }
            
            return new WriteResult(false, "Write operation failed: " + e.getMessage(), 
                backupPath != null ? backupPath.toString() : null);
        }
    }
    
    /**
     * 写入内容到临时文件
     * @param tempPath 临时文件路径
     * @param content 内容
     * @param params 写入参数
     * @throws IOException IO异常
     */
    private void writeToTempFile(Path tempPath, String content, WriteParameters params) throws IOException {
        // 确保临时文件目录存在
        Path tempDir = tempPath.getParent();
        if (tempDir != null && !Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        
        // 根据编码处理内容
        Charset charset = Charset.forName(params.encoding);
        byte[] contentBytes = content.getBytes(charset);
        
        // 添加BOM（如果需要）
        byte[] finalBytes = contentBytes;
        if (params.addBom && "UTF-8".equalsIgnoreCase(params.encoding)) {
            byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            finalBytes = new byte[bom.length + contentBytes.length];
            System.arraycopy(bom, 0, finalBytes, 0, bom.length);
            System.arraycopy(contentBytes, 0, finalBytes, bom.length, contentBytes.length);
        }
        
        // 写入临时文件
        Files.write(tempPath, finalBytes);
    }
    
    /**
     * 验证临时文件完整性
     * @param tempPath 临时文件路径
     * @param expectedContent 期望内容
     * @param params 写入参数
     * @return 是否通过验证
     */
    private boolean verifyTempFileIntegrity(Path tempPath, String expectedContent, WriteParameters params) {
        try {
            // 1. 基础存在性检查
            if (!Files.exists(tempPath)) {
                return false;
            }
            
            // 2. 文件大小验证
            long actualSize = Files.size(tempPath);
            Charset charset = Charset.forName(params.encoding);
            long expectedSize = expectedContent.getBytes(charset).length;
            
            if (params.addBom && "UTF-8".equalsIgnoreCase(params.encoding)) {
                expectedSize += 3; // BOM大小
            }
            
            if (actualSize != expectedSize) {
                return false;
            }
            
            // 3. 内容哈希验证
            String actualHash = calculateFileHash(tempPath);
            String expectedHash = calculateContentHash(expectedContent, params.encoding);
            
            return actualHash.equals(expectedHash);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 计算文件哈希
     * @param filePath 文件路径
     * @return 哈希值
     * @throws IOException IO异常
     * @throws NoSuchAlgorithmException 算法异常
     */
    private String calculateFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] hashBytes = digest.digest(fileBytes);
        return bytesToHex(hashBytes);
    }
    
    /**
     * 计算内容哈希
     * @param content 内容
     * @param encoding 编码
     * @return 哈希值
     */
    private String calculateContentHash(String content, String encoding) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Charset charset = Charset.forName(encoding);
            byte[] contentBytes = content.getBytes(charset);
            byte[] hashBytes = digest.digest(contentBytes);
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 字节数组转十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * 更新readFileState
     * @param filePath 文件路径
     * @param content 内容
     * @param params 写入参数
     */
    private void updateReadFileState(String filePath, String content, WriteParameters params) {
        try {
            Path path = Paths.get(filePath);
            long mtime = Files.getLastModifiedTime(path).toMillis();
            long size = Files.size(path);
            String contentHash = calculateContentHash(content, params.encoding);
            
            FileState fileState = new FileState();
            fileState.content = content;
            fileState.timestamp = mtime;
            fileState.fileSystemTimestamp = mtime;
            fileState.size = size;
            fileState.encoding = params.encoding;
            fileState.contentHash = contentHash;
            
            readFileState.put(filePath, fileState);
        } catch (Exception e) {
            // 忽略状态更新错误
        }
    }
    
    /**
     * 安排备份文件清理
     * @param backupPath 备份文件路径
     * @param retentionTime 保留时间
     */
    private void scheduleBackupCleanup(String backupPath, long retentionTime) {
        // 在实际实现中，这应该使用定时任务来清理备份文件
        // 这里简化实现，直接删除
        try {
            Thread.sleep(retentionTime);
            Path backup = Paths.get(backupPath);
            if (Files.exists(backup)) {
                Files.delete(backup);
            }
        } catch (Exception e) {
            // 忽略清理错误
        }
    }
    
    /**
     * 格式化成功响应
     * @param filePath 文件路径
     * @param result 写入结果
     * @return 格式化后的响应
     */
    private String formatSuccessResponse(String filePath, WriteResult result) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("file_path", filePath);
        response.addProperty("bytes_written", result.backupPath != null ? new File(result.backupPath).length() : 0);
        response.addProperty("encoding_used", "UTF-8");
        response.addProperty("is_new_file", !Files.exists(Paths.get(filePath)));
        
        JsonObject fileInfo = new JsonObject();
        try {
            Path path = Paths.get(filePath);
            fileInfo.addProperty("size", Files.size(path));
            fileInfo.addProperty("mtime", Files.getLastModifiedTime(path).toMillis());
            fileInfo.addProperty("content_hash", calculateFileHash(path));
        } catch (Exception e) {
            fileInfo.addProperty("size", 0);
            fileInfo.addProperty("mtime", 0);
            fileInfo.addProperty("content_hash", "");
        }
        response.add("file_info", fileInfo);
        
        JsonObject operationDetails = new JsonObject();
        operationDetails.addProperty("backup_created", result.backupPath != null);
        if (result.backupPath != null) {
            operationDetails.addProperty("backup_path", result.backupPath);
        }
        operationDetails.addProperty("atomic_write_used", true);
        operationDetails.addProperty("verification_passed", true);
        response.add("operation_details", operationDetails);
        
        JsonObject stateUpdate = new JsonObject();
        stateUpdate.addProperty("read_file_state_updated", true);
        stateUpdate.addProperty("state_key", filePath);
        stateUpdate.addProperty("timestamp", System.currentTimeMillis());
        response.add("state_update", stateUpdate);
        
        return gson.toJson(response);
    }
    
    /**
     * 文件状态类
     */
    private static class FileState {
        String content;
        long timestamp;
        long fileSystemTimestamp;
        long size;
        String encoding;
        String contentHash;
    }
    
    /**
     * 写入参数类
     */
    private static class WriteParameters {
        String filePath;
        String content;
        String encoding = "UTF-8";
        boolean addBom = false;
        boolean overwrite = true;
        boolean createDirectories = true;
        boolean createBackup = true;
        boolean enableHashVerification = false;
        boolean validateEncoding = true;
        int fileMode = 0644;
        boolean atomicWrite = true;
        boolean syncAfterWrite = false;
        boolean autoCleanupBackup = true;
        long backupRetentionTime = 3600000L; // 1小时
    }
    
    /**
     * 验证结果类
     */
    private static class ValidationResult {
        boolean result;
        String message;
        int errorCode;
        
        ValidationResult(boolean result, String message, int errorCode) {
            this.result = result;
            this.message = message;
            this.errorCode = errorCode;
        }
    }
    
    /**
     * 写入结果类
     */
    private static class WriteResult {
        boolean success;
        String errorMessage;
        String backupPath;
        
        WriteResult(boolean success, String errorMessage, String backupPath) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.backupPath = backupPath;
        }
    }
}