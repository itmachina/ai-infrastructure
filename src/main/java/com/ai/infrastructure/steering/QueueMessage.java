package com.ai.infrastructure.steering;

/**
 * 队列消息封装类
 * @param <T> 消息类型
 */
public class QueueMessage<T> {
    private final boolean done;
    private final T value;
    
    public QueueMessage(boolean done, T value) {
        this.done = done;
        this.value = value;
    }
    
    /**
     * 是否完成
     * @return boolean
     */
    public boolean isDone() {
        return done;
    }
    
    /**
     * 获取消息值
     * @return T
     */
    public T getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return "QueueMessage{done=" + done + ", value=" + value + "}";
    }
}