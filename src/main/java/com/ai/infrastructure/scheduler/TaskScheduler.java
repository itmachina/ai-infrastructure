package com.ai.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 任务调度器，支持并发控制和抢占式调度
 * 基于Claude Code的调度机制实现优先级调度和任务抢占
 */
public class TaskScheduler {
    private static final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);
    
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
        logger.debug("Scheduling task with ID: {}, priority: {}", taskId, priority);
        
        try {
            // 创建调度任务
            ScheduledTask scheduledTask = new ScheduledTask(taskId, task, taskProcessor, priority);
            taskQueue.offer(scheduledTask);
            logger.debug("Task added to queue: {}", taskId);
            
            // 获取执行许可
            concurrencyLimiter.acquire();
            logger.debug("Acquired concurrency permit for task: {}", taskId);
            
            // 从队列中获取最高优先级任务
            ScheduledTask currentTask = taskQueue.poll();
            logger.debug("Retrieved task from queue: {}", currentTask.getTaskId());
            
            // 创建运行中的任务对象
            RunningTask runningTask = new RunningTask(currentTask.getTaskId(), null);
            runningTasks.put(currentTask.getTaskId(), runningTask);
            logger.debug("Registered running task: {}", currentTask.getTaskId());
            
            // 提交任务执行
            Future<String> future = executorService.submit(() -> {
                try {
                    logger.debug("Starting execution of task: {}", currentTask.getTaskId());
                    
                    // 更新线程信息
                    runningTask.setThread(Thread.currentThread());
                    
                    // 检查是否被抢占
                    if (isPreempted(currentTask.getTaskId())) {
                        logger.info("Task preempted: {}", currentTask.getTaskId());
                        return "Task preempted: " + currentTask.getTask();
                    }
                    
                    // 执行任务
                    String result = currentTask.getTaskProcessor().apply(currentTask.getTask());
                    logger.debug("Task execution completed: {}", currentTask.getTaskId());
                    return result;
                } catch (Exception e) {
                    logger.error("Error executing task: {}", currentTask.getTaskId(), e);
                    return "Task execution failed: " + e.getMessage();
                } finally {
                    // 移除运行中的任务
                    runningTasks.remove(currentTask.getTaskId());
                    logger.debug("Removed running task: {}", currentTask.getTaskId());
                    
                    // 释放执行许可
                    concurrencyLimiter.release();
                    logger.debug("Released concurrency permit for task: {}", currentTask.getTaskId());
                }
            });
            
            // 注册Future以便取消
            runningTask.setFuture(future);
            logger.debug("Registered future for task: {}", currentTask.getTaskId());
            
            // 等待并返回结果
            String result = future.get(120, TimeUnit.SECONDS); // 120秒超时
            logger.debug("Task execution result for {}: {}", currentTask.getTaskId(), result);
            return result;
        } catch (InterruptedException e) {
            logger.warn("Task execution interrupted: {}", taskId, e);
            Thread.currentThread().interrupt();
            return "Task execution interrupted: " + e.getMessage();
        } catch (ExecutionException e) {
            logger.error("Task execution failed: {}", taskId, e);
            return "Task execution failed: " + e.getCause().getMessage();
        } catch (TimeoutException e) {
            logger.warn("Task execution timed out: {}", taskId, e);
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
        logger.debug("Attempting to cancel task: {}", taskId);
        
        RunningTask runningTask = runningTasks.get(taskId);
        if (runningTask != null && runningTask.getFuture() != null) {
            boolean cancelled = runningTask.getFuture().cancel(true);
            logger.debug("Task cancellation result for {}: {}", taskId, cancelled);
            
            if (cancelled) {
                // 中断执行任务的线程
                if (runningTask.getThread() != null) {
                    runningTask.getThread().interrupt();
                    logger.debug("Interrupted thread for task: {}", taskId);
                }
                runningTasks.remove(taskId);
                logger.debug("Removed task from running tasks: {}", taskId);
            }
            return cancelled;
        }
        
        logger.debug("Task not found or not running: {}", taskId);
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
        logger.info("Shutting down TaskScheduler");
        
        // 取消所有运行中的任务
        logger.debug("Cancelling {} running tasks", runningTasks.size());
        for (RunningTask task : runningTasks.values()) {
            if (task.getFuture() != null) {
                boolean cancelled = task.getFuture().cancel(true);
                logger.debug("Cancelled task {}: {}", task.getTaskId(), cancelled);
            }
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Executor service did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            } else {
                logger.info("Executor service terminated gracefully");
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor service termination", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("TaskScheduler shutdown completed");
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