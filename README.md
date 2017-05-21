# memory-limited-worker
A simple framework to run series of tasks until JVM memory is full.

## Abstract
This simple project will be a part of [new Java implementation](https://github.com/walak/knight-java)
of [knight game](https://github.com/walak/knight). To gain better performance in distributed environment,
Java implementation will behave a bit different comparing to Python's solution - it produces new results until
JVM memory is almost full. To control the process automatically I prepared this simple framework which is able
to spot that memory is almost full, then handle all produced results and continue right after GC is finished.

## Usage


### Basic usage
The core of the project is `MemoryWorker` class. It is implement as `Runnable` so it can be ran in
separated thread.

Particular task should implement `MemoryWorkerTask<O>`. `O` is a type of returned result. The interface
declares one method `execute()` used to perform actual work.

To cover the most simple use cases you can use `BasicMemoryWorkerTask<I, O>` class,
allowing you to pass input data of type `I` as a constuctor parameter. Passed argument is available by
calling `getInput()` method. Example of usage:
```java
import com.walak.github.memoryworker.task.BasicMemoryWorkerTask;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) {
        MemoryWorker<Double, SimpleMemoryTask> memoryWorker =
                new MemoryWorker<Double, SimpleMemoryTask>(10240);

        Thread workerExecutionThread = new Thread(memoryWorker);
        workerExecutionThread.setName("MemoryWorker thread");
        workerExecutionThread.start();
        while (true) {
            SimpleMemoryTask simpleMemoryTask = new SimpleMemoryTask(SimpleMemoryTask.RANDOM.nextInt());
            memoryWorker.addTask(simpleMemoryTask);
        }
    }

    private static class SimpleMemoryTask extends BasicMemoryWorkerTask<Integer, Double> {
        private static final Random RANDOM = new Random();

        public SimpleMemoryTask(Integer input) {
            super(input);
        }

        @Override
        public Double execute() {
            return RANDOM.nextDouble() * getInput();
        }
    }
}
```
See `Main.java` for full example.

### Handlers
One `MemoryWorker` can have many handlers attached. The handlers should implement
`MemoryFullHandler` interface and implement `onMemoryFull()` method.
When general memory usage is above certain treshold (90% for now)
execution is stopped and all handlers are called so that handlers can consume
results.
Important notes:
* **All handlers receive the same instance of immutable list**
* **Handlers are called in `MemoryWorker`'s thread so execution is stopped**
* **Tasks can be added during the phase unless job queue is full**

Besides user's handlers there are a few mandatory handlers to assure GC run, etc.

### Tips

* for optimal performance it should rather run small tasks that produce large output
* current design works fine with small JVM memory settings, e. g. `-Xmx256m`, running it
with big heap space and small results may cause significant pauses due to excessive GC.
* setting both `-Xms` and `-Xmx` may improve performance a bit

## Further development

For now it is bare (but good enough) minimum I needed. As a next step I want to add such features:
* add more smart capacity checking, for now it may cause OOME during task execution
