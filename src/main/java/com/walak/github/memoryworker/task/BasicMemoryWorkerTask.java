package com.walak.github.memoryworker.task;

public abstract class BasicMemoryWorkerTask<I, O> implements MemoryWorkerTask<O> {

    private final I input;

    public BasicMemoryWorkerTask(I input) {
        this.input = input;
    }

    public I getInput() {
        return input;
    }


}
