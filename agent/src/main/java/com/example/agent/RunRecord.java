package com.example.agent;

import java.time.Instant;

/**
 * A detached run's persisted state (Phase 4). Immutable — transitions create a new record that
 * replaces the old one in the {@link RunStore}. Holds the same {@link AgentResponse}
 * (answer + steps + usage) the sync endpoint returns, now stored instead of returned inline.
 */
public record RunRecord(
        String runId,
        RunStatus status,
        AgentResponse result,
        String error,
        Instant createdAt,
        Instant finishedAt) {

    static RunRecord queued(String runId, Instant now) {
        return new RunRecord(runId, RunStatus.QUEUED, null, null, now, null);
    }

    RunRecord running() {
        return new RunRecord(runId, RunStatus.RUNNING, null, null, createdAt, null);
    }

    RunRecord done(AgentResponse result, Instant now) {
        return new RunRecord(runId, RunStatus.DONE, result, null, createdAt, now);
    }

    RunRecord failed(String error, Instant now) {
        return new RunRecord(runId, RunStatus.FAILED, null, error, createdAt, now);
    }
}
