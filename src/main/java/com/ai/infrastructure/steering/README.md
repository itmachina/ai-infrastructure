# 实时Steering机制Java实现

## 概述

本项目实现了基于Claude Code的实时Steering机制的Java 17版本。实时Steering机制是一种先进的异步消息处理系统，支持真正的实时消息处理、非阻塞的异步执行以及完整的中断和恢复机制。

## 核心组件

### 1. AsyncMessageQueue (异步消息队列)
基于Claude Code的h2A类实现，支持：
- 实时消息入队和非阻塞读取
- 异步读取操作（CompletableFuture）
- 完成状态和错误状态管理
- 线程安全的操作

### 2. StreamingMessageParser (流式消息解析器)
基于Claude Code的g2A类实现，支持：
- 流式消息解析
- 实时输入处理
- 消息格式验证

### 3. StreamingProcessor (流式处理引擎)
基于Claude Code的kq5类实现，支持：
- 命令队列管理
- 异步任务执行
- 与主Agent的集成

### 4. MainAgentLoop (主Agent循环)
基于Claude Code的nO函数实现，支持：
- 可中断的流式处理
- 消息压缩机制
- 模型降级处理

### 5. RealtimeSteeringSystem (完整集成)
完整的实时Steering系统集成，支持：
- 端到端的消息处理
- 系统中断和恢复
- 资源管理

## 技术特性

### 1. 异步非阻塞处理
- 使用CompletableFuture实现异步操作
- 非阻塞的消息入队和出队
- 支持高并发处理

### 2. 实时消息处理
- 支持实时消息入队
- 等待读取的优先处理机制
- 流式输入输出处理

### 3. 完整的状态管理
- 完成状态标记
- 错误状态处理
- 中断信号支持

### 4. 线程安全
- 使用并发安全的数据结构
- 原子操作状态管理
- 线程安全的队列操作

## 使用示例

```java
// 创建实时Steering系统
try (RealtimeSteeringSystem system = new RealtimeSteeringSystem()) {
    system.start();
    
    // 发送输入消息
    system.sendInput("Hello, World!");
    
    // 发送命令
    system.sendCommand(new Command("prompt", "Calculate 2+2"));
    
    // 读取输出
    AsyncMessageQueue<Object> outputQueue = system.getOutputQueue();
    CompletableFuture<QueueMessage<Object>> future = outputQueue.read();
    QueueMessage<Object> result = future.join();
    System.out.println("Result: " + result.getValue());
}
```

## 测试验证

通过完整的测试套件验证了所有核心功能：
1. 基本入队和出队测试
2. 异步读取测试
3. 完成状态测试
4. 错误状态测试
5. 完整系统集成测试

所有测试均通过，验证了系统的稳定性和功能完整性。

## 扩展性

该实现具有良好的扩展性：
- 模块化设计，各组件职责清晰
- 易于集成到现有系统
- 支持自定义消息类型
- 可扩展的处理逻辑

## 依赖关系

- Java 17或更高版本
- 无外部依赖（纯Java实现）

## 总结

本实现成功还原了Claude Code实时Steering机制的核心功能，为Java开发者提供了可行的技术路径和具体的代码参考。该实现可以作为构建更复杂AI应用的基础框架。