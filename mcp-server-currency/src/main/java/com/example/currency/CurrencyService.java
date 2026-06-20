package com.example.currency;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * The actual tools exposed by this MCP server. Plain methods annotated with {@code @Tool};
 * Spring AI turns them into MCP tools (registered in {@link ToolConfig}).
 *
 * <p>Demo rates are static and expressed relative to USD. Unknown currencies throw — that is
 * deliberate: Phase 2 (failure & recovery) drives a tool failure through here.
 */
@Service
public class CurrencyService {

    /** Units of currency per 1 USD. */
    private static final Map<String, Double> PER_USD = Map.of(
            "USD", 1.00,
            "EUR", 0.92,
            "GBP", 0.79,
            "JPY", 157.0,
            "CHF", 0.89,
            "CAD", 1.36
    );

    @Tool(description = "Convert an amount of money from one ISO 4217 currency code to another "
            + "using fixed demo exchange rates. Returns the converted amount.")
    public double convert(
            @ToolParam(description = "The amount to convert, in the 'from' currency") double amount,
            @ToolParam(description = "Source ISO currency code, e.g. USD") String from,
            @ToolParam(description = "Target ISO currency code, e.g. EUR") String to) {

        double fromRate = rateOf(from);
        double toRate = rateOf(to);
        // amount in USD = amount / fromRate ; then * toRate to reach target
        double converted = amount / fromRate * toRate;
        return Math.round(converted * 100.0) / 100.0;
    }

    @Tool(description = "List the available demo exchange rates, expressed as units per 1 USD.")
    public Map<String, Double> listRates() {
        return new LinkedHashMap<>(PER_USD);
    }

    private static double rateOf(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Currency code must not be null");
        }
        Double rate = PER_USD.get(code.trim().toUpperCase());
        if (rate == null) {
            throw new IllegalArgumentException(
                    "Unsupported currency code: '" + code + "'. Known codes: " + PER_USD.keySet());
        }
        return rate;
    }
}
