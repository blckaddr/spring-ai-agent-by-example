package com.example.agent;

/**
 * Request body for the agent endpoints.
 *
 * @param input     the natural-language request
 * @param sessionId conversation id for multi-turn memory (Phase 3); optional — defaults when blank
 * @param eventId   idempotency key for async runs (Phase 4); optional — a repeated eventId returns
 *                  the same run instead of starting a new one
 */
public record AgentRequest(String input, String sessionId, String eventId) {
}
