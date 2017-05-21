package com.walak.github.memoryworker;

import java.util.List;

public interface MemoryFullHandler<O> {

    void onMemoryFull(List<O> results);
}
