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
7. **OpenAI风格大模型集成** - 支持心流开放平台的OpenAI兼容API接口
8. **智能任务分发机制** - 基于大模型的智能决策能力
9. **持续执行和对话管理** - 支持多轮对话和任务的连续执行

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
- `TaskScheduler` - 任务调度器，支持并发控制和任务队列管理
- `ConcurrentExecutor` - 并发执行器，支持任务的并发执行

### 4. 内存管理
- `MemoryManager` - 内存管理器，实现三层记忆架构
- `MemoryItem` - 内存项，表示单个记忆单元
- `CompressedMemory` - 压缩内存，实现对话历史的智能压缩

### 5. 工具引擎
- `ToolEngine` - 工具引擎，实现6阶段执行流程
- `ToolExecutor` - 工具执行器接口
- `ReadToolExecutor` - 读取工具执行器
- `WriteToolExecutor` - 写入工具执行器
- `SearchToolExecutor` - 搜索工具执行器
- `CalculateToolExecutor` - 计算工具执行器
- `WebSearchToolExecutor` - 网页搜索工具执行器
- `OpenAIStyleModelToolExecutor` - OpenAI风格大模型工具执行器

### 6. 安全管理
- `SecurityManager` - 安全管理器，实现6层安全防护架构

### 7. 对话和持续执行
- `ConversationManager` - 对话管理器，管理对话历史和消息构建
- `ContinuousExecutionManager` - 持续执行管理器，管理任务的连续执行

### 8. 大模型客户端
- `OpenAIModelClient` - OpenAI风格大模型客户端，独立封装的心流开放平台API调用客户端

## 功能特性

### 1. OpenAI风格大模型集成
- 支持心流开放平台的OpenAI兼容API接口
- 集成`Qwen3-235B-A22B-Thinking-2507`模型
- 完整的API调用流程实现

### 2. 智能任务分发机制
- 基于大模型的智能决策能力
- 自动选择最佳处理方式：
  - DIRECT: 直接回答简单问题
  - TOOL: 调用工具执行具体操作
  - SUBAGENT: 创建子Agent处理复杂任务

### 3. 丰富的工具集
- **read**: 读取文件内容
- **write**: 写入文件内容
- **search**: 本地搜索
- **web_search**: 网页搜索（使用百度搜索）
- **calculate**: 数学计算

### 3. 持续执行和对话管理
- 支持多轮对话和任务的连续执行
- 对话历史管理
- 执行步骤控制和超时机制

### 4. 安全特性
- 完整的输入验证和安全检查
- API密钥安全管理和保护
- 命令注入检测和防护

## 系统架构

```
AIInfrastructureApplication (应用入口)
├── MainAgent (主Agent)
│   ├── ContinuousExecutionManager (持续执行管理器)
│   │   ├── ConversationManager (对话管理器)
│   │   └── TaskScheduler (任务调度器)
│   ├── ToolEngine (工具引擎)
│   ├── MemoryManager (内存管理器)
│   └── SecurityManager (安全管理器)
└── SubAgent (子Agent)
```

## 快速开始

### 编译项目
```bash
mvn compile
```

### 运行测试
```bash
mvn test
```

### 打包项目
```bash
mvn package
```

### 运行主应用
```bash
# 不带API密钥运行（使用回退机制）
mvn exec:java

# 带API密钥运行（启用大模型功能）
mvn exec:java -Dexec.args="YOUR_API_KEY_HERE"

# 通过环境变量设置API密钥
export OPENAI_MODEL_API_KEY="your-api-key-here"
mvn exec:java
```

### 运行OpenAI模型使用示例
```bash
# 不带API密钥运行（使用回退机制）
mvn exec:java -Dexec.mainClass="com.ai.infrastructure.OpenAIModelUsageExample"

# 带API密钥运行（启用大模型功能）
mvn exec:java -Dexec.mainClass="com.ai.infrastructure.OpenAIModelUsageExample" -Dexec.args="YOUR_API_KEY_HERE"
```

### 运行演示示例
```bash
mvn exec:java@run-demo -Dexec.args="YOUR_API_KEY_HERE"
```

## 配置选项

### API密钥配置
1. 命令行参数方式：`mvn exec:java -Dexec.args="YOUR_API_KEY_HERE"`
2. 环境变量方式：`export OPENAI_MODEL_API_KEY="your-api-key-here"`

### 网页搜索工具配置
web_search工具使用百度搜索，无需额外配置API密钥。

### 模型参数配置
- 默认模型：`Qwen3-235B-A22B-Thinking-2507`
- 默认温度：`0.7`
- 默认最大token数：`1000`

## 使用示例

### 基本使用
```java
AIInfrastructureApplication application = new AIInfrastructureApplication();
application.setOpenAIModelApiKey("YOUR_API_KEY_HERE");

// 执行任务
CompletableFuture<String> result = application.executeTask("分析机器学习的发展趋势");
String response = result.join();
```

### 使用网页搜索工具
```java
AIInfrastructureApplication application = new AIInfrastructureApplication();
application.setOpenAIModelApiKey("YOUR_API_KEY_HERE");

// 执行网页搜索任务
CompletableFuture<String> result = application.executeTask("web_search 2025年最新的人工智能技术发展趋势");
String response = result.join();
```

### 交互式模式
```bash
java -jar ai-infrastructure.jar YOUR_API_KEY_HERE
# 系统将启动交互式模式，支持多轮对话和持续执行
```

## 文档

- [Agent核心模型集成总结](docs/agent_core_model_integration_summary.md)
- [OpenAI模型集成详细文档](docs/openai_model_integration.md)
- [智能任务分发机制](docs/intelligent_task_dispatch.md)
- [持续执行和对话管理](docs/continuous_execution_and_conversation_management.md)

## 依赖项

- Java 17+
- Gson 2.10.1 (JSON处理)
- SLF4J 2.0.6 (日志记录)
- Logback 1.4.5 (日志实现)
- JUnit 5.9.2 (测试框架)
- Mockito 5.1.1 (测试框架)

## 未来改进方向

1. 实现流式响应处理
2. 添加智能重试逻辑
3. 实现响应缓存提高性能
4. 添加API调用成本跟踪
5. 支持多模型端点负载均衡
6. 扩展内存管理策略
7. 增强安全防护机制
8. 实现新的消息解析格式
9. 添加更多的并发调度策略
10. 更智能的对话管理
11. 任务状态持久化

## 安全特性

1. **工具白名单机制** - SubAgent只能访问预定义的安全工具集合
2. **递归调用防护** - 严格禁止Task工具的递归调用
3. **资源使用监控** - 实时监控Token使用量、执行时间和工具调用次数
4. **输入验证和过滤** - 严格的输入验证和危险字符过滤
5. **权限控制** - 细粒度的工具权限验证和访问控制
6. **错误隔离** - 完整的错误处理和隔离机制
7. **API密钥保护** - 安全的API密钥管理和存储
8. **命令注入防护** - 基于LLM的命令注入检测

## 性能优化

1. **上下文压缩技术** - 智能的对话历史压缩算法
2. **并发执行优化** - 基于工具特性的并发安全评估
3. **内存管理优化** - 三层记忆架构和智能压缩算法
4. **非阻塞设计** - 完整的异步非阻塞消息处理机制
5. **持续执行控制** - 执行步骤限制和超时控制