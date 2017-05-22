package com.github.walak.memoryworker;

import com.walak.github.memoryworker.MemoryFullHandler;
import com.walak.github.memoryworker.MemoryWorker;
import com.walak.github.memoryworker.task.MemoryWorkerTask;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class Example {

    public static void main(String[] args) {
        MemoryWorker<Double> memoryWorker =
                new MemoryWorker<Double>(10240)
                        .addMemoryFullHandler(new PrintOutMemoryFullHandler());

        Thread workerExecutionThread = new Thread(memoryWorker);
        workerExecutionThread.setName("MemoryWorker thread");
        workerExecutionThread.start();
        while (true) {
            SimpleMemoryTask simpleMemoryTask = new SimpleMemoryTask(SimpleMemoryTask.RANDOM.nextInt());
            memoryWorker.addTask(new MemoryWorkerTask<>(simpleMemoryTask));
        }
    }

    private static class PrintOutMemoryFullHandler implements MemoryFullHandler<Double> {
        private static final Logger LOG = Logger.getLogger(PrintOutMemoryFullHandler.class.getSimpleName());

        @Override
        public void onMemoryFull(List<Double> results) {
            LOG.info("Generated tasks: " + results.size());
        }
    }

    private static class SimpleMemoryTask implements Callable<Double> {
        private static final Random RANDOM = new Random();

        private final int input;

        public SimpleMemoryTask(int input) {
            this.input = input;
        }

        @Override
        public Double call() {
            return RANDOM.nextDouble() * input;
        }
    }
}
