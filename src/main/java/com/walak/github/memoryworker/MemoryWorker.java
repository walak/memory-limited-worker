package com.walak.github.memoryworker;

import com.walak.github.memoryworker.task.MemoryWorkerTask;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class MemoryWorker<O> implements Runnable {
    private static final Logger LOG = Logger.getLogger(MemoryWorker.class.getSimpleName());
    private static final double ALLOWED_MEMORY_FILL_RATIO = 0.9;

    private BlockingQueue<MemoryWorkerTask<O>> tasks;
    private List<O> results;
    private AtomicBoolean isRunning;
    private AtomicInteger taskCounter;
    private List<MemoryFullHandler<O>> memoryFullHandlers;
    private List<MemoryFullHandler<O>> mandatoryHandlers;

    public MemoryWorker(int jobQueueCapacity) {
        this.tasks = new LinkedBlockingQueue<>(jobQueueCapacity);
        this.taskCounter = new AtomicInteger(0);
        this.isRunning = new AtomicBoolean(true);
        this.results = new LinkedList<>();
        this.memoryFullHandlers = Collections.synchronizedList(new LinkedList<>());
        this.mandatoryHandlers = new LinkedList<>();
        this.mandatoryHandlers.add(new GCRunHandler());
    }

    public boolean tryAddTask(MemoryWorkerTask<O> task) {
        boolean resultOfAdding = tasks.offer(task);
        if (resultOfAdding) {
            LOG.fine("Task added to queue. Remaining capacity: " + tasks.remainingCapacity());
        } else {
            LOG.fine("Failed to add task to queue. Capacity drained.");
        }
        return resultOfAdding;
    }

    public boolean addTask(MemoryWorkerTask<O> task) {
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
            checkMemory();
            MemoryWorkerTask<O> task = getNextTaskBlocking();
            Optional<O> result = executeTaskCatchingAnyErrors(task);
            result.ifPresent(r -> {
                taskCounter.incrementAndGet();
                results.add(r);
            });
            checkMemory();
        }
    }

    private MemoryWorkerTask<O> getNextTaskBlocking() {
        try {
            return tasks.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    private Optional<O> executeTaskCatchingAnyErrors(MemoryWorkerTask<O> task) {
        try {
            Optional<O> result = Optional.of(task.call());
            LOG.fine("Task successfully executed");
            Thread.currentThread().setName("MemoryWorker-" + taskCounter.get() + "-done");
            taskCounter.incrementAndGet();
            return result;
        } catch (Throwable e) {
            LOG.warning("Task execution finished with error: " + e.getMessage());
            return Optional.empty();
        }
    }

    private void executeHandleCatchingAnyError(MemoryFullHandler<O> handler, List<O> results) {
        try {
            handler.onMemoryFull(results);
        } catch (Throwable e) {
            String message = String.format("Error executing handler %s: %s",
                    handler.getClass().getName(),
                    e.getMessage());
            LOG.warning(message);
        }
    }

    public List<O> pullResults() {
        List<O> resultsToReturn = this.results;
        this.results = new LinkedList<O>();
        return resultsToReturn;
    }

    private void checkMemory() {
        if (getMemoryFillRatio() >= ALLOWED_MEMORY_FILL_RATIO) {
            LOG.info(String.format("Memory filled too much to process (%.2f%%, allowed %.2f%%)",
                    getMemoryFillRatio() * 100,
                    ALLOWED_MEMORY_FILL_RATIO * 100));

            List<O> results = Collections.unmodifiableList(pullResults());

            for (MemoryFullHandler<O> handler : getHandlersToRun()) {
                executeHandleCatchingAnyError(handler, results);
            }
        }
    }

    private List<MemoryFullHandler<O>> getHandlersToRun() {
        List<MemoryFullHandler<O>> handlersToRun = new LinkedList<>(memoryFullHandlers);
        handlersToRun.addAll(mandatoryHandlers);
        return handlersToRun;
    }

    private double getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    private double getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    private double getMemoryFillRatio() {
        return 1.0 - (getFreeMemory() / getMaxMemory());
    }

    public MemoryWorker<O> addMemoryFullHandler(MemoryFullHandler<O> memoryFullHandler) {
        this.memoryFullHandlers.add(memoryFullHandler);
        return this;
    }

    public void stopExecution() {
        this.isRunning.set(false);
    }

    private class GCRunHandler implements MemoryFullHandler<O> {

        @Override
        public void onMemoryFull(List<O> results) {
            System.gc();
            LOG.info(String.format("Memory filled in %.2f%% after GC.", MemoryStats.getMemoryFillRatio() * 100));
        }
    }
}
