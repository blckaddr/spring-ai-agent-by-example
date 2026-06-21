package com.example.agent.chat;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The synchronous chat endpoint (phases 0–3.5): {@code POST /agent/run} runs the tool-calling loop
 * and returns the answer + steps + usage, blocking until done.
 *
 * <p>The detached/async variant ({@code POST /agent/runs}, {@code GET /agent/runs/{id}}) lives in
 * {@code run.RunController} — keeping this controller (and the whole {@code chat} package) free of
 * any dependency on the {@code run} package.
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /** Synchronous run — blocks until the loop finishes. */
    @PostMapping("/run")
    public AgentResponse run(@RequestBody AgentRequest request) {
        return agentService.run(request.input(), request.sessionId());
    }
}
