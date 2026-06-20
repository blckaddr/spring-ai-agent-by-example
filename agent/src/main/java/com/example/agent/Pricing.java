package com.example.agent;

import java.util.Map;

/**
 * Phase 3.5 — turns token counts into a dollar estimate.
 *
 * <p>Local models (Ollama) cost nothing, so the estimate is $0 and tokens are the real observable.
 * The hosted rows are illustrative: they show that switching to a paid provider (e.g. Gemini) is
 * "add a row here" — the capture code never changes, because Spring AI reports usage uniformly.
 *
 * <p>Rates are USD per 1,000,000 tokens, input and output priced separately (output is typically
 * the pricier side). Verify against the provider's current pricing before trusting the number.
 */
public final class Pricing {

    private Pricing() {
    }

    /** USD per 1M tokens: (input, output). */
    private record Rate(double inputPerMillion, double outputPerMillion) {
    }

    private static final Map<String, Rate> RATES = Map.of(
            // Local — free. (Listed explicitly so the note reads "local" rather than "unknown".)
            "qwen2.5:14b", new Rate(0.0, 0.0),
            "llama3.1:8b", new Rate(0.0, 0.0),
            // Illustrative hosted rates — VERIFY before relying on these.
            "gemini-1.5-flash", new Rate(0.075, 0.30),
            "gemini-1.5-pro", new Rate(1.25, 5.00),
            "gpt-4o-mini", new Rate(0.15, 0.60)
    );

    /** Estimated USD cost; 0.0 for local/unknown models. */
    public static double estimateUsd(String model, int promptTokens, int completionTokens) {
        Rate rate = model == null ? null : RATES.get(model);
        if (rate == null) {
            return 0.0;
        }
        return promptTokens / 1_000_000.0 * rate.inputPerMillion()
                + completionTokens / 1_000_000.0 * rate.outputPerMillion();
    }

    public static String note(String model) {
        Rate rate = model == null ? null : RATES.get(model);
        if (rate == null) {
            return "no rate for '" + model + "' — treated as free; add it to Pricing to estimate";
        }
        if (rate.inputPerMillion() == 0.0 && rate.outputPerMillion() == 0.0) {
            return "local model — $0; tokens are the cost proxy";
        }
        return "estimate at " + model + " rates (verify current pricing)";
    }
}
