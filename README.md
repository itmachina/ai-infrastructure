# AI Infrastructure Project

基于Claude Code核心原理的Java 17 AI组件实现

## 项目概述

本项目实现了一个基于Claude Code核心原理的AI基础设施组件，包含以下特性：

1. **分层多Agent架构** - 主Agent协调+子Agent执行的任务隔离模式
2. **实时Steering机制** - 支持用户在Agent执行过程中实时交互和引导
3. **智能任务调度** - 支持并发控制的任务调度器
4. **内存管理机制** - 三层记忆架构（短期/中期/长期）
5. **工具执行引擎** - 6阶段执行流程
6. **安全防护机制** - 6层安全防护架构

## 核心组件

### 1. Agent系统
- `BaseAgent` - Agent基类
- `MainAgent` - 主Agent，负责协调和调度
- `SubAgent` - 子Agent，负责执行专项任务

### 2. 实时Steering系统
- `AsyncMessageQueue` - 异步消息队列，支持非阻塞读取和实时消息入队
- `StreamingMessageParser` - 流式消息解析器，支持JSON格式消息解析
- `StreamingProcessor` - 流式处理引擎，协调消息队列和Agent执行
- `MainAgentLoop` - 主Agent循环，实现完整的Agent生命周期管理
- `RealtimeSteeringSystem` - 实时Steering系统的完整集成

### 3. 任务调度
- `TaskScheduler` - 任务调度器，支持最大10个并发任务
- `ConcurrentExecutor` - 并发执行器，实现Claude Code的UH1并发调度机制

### 4. 内存管理
- `MemoryManager` - 内存管理器，实现三层记忆架构
- `MemoryItem` - 内存项
- `CompressedMemory` - 压缩内存

### 5. 工具执行
- `ToolEngine` - 工具引擎，实现6阶段执行流程
- `ToolExecutor` - 工具执行器接口
- 各种具体工具执行器实现

### 6. 安全防护
- `SecurityManager` - 安全管理器，实现6层安全防护

## 架构特点

### 分层多Agent架构
```
主Agent (协调)
    │
    ├── 子Agent 1 (执行专项任务)
    ├── 子Agent 2 (执行专项任务)
    └── 子Agent N (执行专项任务)
```

### 实时Steering机制架构
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   stdin监听      │───▶│  消息解析器      │───▶│  异步消息队列    │
│  (实时输入)      │    │   (g2A)         │    │    (h2A)        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  AbortController │◀───│   流式处理      │◀───│   Agent循环     │
│   (中断控制)     │    │   (kq5)         │    │    (nO)         │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 三层记忆架构
1. **短期记忆** - 实时消息数组
2. **中期记忆** - 基于8段式结构化压缩
3. **长期记忆** - 持久化存储

### 6阶段工具执行流程
1. 工具发现与验证
2. 输入验证
3. 权限检查
4. 取消检查
5. 工具执行
6. 结果格式化与清理

### 6层安全防护
1. 输入验证层
2. 权限控制层
3. 沙箱隔离层
4. 执行监控层
5. 错误恢复层
6. 审计记录层

## 核心技术实现

### 1. 异步消息队列 (h2A类实现)
```java
public class AsyncMessageQueue<T> implements Iterator<CompletableFuture<QueueMessage<T>>> {
    // 实现AsyncIterator接口
    // 支持非阻塞读取和实时消息入队
    // 完整的状态管理和错误处理
}
```

### 2. 消息解析器 (g2A类实现)
```java
public class StreamingMessageParser implements AutoCloseable {
    // 异步生成器实现流式处理
    // 支持JSON格式消息解析和严格类型验证
    // 错误处理和恢复机制
}
```

### 3. 流式处理引擎 (kq5函数实现)
```java
public class StreamingProcessor implements AutoCloseable {
    // 并发执行协调机制
    // 命令队列管理和状态同步
    // 流式输出和中断处理
}
```

### 4. Agent主循环 (nO函数实现)
```java
public class MainAgentLoop {
    // Async Generator实现
    // 消息压缩和上下文管理
    // 模型降级和错误恢复
}
```

## 运行项目

```bash
# 使用Maven编译项目
mvn compile

# 运行测试
mvn test

# 打包项目
mvn package

# 运行主应用
mvn exec:java -Dexec.mainClass="com.ai.infrastructure.Main"

# 运行实时Steering演示
mvn exec:java -Dexec.mainClass="com.ai.infrastructure.RealtimeSteeringDemo"
```

## 技术栈

- Java 17
- CompletableFuture (异步编程)
- 并发工具 (ExecutorService, Semaphore)
- Gson (JSON处理)
- JUnit 5 (测试框架)
- SLF4J/Logback (日志框架)

## 扩展性

该架构设计具有良好的扩展性，可以通过以下方式扩展：

1. 添加新的工具执行器
2. 实现新的Agent类型
3. 扩展内存管理策略
4. 增强安全防护机制
5. 实现新的消息解析格式
6. 添加更多的并发调度策略

## 安全特性

1. **工具白名单机制** - SubAgent只能访问预定义的安全工具集合
2. **递归调用防护** - 严格禁止Task工具的递归调用
3. **资源使用监控** - 实时监控Token使用量、执行时间和工具调用次数
4. **输入验证和过滤** - 严格的输入验证和危险字符过滤
5. **权限控制** - 细粒度的工具权限验证和访问控制
6. **错误隔离** - 完整的错误处理和隔离机制

## 性能优化

1. **上下文压缩技术** - 智能的对话历史压缩算法
2. **并发执行优化** - 基于工具特性的并发安全评估
3. **内存管理优化** - 三层记忆架构和智能压缩算法
4. **非阻塞设计** - 完整的异步非阻塞消息处理机制