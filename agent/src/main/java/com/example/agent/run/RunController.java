package com.example.agent.run;

import com.example.agent.chat.AgentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 4 — the detached/async run endpoints. {@code POST /agent/runs} returns 202 + a runId
 * immediately and runs the loop detached; {@code GET /agent/runs/{runId}} polls status + result.
 *
 * <p>Lives in the {@code run} package (not {@code chat}) so the dependency runs one way:
 * {@code run → chat} (the runner drives the chat agent), with no cycle back.
 */
@RestController
@RequestMapping("/agent")
public class RunController {

    private final AgentRunner agentRunner;

    public RunController(AgentRunner agentRunner) {
        this.agentRunner = agentRunner;
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
