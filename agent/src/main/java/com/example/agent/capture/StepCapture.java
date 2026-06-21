package com.example.agent.capture;

/**
 * Binds the {@link StepCollector} for the current run to the executing thread.
 *
 * <p>Spring AI runs the tool-calling loop synchronously on the request thread, so a
 * {@link ThreadLocal} cleanly ties captured steps to the in-flight run without threading a
 * collector through the ChatClient API or risking it leaking into the MCP wire payload.
 *
 * <p>Phase 4 (async) moves execution onto a background worker — the collector is simply
 * started on that worker thread instead; the same-thread assumption still holds per run.
 */
public final class StepCapture {

    private static final ThreadLocal<StepCollector> CURRENT = new ThreadLocal<>();

    private StepCapture() {
    }

    public static StepCollector start(int maxSteps, java.util.function.Consumer<Step> listener) {
        StepCollector collector = new StepCollector(maxSteps, listener);
        CURRENT.set(collector);
        return collector;
    }

    static StepCollector current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
