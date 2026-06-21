package com.example.agent.plan;

import java.util.List;

import com.example.agent.mcp.ToolCatalog;
import com.example.agent.usage.RunUsage;

/**
 * Phase 6 — response for {@code POST /agent/plan}: the plan graph, the tool catalog it was planned
 * against (so the UI can show what was available), and the cost of the single planning call.
 */
public record PlanResult(PlanGraph graph, List<ToolCatalog.CatalogTool> catalog, RunUsage usage) {
}
