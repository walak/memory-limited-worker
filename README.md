# memory-limited-worker
A simple framework to run series of tasks until JVM memory is full.

## Abstract
This simple project will be a part of [new Java implementation](https://github.com/walak/knight-java)
of [knight game](https://github.com/walak/knight). To gain better performance in distributed environment,
Java implementation behave a bit different comparing to Python's solution - it produces new results until
JVM memory is full above certain threshold (90% for now). To control the process automatically I prepared this simple framework which is able
to spot that memory is almost full, then handle all produced results and continue right after GC is finished.

## Usage

### Basic usage
The core of the project is `MemoryWorker` class. 
It is implement as `Runnable` so it can be ran in separated thread.
Any task to be executed should be implemented as Callable<O> as there is an assumption that **every job returns a value
which should not be null**. 

To add a task to the MemoryWorker's queue you need to wrap it in MemoryWorkerTask, as below:
```java
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
.
.
.

MemoryWorker<Double> memoryWorker = new MemoryWorker<Double>(10240);
SimpleMemoryTask simpleMemoryTask = new SimpleMemoryTask(SimpleMemoryTask.RANDOM.nextInt());
MemoryWorkerTask<Double> task = new MemoryWorkerTask<>(simpleMemoryTask)
memoryWorker.addTask(task);
```
For full example please see [`Example.java`](https://github.com/walak/memory-limited-worker/blob/master/src/test/java/com/github/walak/memoryworker/Example.java)

### Task status
MemoryWorkerTask holds a value to determine whether task has been executed successfully. It is accessible 
via `getStatus()` method Possible values are:
* `UNKNOWN` - task is yet to run or it is just executing.
* `SUCCESS` - task was executed correctly
* `FAILURE` - exception was thrown during task execution.

### Handlers
One `MemoryWorker` can have many handlers attached. The handlers should implement
`MemoryFullHandler` interface and implement `onMemoryFull()` method.
When general memory usage is above certain threshold (90% for now)
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
