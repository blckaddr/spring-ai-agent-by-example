package com.example.agent.run;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.example.agent.chat.AgentResponse;
import org.springframework.stereotype.Component;

/**
 * In-memory store of detached runs (Phase 4), keyed by runId. Plus a basic idempotency index:
 * the same eventId returns the same run rather than starting a new one.
 *
 * <p>Learning-scope on purpose: runs are lost on restart and not shared across instances.
 * Production would swap this Map for Redis/JDBC — and because callers depend only on this
 * component, that swap wouldn't touch the agent logic.
 */
@Component
public class RunStore {

    private final Map<String, RunRecord> runs = new ConcurrentHashMap<>();
    private final Map<String, String> runIdByEventId = new ConcurrentHashMap<>();

    /** Create a fresh QUEUED run (no idempotency key). */
    public RunRecord create() {
        String runId = UUID.randomUUID().toString();
        RunRecord record = RunRecord.queued(runId, Instant.now());
        runs.put(runId, record);
        return record;
    }

    /** Idempotent create: a repeated eventId returns the existing run. */
    public RunRecord createForEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return create();
        }
        String runId = runIdByEventId.computeIfAbsent(eventId, k -> create().runId());
        return runs.get(runId);
    }

    public void markRunning(String runId) {
        runs.computeIfPresent(runId, (k, v) -> v.running());
    }

    public void complete(String runId, AgentResponse result) {
        runs.computeIfPresent(runId, (k, v) -> v.done(result, Instant.now()));
    }

    public void fail(String runId, String error) {
        runs.computeIfPresent(runId, (k, v) -> v.failed(error, Instant.now()));
    }

    public Optional<RunRecord> get(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }
}
