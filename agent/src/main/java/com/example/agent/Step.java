package com.example.agent;

/**
 * One captured tool call in the agent loop — the project's "handrail".
 *
 * <p>This same record is reused, unchanged, by every later phase: logged (0), chained (1),
 * error-annotated (2), context-aware (3), persisted (4), streamed live (5).
 *
 * @param step      1-based position in the loop
 * @param tool      tool name as the model called it
 * @param server    which MCP server provided the tool (best-effort in Phase 0)
 * @param arguments raw JSON arguments the model passed
 * @param result    raw result returned to the model (null if the call threw)
 * @param error     error description if the call threw (null on success)
 * @param latencyMs wall-clock time for the tool call
 */
public record Step(
        int step,
        String tool,
        String server,
        String arguments,
        String result,
        String error,
        long latencyMs) {
}
