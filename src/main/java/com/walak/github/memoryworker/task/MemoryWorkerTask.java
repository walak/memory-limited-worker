package com.walak.github.memoryworker.task;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class MemoryWorkerTask<O> implements Callable<O> {
    private static final Logger LOG = Logger.getLogger(MemoryWorkerTask.class.getSimpleName());

    private MemoryWorkerTaskStatus status;
    private final Callable<O> actualTask;

    public MemoryWorkerTask(Callable<O> task) {
        this.status = MemoryWorkerTaskStatus.UNKNOWN;
        this.actualTask = task;
    }

    @Override
    public O call() {
        try {
            this.status = MemoryWorkerTaskStatus.SUCESS;
            return actualTask.call();
        } catch (Exception e) {
            LOG.severe("Exception occured running task: " + e.getMessage());
            this.status = MemoryWorkerTaskStatus.FAILURE;
            throw new RuntimeException(e);
        }
    }

    public MemoryWorkerTaskStatus getStatus() {
        return status;
    }

}
