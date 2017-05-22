package com.github.walak.memoryworker;

import com.walak.github.memoryworker.MemoryFullHandler;
import com.walak.github.memoryworker.MemoryWorker;
import com.walak.github.memoryworker.task.BasicMemoryWorkerTask;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class Example {

    public static void main(String[] args) {
        MemoryWorker<Double, SimpleMemoryTask> memoryWorker =
                new MemoryWorker<Double, SimpleMemoryTask>(10240)
                        .addMemoryFullHandler(new PrintOutMemoryFullHandler());

        Thread workerExecutionThread = new Thread(memoryWorker);
        workerExecutionThread.setName("MemoryWorker thread");
        workerExecutionThread.start();
        while (true) {
            SimpleMemoryTask simpleMemoryTask = new SimpleMemoryTask(SimpleMemoryTask.RANDOM.nextInt());
            memoryWorker.addTask(simpleMemoryTask);
        }
    }

    private static class PrintOutMemoryFullHandler implements MemoryFullHandler<Double> {
        private static final Logger LOG = Logger.getLogger(PrintOutMemoryFullHandler.class.getSimpleName());

        @Override
        public void onMemoryFull(List<Double> results) {
            LOG.info("Generated tasks: " + results.size());
        }
    }

    private static class SimpleMemoryTask extends BasicMemoryWorkerTask<Integer, Double> {
        private static final Random RANDOM = new Random();

        public SimpleMemoryTask(Integer input) {
            super(input);
        }

        @Override
        public Double call() {
            return RANDOM.nextDouble() * getInput();
        }
    }
}
