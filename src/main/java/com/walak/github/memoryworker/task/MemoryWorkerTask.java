package com.walak.github.memoryworker.task;

public interface MemoryWorkerTask<O> {

    O execute();
}
