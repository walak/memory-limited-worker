package com.walak.github.memoryworker.task;

import java.util.concurrent.Callable;

public interface MemoryWorkerTask<O> extends Callable<O> {

}
