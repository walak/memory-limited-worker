package com.walak.github.memoryworker;

public class MemoryStats {

    public static double getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public static double getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public static double getMemoryFillRatio() {
        return 1 - (getFreeMemory() / getMaxMemory());
    }
}
