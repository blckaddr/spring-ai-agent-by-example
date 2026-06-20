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
    private final int maxSteps;

    public StepCollector(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    /** Phase 4 safety cap: max tool steps allowed in one run. */
    public int maxSteps() {
        return maxSteps;
    }

    public int stepCount() {
        return steps.size();
    }

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
