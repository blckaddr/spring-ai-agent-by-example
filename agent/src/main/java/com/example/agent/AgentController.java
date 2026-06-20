package com.example.agent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent endpoints.
 *
 * <ul>
 *   <li><b>Sync</b> (phases 0–3.5): {@code POST /agent/run} — runs the loop and returns the
 *       answer + steps + usage, blocking until done.</li>
 *   <li><b>Async</b> (Phase 4): {@code POST /agent/runs} returns 202 + a runId immediately and runs
 *       detached; {@code GET /agent/runs/{runId}} polls status + result. The sync endpoint stays.</li>
 * </ul>
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;
    private final AgentRunner agentRunner;

    public AgentController(AgentService agentService, AgentRunner agentRunner) {
        this.agentService = agentService;
        this.agentRunner = agentRunner;
    }

    /** Synchronous run — blocks until the loop finishes. */
    @PostMapping("/run")
    public AgentResponse run(@RequestBody AgentRequest request) {
        return agentService.run(request.input(), request.sessionId());
    }

    /** Async run — returns 202 + runId immediately; the loop runs detached. */
    @PostMapping("/runs")
    public ResponseEntity<RunRecord> startRun(@RequestBody AgentRequest request) {
        RunRecord record = agentRunner.submit(request.input(), request.sessionId(), request.eventId());
        return ResponseEntity.accepted().body(record);
    }

    /** Poll a detached run's status + result. */
    @GetMapping("/runs/{runId}")
    public ResponseEntity<RunRecord> getRun(@PathVariable String runId) {
        return agentRunner.get(runId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
