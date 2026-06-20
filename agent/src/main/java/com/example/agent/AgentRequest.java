package com.example.agent;

/**
 * Request body for {@code POST /agent/run}.
 *
 * @param input     the natural-language request
 * @param sessionId conversation id for multi-turn memory (Phase 3); optional — defaults when blank
 */
public record AgentRequest(String input, String sessionId) {
}
