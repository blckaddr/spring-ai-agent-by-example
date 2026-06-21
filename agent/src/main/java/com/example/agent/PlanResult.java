package com.example.agent;

import java.util.List;

/**
 * Phase 6 — response for {@code POST /agent/plan}: the plan graph, the tool catalog it was planned
 * against (so the UI can show what was available), and the cost of the single planning call.
 */
public record PlanResult(PlanGraph graph, List<ToolCatalog.CatalogTool> catalog, RunUsage usage) {
}
