package com.example.agent.usage;

/**
 * Phase 3.5 — cost/usage summary for a run, taken from the final {@code ChatResponse}.
 *
 * <p>Honest scope note: in Spring AI the tool-calling loop runs *inside* the chat model, so the
 * cleanly-accessible usage is the FINAL response's (the last, largest round — it carries the whole
 * grown context). It is a strong proxy for run cost but not a per-round total; true per-round
 * accounting needs the tracing/observability stack this project deliberately skips. Token counts
 * are whatever the provider reported (null if it reported none); {@code estimatedCostUsd} is 0 for
 * local models. See {@link Pricing} — switching to a paid model is a one-row change there.
 *
 * @param promptTokens     input tokens reported by the provider (nullable)
 * @param completionTokens output tokens reported by the provider (nullable)
 * @param totalTokens      total tokens reported by the provider (nullable)
 * @param wallClockMs      wall-clock time for the whole run (all rounds)
 * @param estimatedCostUsd dollar estimate (0 for local models)
 * @param model            model id reported by the provider
 * @param note             how to read the cost / its scope
 */
public record RunUsage(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        long wallClockMs,
        double estimatedCostUsd,
        String model,
        String note) {
}
