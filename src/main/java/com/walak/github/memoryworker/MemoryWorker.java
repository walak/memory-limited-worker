package com.walak.github.memoryworker;

import com.walak.github.memoryworker.task.MemoryWorkerTask;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class MemoryWorker<O, T extends MemoryWorkerTask<O>> implements Runnable {
    private static final Logger LOG = Logger.getLogger(MemoryWorker.class.getSimpleName());

    private BlockingQueue<T> tasks;
    private List<O> results;
    private AtomicBoolean isRunning;
    private AtomicInteger taskCounter;

    public MemoryWorker(int jobQueueCapacity) {
        this.tasks = new LinkedBlockingQueue<T>(jobQueueCapacity);
        this.taskCounter = new AtomicInteger(0);
        this.isRunning = new AtomicBoolean(true);
        this.results = new LinkedList<O>();
    }

    public boolean tryAddTask(T task) {
        boolean resultOfAdding = tasks.offer(task);
        if (resultOfAdding) {
            LOG.fine("Task added to queue. Remaining capacity: " + tasks.remainingCapacity());
        } else {
            LOG.fine("Failed to add task to queue. Capacity drained.");
        }
        return resultOfAdding;
    }

    public boolean addTask(T task) {
        try {
            tasks.put(task);
            LOG.fine("Task added to queue. Remaining capacity: " + tasks.remainingCapacity());
            return true;
        } catch (InterruptedException e) {
            LOG.info("Thread interrupted during adding a task to queue.");
            throw new RuntimeException("It should not happen!", e);
        }
    }

    public void run() {
        LOG.info("MemoryWorker started!");
        while (isRunning.get()) {
            T task = tasks.poll();
            Optional<O> result = executeTaskCatchingAnyErrors(task);
            result.ifPresent(results::add);
        }
    }

    private Optional<O> executeTaskCatchingAnyErrors(T task) {
        try {
            Optional<O> result = Optional.of(task.execute());
            LOG.fine("Task successfully executed");
            return result;
        } catch (Throwable e) {
            LOG.warning("Task execution finished with error: " + e.getMessage());
            return Optional.empty();
        }
    }
}
