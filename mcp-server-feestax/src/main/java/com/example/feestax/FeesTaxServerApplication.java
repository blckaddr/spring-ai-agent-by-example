package com.example.feestax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Fees &amp; tax MCP server — a third passive tool provider, added in Phase 6 so the planner has a
 * genuinely multi-domain goal to decompose ("cheapest currency to consolidate into after fees and
 * tax"). Like the other servers it contains no agent loop (see docs/adr/0003); it just exposes
 * tools over MCP/streamable-HTTP. In the planning phase these tools are never *called* — the
 * planner only needs to discover that they exist.
 */
@SpringBootApplication
public class FeesTaxServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeesTaxServerApplication.class, args);
    }
}
