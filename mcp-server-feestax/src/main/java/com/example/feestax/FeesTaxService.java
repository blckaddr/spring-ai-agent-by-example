package com.example.feestax;

import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Fees &amp; tax tools, kept GRANULAR and with fixed demo numbers — same spirit as the currency
 * server's static rates. They give the planner a third specialist to reason about: consolidating
 * money into a currency incurs a per-transaction fee and a holding tax, so "the cheapest target
 * currency" is no longer a single conversion — it forces a real decomposition.
 *
 * <p>In the Phase-6 planning demo these are never executed; the planner only discovers their
 * signatures over MCP. The bodies are real (demo values) so a future execution phase works for free.
 */
@Service
public class FeesTaxService {

    /** Demo per-currency holding/withholding tax, as a percentage. */
    private static final Map<String, Double> TAX_PCT = Map.of(
            "USD", 0.4,
            "EUR", 0.6,
            "GBP", 0.5,
            "JPY", 0.2,
            "CHF", 0.1,
            "CAD", 0.3
    );

    @Tool(description = "Estimate the transaction fee charged to consolidate an amount of money in a "
            + "given currency. Demo model: 0.5% of the amount, with a minimum fee of 1 unit. "
            + "Returns the fee in the same currency.")
    public double transactionFee(
            @ToolParam(description = "The amount being consolidated, in 'currency'") double amount,
            @ToolParam(description = "ISO 4217 currency code the amount is held in, e.g. GBP") String currency) {
        double fee = Math.max(1.0, Math.abs(amount) * 0.005);
        return Math.round(fee * 100.0) / 100.0;
    }

    @Tool(description = "Look up the holding/withholding tax rate for consolidating into a currency, "
            + "as a percentage (e.g. 0.5 means 0.5%). Uses fixed demo rates.")
    public double taxRate(
            @ToolParam(description = "Target ISO 4217 currency code, e.g. GBP") String currency) {
        if (currency == null) {
            throw new IllegalArgumentException("currency code must not be null");
        }
        Double pct = TAX_PCT.get(currency.trim().toUpperCase());
        if (pct == null) {
            throw new IllegalArgumentException(
                    "Unknown currency code: '" + currency + "'. Known codes: " + TAX_PCT.keySet());
        }
        return pct;
    }
}
