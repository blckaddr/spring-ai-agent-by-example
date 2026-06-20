package com.example.agent;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Phase 4 — runs the agent loop detached. {@link #submit} returns immediately with a QUEUED/RUNNING
 * {@link RunRecord}; the loop runs on the background executor and writes its result (answer + steps
 * + usage) into the {@link RunStore} when done. A wall-clock cap marks a run FAILED if it overruns.
 */
@Service
public class AgentRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentRunner.class);

    private final AgentService agentService;
    private final RunStore runStore;
    private final ExecutorService executor;
    private final long maxWallClockMs;
    private final Set<String> submitted = ConcurrentHashMap.newKeySet();

    public AgentRunner(AgentService agentService,
                       RunStore runStore,
                       ExecutorService agentExecutor,
                       @Value("${agent.safety.max-wall-clock-ms:120000}") long maxWallClockMs) {
        this.agentService = agentService;
        this.runStore = runStore;
        this.executor = agentExecutor;
        this.maxWallClockMs = maxWallClockMs;
    }

    public RunRecord submit(String input, String sessionId, String eventId) {
        RunRecord record = runStore.createForEvent(eventId);
        String runId = record.runId();
        // submitted.add is true only the first time -> idempotent; a repeated eventId won't re-run.
        if (submitted.add(runId)) {
            CompletableFuture
                    .runAsync(() -> {
                        runStore.markRunning(runId);
                        AgentResponse response = agentService.run(input, sessionId);
                        runStore.complete(runId, response);
                        log.info("run {} DONE", runId);
                    }, executor)
                    .orTimeout(maxWallClockMs, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> {
                        String reason = describe(ex);
                        runStore.fail(runId, reason);
                        log.warn("run {} FAILED: {}", runId, reason);
                        return null;
                    });
        }
        return runStore.get(runId).orElse(record);
    }

    public Optional<RunRecord> get(String runId) {
        return runStore.get(runId);
    }

    private static String describe(Throwable ex) {
        Throwable cause = (ex instanceof CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
