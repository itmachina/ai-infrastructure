package com.ai.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TaskSchedulerTest {
    
    private TaskScheduler scheduler;
    
    @BeforeEach
    void setUp() {
        scheduler = new TaskScheduler(3); // Allow up to 3 concurrent tasks
    }
    
    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }
    
    @Test
    void testBasicTaskScheduling() {
        Function<String, String> processor = (task) -> "Processed: " + task;
        String result = scheduler.scheduleTask("test task", processor);
        assertEquals("Processed: test task", result);
    }
    
    @Test
    void testPriorityBasedScheduling() {
        // This test verifies that tasks can be scheduled with priorities
        Function<String, String> processor = (task) -> "Processed: " + task;
        
        // Schedule a low priority task
        CompletableFuture<String> lowPriorityResult = CompletableFuture.supplyAsync(() -> 
            scheduler.scheduleTask("low priority task", processor, 1));
        
        // Schedule a high priority task
        CompletableFuture<String> highPriorityResult = CompletableFuture.supplyAsync(() -> 
            scheduler.scheduleTask("high priority task", processor, 10));
        
        // Both should complete successfully
        assertDoesNotThrow(() -> {
            lowPriorityResult.get(5, TimeUnit.SECONDS);
            highPriorityResult.get(5, TimeUnit.SECONDS);
        });
    }
    
    @Test
    void testConcurrentTaskExecution() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        Function<String, String> processor = (task) -> {
            try {
                // Simulate some work
                Thread.sleep(100);
                latch.countDown();
                return "Processed: " + task;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Interrupted: " + task;
            }
        };
        
        // Schedule multiple tasks concurrently
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> 
            scheduler.scheduleTask("task 1", processor));
        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> 
            scheduler.scheduleTask("task 2", processor));
        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> 
            scheduler.scheduleTask("task 3", processor));
        
        // Wait for all tasks to complete
        CompletableFuture.allOf(task1, task2, task3).get(5, TimeUnit.SECONDS);
        
        // Verify that all tasks completed
        assertEquals(0, latch.getCount());
    }
    
    @Test
    void testTaskCancellation() {
        Function<String, String> processor = (task) -> {
            try {
                // Simulate long running task
                Thread.sleep(5000);
                return "Processed: " + task;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Interrupted: " + task;
            }
        };
        
        // Schedule a task with ID
        String taskId = "test_task_1";
        CompletableFuture<String> taskFuture = CompletableFuture.supplyAsync(() -> 
            scheduler.scheduleTaskWithId(taskId, "long running task", processor, 0));
        
        // Give the task a moment to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Cancel the task
        boolean cancelled = scheduler.cancelTask(taskId);
        assertTrue(cancelled, "Task should be cancelled successfully");
        
        // Verify the task was cancelled
        assertThrows(java.util.concurrent.ExecutionException.class, () -> {
            taskFuture.get(5, TimeUnit.SECONDS);
        }, "Cancelled task should throw ExecutionException");
    }
    
    @Test
    void testTaskStatus() {
        Function<String, String> processor = (task) -> {
            try {
                Thread.sleep(200);
                return "Processed: " + task;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Interrupted: " + task;
            }
        };
        
        String taskId = "status_test_task";
        
        // Schedule a task
        CompletableFuture<String> taskFuture = CompletableFuture.supplyAsync(() -> 
            scheduler.scheduleTaskWithId(taskId, "status test task", processor, 0));
        
        // Check task status
        String status = scheduler.getTaskStatus(taskId);
        assertNotNull(status);
        
        // Wait for task completion
        assertDoesNotThrow(() -> taskFuture.get(5, TimeUnit.SECONDS));
    }
    
    @Test
    void testSchedulerMetrics() {
        assertEquals(3, scheduler.getMaxConcurrency());
        assertEquals(0, scheduler.getRunningTaskCount());
        assertEquals(0, scheduler.getQueuedTaskCount());
    }
}