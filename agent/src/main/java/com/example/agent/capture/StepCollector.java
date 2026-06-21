package com.example.agent.capture;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Accumulates {@link Step}s for a single agent run. One instance per run.
 */
public class StepCollector {

    private final List<Step> steps = new CopyOnWriteArrayList<>();
    private final AtomicInteger counter = new AtomicInteger();
    private final int maxSteps;
    private final Consumer<Step> listener;

    public StepCollector(int maxSteps, Consumer<Step> listener) {
        this.maxSteps = maxSteps;
        this.listener = listener;
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
        if (listener != null) {
            // Phase 5: push the step live (e.g. onto an SSE stream) the moment it fires.
            // Never let a slow/broken consumer break the loop.
            try {
                listener.accept(step);
            } catch (RuntimeException ignored) {
                // client disconnected or sink failed — keep running, still collected
            }
        }
        return step;
    }

    public List<Step> getSteps() {
        return List.copyOf(steps);
    }
}
