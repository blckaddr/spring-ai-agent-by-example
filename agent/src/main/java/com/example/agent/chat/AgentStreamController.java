package com.example.agent.chat;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.example.agent.capture.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Phase 5 — stream the loop live over Server-Sent Events.
 *
 * <p>The "watch it think" goal. Spring AI runs the tool loop internally and hands back only a
 * final answer, so we pry out the intermediate steps via the same capture hook (Phase 0): a step
 * listener pushes each {@link Step} onto the SSE stream the instant the tool returns, instead of
 * collecting them for the end. Same step data as every other phase — now emitted live.
 *
 * <p>GET (not POST) so a browser {@code EventSource} can connect; the loop runs on the background
 * executor and emits events as it goes.
 */
@RestController
@RequestMapping("/agent")
public class AgentStreamController {

    private static final Logger log = LoggerFactory.getLogger(AgentStreamController.class);
    private static final long STREAM_TIMEOUT_MS = 300_000L;

    private final AgentService agentService;
    private final ExecutorService executor;

    public AgentStreamController(AgentService agentService, ExecutorService agentExecutor) {
        this.agentService = agentService;
        this.executor = agentExecutor;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String input,
                             @RequestParam(required = false) String sessionId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        executor.submit(() -> {
            try {
                emit(emitter, "status", Map.of("state", "thinking"));
                AgentResponse response = agentService.run(input, sessionId,
                        step -> emit(emitter, "step", step));
                emit(emitter, "answer", Map.of("answer", response.answer(), "usage", response.usage()));
                emit(emitter, "done", Map.of("state", "complete"));
                emitter.complete();
            } catch (Exception e) {
                log.warn("stream failed: {}", e.toString());
                emit(emitter, "error", Map.of("message", String.valueOf(e.getMessage())));
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private static void emit(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            // client disconnected — surfaced to the loop as a no-op; run continues/ends naturally
            throw new RuntimeException(e);
        }
    }
}
