package com.example.agent;

/**
 * Phase 6 — thrown when the planner's output can't be turned into a valid {@link PlanGraph}
 * (not JSON, malformed, dangling dependency, or a cycle). Carries the raw model text so the UI can
 * show <em>what</em> the model produced and <em>why</em> it was rejected — the planning analogue of
 * "trust the steps, not the prose".
 */
public class PlanException extends RuntimeException {

    private final String raw;

    public PlanException(String message, String raw) {
        super(message);
        this.raw = raw;
    }

    public String getRaw() {
        return raw;
    }
}
