package com.example.agent.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Phase 6 — the catalog of tools the planner can plan against, discovered over MCP at startup
 * (never hardcoded in the agent). Each connected {@link McpSyncClient} is asked for its server
 * name and tool list, including each tool's description and parameter names. The planner is given
 * this catalog so it can compose a plan from real, attributed capabilities — exactly how the agent
 * learns what currency, calculator and fees/tax can do.
 *
 * <p>Note: in the planning phase these tools are never <em>called</em>; the catalog only tells the
 * planner what exists. That's why a third "specialist" server costs almost nothing here.
 */
@Component
public class ToolCatalog {

    private static final Logger log = LoggerFactory.getLogger(ToolCatalog.class);

    /** One discovered tool: which server provides it, its name, description and parameter names. */
    public record CatalogTool(String server, String name, String description, List<String> params) {
    }

    private final List<CatalogTool> tools = new ArrayList<>();

    public ToolCatalog(List<McpSyncClient> mcpClients) {
        for (McpSyncClient client : mcpClients) {
            String server = serverName(client);
            try {
                for (var tool : client.listTools().tools()) {
                    tools.add(new CatalogTool(server, tool.name(), tool.description(), paramsOf(tool)));
                }
            } catch (RuntimeException e) {
                log.warn("Could not list tools for MCP server '{}': {}", server, e.toString());
            }
        }
        log.info("tool catalog discovered: {}", tools);
    }

    public List<CatalogTool> tools() {
        return List.copyOf(tools);
    }

    /** The distinct server names, in discovery order — the valid "specialist" values for a plan. */
    public List<String> servers() {
        List<String> servers = new ArrayList<>();
        for (CatalogTool t : tools) {
            if (!servers.contains(t.server())) {
                servers.add(t.server());
            }
        }
        return servers;
    }

    /** A compact, human/LLM-readable rendering grouped by server, for the planner prompt. */
    public String forPrompt() {
        Map<String, List<CatalogTool>> byServer = new LinkedHashMap<>();
        for (CatalogTool t : tools) {
            byServer.computeIfAbsent(t.server(), k -> new ArrayList<>()).add(t);
        }
        StringBuilder sb = new StringBuilder();
        byServer.forEach((server, list) -> {
            sb.append("== ").append(server).append(" ==\n");
            for (CatalogTool t : list) {
                sb.append("- ").append(t.name()).append('(')
                        .append(String.join(", ", t.params())).append("): ")
                        .append(t.description() == null ? "" : t.description().replaceAll("\\s+", " ").trim())
                        .append('\n');
            }
        });
        return sb.toString();
    }

    private static List<String> paramsOf(io.modelcontextprotocol.spec.McpSchema.Tool tool) {
        try {
            // inputSchema is a JSON-schema Map; parameter names are the keys of "properties".
            if (tool.inputSchema() instanceof Map<?, ?> schema
                    && schema.get("properties") instanceof Map<?, ?> props) {
                return props.keySet().stream().map(String::valueOf).toList();
            }
        } catch (RuntimeException ignored) {
            // schema shape varies between SDK versions — params are decorative, fall back to none
        }
        return List.of();
    }

    private static String serverName(McpSyncClient client) {
        try {
            if (client.getServerInfo() != null && client.getServerInfo().name() != null) {
                return client.getServerInfo().name();
            }
        } catch (RuntimeException e) {
            log.warn("Could not read server info: {}", e.toString());
        }
        return "?";
    }
}
