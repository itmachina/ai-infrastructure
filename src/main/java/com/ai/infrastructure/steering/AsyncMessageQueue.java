package com.ai.infrastructure.steering;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 异步消息队列 - 支持实时消息入队和非阻塞读取
 * 基于Claude Code的h2A类实现，完整实现AsyncIterator接口
 */
public class AsyncMessageQueue<T> implements Iterator<CompletableFuture<QueueMessage<T>>> {
    private final Queue<T> messageQueue;
    private final Queue<CompletableFuture<QueueMessage<T>>> pendingReads;
    private final AtomicBoolean isCompleted;
    private final AtomicBoolean hasError;
    private volatile Exception errorState;
    private final AtomicBoolean hasStarted;
    private final Runnable cleanupCallback;
    
    public AsyncMessageQueue() {
        this(null);
    }
    
    public AsyncMessageQueue(Runnable cleanupCallback) {
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.pendingReads = new ConcurrentLinkedQueue<>();
        this.isCompleted = new AtomicBoolean(false);
        this.hasError = new AtomicBoolean(false);
        this.hasStarted = new AtomicBoolean(false);
        this.cleanupCallback = cleanupCallback;
    }
    
    /**
     * 实现Iterator接口，支持异步迭代
     * @return boolean
     */
    @Override
    public boolean hasNext() {
        // 对于异步迭代器，总是返回true，因为我们不知道何时会有下一个元素
        return !isCompleted.get() || !messageQueue.isEmpty();
    }
    
    /**
     * 实现Iterator接口，返回下一个CompletableFuture
     * @return CompletableFuture<QueueMessage<T>>
     */
    @Override
    public CompletableFuture<QueueMessage<T>> next() {
        if (!hasStarted.compareAndSet(false, true) && !hasNext()) {
            throw new NoSuchElementException("No more elements in the async queue");
        }
        return read();
    }
    
    /**
     * 消息入队 - 支持实时插入
     * @param message 消息
     */
    public void enqueue(T message) {
        if (isCompleted.get() || hasError.get()) {
            return;
        }
        
        // 检查是否有等待的读取
        CompletableFuture<QueueMessage<T>> pendingRead = pendingReads.poll();
        if (pendingRead != null) {
            // 有等待的读取，直接返回消息
            pendingRead.complete(new QueueMessage<>(false, message));
        } else {
            // 推入队列缓冲
            messageQueue.offer(message);
        }
    }
    
    /**
     * 异步读取消息 - 核心方法实现非阻塞读取
     * @return CompletableFuture<QueueMessage<T>>
     */
    public CompletableFuture<QueueMessage<T>> read() {
        // 优先处理队列中的消息
        T message = messageQueue.poll();
        if (message != null) {
            return CompletableFuture.completedFuture(new QueueMessage<>(false, message));
        }
        
        // 队列已完成
        if (isCompleted.get()) {
            return CompletableFuture.completedFuture(new QueueMessage<>(true, null));
        }
        
        // 有错误状态
        if (hasError.get()) {
            CompletableFuture<QueueMessage<T>> future = new CompletableFuture<>();
            future.completeExceptionally(errorState);
            return future;
        }
        
        // 等待新消息 - 关键的非阻塞机制
        CompletableFuture<QueueMessage<T>> future = new CompletableFuture<>();
        pendingReads.offer(future);
        return future;
    }
    
    /**
     * 标记队列完成
     */
    public void complete() {
        if (isCompleted.compareAndSet(false, true)) {
            // 完成所有等待的读取
            CompletableFuture<QueueMessage<T>> pendingRead;
            while ((pendingRead = pendingReads.poll()) != null) {
                pendingRead.complete(new QueueMessage<>(true, null));
            }
        }
    }
    
    /**
     * 设置错误状态
     * @param error 错误
     */
    public void error(Exception error) {
        if (hasError.compareAndSet(false, true)) {
            this.errorState = error;
            // 拒绝所有等待的读取
            CompletableFuture<QueueMessage<T>> pendingRead;
            while ((pendingRead = pendingReads.poll()) != null) {
                pendingRead.completeExceptionally(error);
            }
        }
    }
    
    /**
     * 返回是否已完成
     * @return boolean
     */
    public boolean isCompleted() {
        return isCompleted.get();
    }
    
    /**
     * 返回是否有错误
     * @return boolean
     */
    public boolean hasError() {
        return hasError.get();
    }
    
    /**
     * 检查是否已启动（用于Iterator接口）
     * @return boolean
     */
    public boolean hasStarted() {
        return hasStarted.get();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (cleanupCallback != null) {
            cleanupCallback.run();
        }
        messageQueue.clear();
        pendingReads.clear();
    }
    
    /**
     * 获取队列大小
     * @return int
     */
    public int size() {
        return messageQueue.size();
    }
    
    /**
     * 检查队列是否为空
     * @return boolean
     */
    public boolean isEmpty() {
        return messageQueue.isEmpty() && pendingReads.isEmpty();
    }
}