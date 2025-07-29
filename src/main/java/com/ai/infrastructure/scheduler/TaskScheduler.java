package com.ai.infrastructure.scheduler;

import java.util.concurrent.*;
import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

/**
 * 任务调度器，支持并发控制和抢占式调度
 * 基于Claude Code的调度机制实现优先级调度和任务抢占
 */
public class TaskScheduler {
    private final ExecutorService executorService;
    private final Semaphore concurrencyLimiter;
    private final int maxConcurrency;
    
    // 任务管理
    private final Map<String, RunningTask> runningTasks;
    private final PriorityBlockingQueue<ScheduledTask> taskQueue;
    
    public TaskScheduler(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
        this.executorService = Executors.newCachedThreadPool();
        this.concurrencyLimiter = new Semaphore(maxConcurrency);
        this.runningTasks = new ConcurrentHashMap<>();
        this.taskQueue = new PriorityBlockingQueue<>(11, Comparator.comparingInt(ScheduledTask::getPriority).reversed());
    }
    
    /**
     * 调度任务执行（向后兼容）
     * @param task 任务描述
     * @param taskProcessor 任务处理器
     * @return 执行结果
     */
    public String scheduleTask(String task, Function<String, String> taskProcessor) {
        return scheduleTask(task, taskProcessor, 0); // 默认优先级
    }
    
    /**
     * 调度任务执行（支持优先级）
     * @param task 任务描述
     * @param taskProcessor 任务处理器
     * @param priority 任务优先级（数值越大优先级越高）
     * @return 执行结果
     */
    public String scheduleTask(String task, Function<String, String> taskProcessor, int priority) {
        String taskId = "task_" + System.currentTimeMillis() + "_" + task.hashCode();
        return scheduleTaskWithId(taskId, task, taskProcessor, priority);
    }
    
    /**
     * 调度任务执行（支持任务ID）
     * @param taskId 任务ID
     * @param task 任务描述
     * @param taskProcessor 任务处理器
     * @param priority 任务优先级（数值越大优先级越高）
     * @return 执行结果
     */
    public String scheduleTaskWithId(String taskId, String task, Function<String, String> taskProcessor, int priority) {
        try {
            // 创建调度任务
            ScheduledTask scheduledTask = new ScheduledTask(taskId, task, taskProcessor, priority);
            taskQueue.offer(scheduledTask);
            
            // 获取执行许可
            concurrencyLimiter.acquire();
            
            // 从队列中获取最高优先级任务
            ScheduledTask currentTask = taskQueue.poll();
            
            // 创建运行中的任务对象
            RunningTask runningTask = new RunningTask(currentTask.getTaskId(), null);
            runningTasks.put(currentTask.getTaskId(), runningTask);
            
            // 提交任务执行
            Future<String> future = executorService.submit(() -> {
                try {
                    // 更新线程信息
                    runningTask.setThread(Thread.currentThread());
                    
                    // 检查是否被抢占
                    if (isPreempted(currentTask.getTaskId())) {
                        return "Task preempted: " + currentTask.getTask();
                    }
                    
                    // 执行任务
                    return currentTask.getTaskProcessor().apply(currentTask.getTask());
                } finally {
                    // 移除运行中的任务
                    runningTasks.remove(currentTask.getTaskId());
                    // 释放执行许可
                    concurrencyLimiter.release();
                }
            });
            
            // 注册Future以便取消
            runningTask.setFuture(future);
            
            // 等待并返回结果
            return future.get(120, TimeUnit.SECONDS); // 120秒超时
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Task execution interrupted: " + e.getMessage();
        } catch (ExecutionException e) {
            return "Task execution failed: " + e.getCause().getMessage();
        } catch (TimeoutException e) {
            return "Task execution timed out: " + e.getMessage();
        }
    }
    
    /**
     * 检查任务是否被抢占
     * @param taskId 任务ID
     * @return 是否被抢占
     */
    private boolean isPreempted(String taskId) {
        // 简化的抢占检查逻辑
        // 在实际实现中，可以根据系统负载、更高优先级任务等条件判断
        return false;
    }
    
    /**
     * 取消任务执行
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelTask(String taskId) {
        RunningTask runningTask = runningTasks.get(taskId);
        if (runningTask != null && runningTask.getFuture() != null) {
            boolean cancelled = runningTask.getFuture().cancel(true);
            if (cancelled) {
                // 中断执行任务的线程
                runningTask.getThread().interrupt();
                runningTasks.remove(taskId);
            }
            return cancelled;
        }
        return false;
    }
    
    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态
     */
    public String getTaskStatus(String taskId) {
        if (runningTasks.containsKey(taskId)) {
            return "RUNNING";
        }
        
        // 检查队列中是否有该任务
        for (ScheduledTask task : taskQueue) {
            if (task.getTaskId().equals(taskId)) {
                return "QUEUED";
            }
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 获取最大并发数
     * @return 最大并发数
     */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }
    
    /**
     * 获取当前运行的任务数
     * @return 当前运行的任务数
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }
    
    /**
     * 获取队列中的任务数
     * @return 队列中的任务数
     */
    public int getQueuedTaskCount() {
        return taskQueue.size();
    }
    
    /**
     * 关闭调度器
     */
    public void shutdown() {
        // 取消所有运行中的任务
        for (RunningTask task : runningTasks.values()) {
            if (task.getFuture() != null) {
                task.getFuture().cancel(true);
            }
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 正在运行的任务
     */
    private static class RunningTask {
        private final String taskId;
        private Thread thread;
        private Future<String> future;
        
        public RunningTask(String taskId, Thread thread) {
            this.taskId = taskId;
            this.thread = thread;
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public Thread getThread() {
            return thread;
        }
        
        public void setThread(Thread thread) {
            this.thread = thread;
        }
        
        public Future<String> getFuture() {
            return future;
        }
        
        public void setFuture(Future<String> future) {
            this.future = future;
        }
    }
    
    /**
     * 调度任务
     */
    private static class ScheduledTask {
        private final String taskId;
        private final String task;
        private final Function<String, String> taskProcessor;
        private final int priority;
        
        public ScheduledTask(String taskId, String task, Function<String, String> taskProcessor, int priority) {
            this.taskId = taskId;
            this.task = task;
            this.taskProcessor = taskProcessor;
            this.priority = priority;
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public String getTask() {
            return task;
        }
        
        public Function<String, String> getTaskProcessor() {
            return taskProcessor;
        }
        
        public int getPriority() {
            return priority;
        }
    }
}