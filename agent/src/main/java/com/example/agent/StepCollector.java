package com.example.agent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Accumulates {@link Step}s for a single agent run. One instance per run.
 */
public class StepCollector {

    private final List<Step> steps = new CopyOnWriteArrayList<>();
    private final AtomicInteger counter = new AtomicInteger();

    public Step add(String tool, String server, String arguments,
                    String result, String error, long latencyMs) {
        Step step = new Step(counter.incrementAndGet(), tool, server, arguments, result, error, latencyMs);
        steps.add(step);
        return step;
    }

    public List<Step> getSteps() {
        return List.copyOf(steps);
    }
}
