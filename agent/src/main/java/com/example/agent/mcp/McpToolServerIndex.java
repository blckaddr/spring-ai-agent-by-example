package com.example.agent.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.agent.capture.Step;
import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Maps each tool name to the MCP server that provides it, so {@link Step#server()} is accurate
 * once more than one server is connected (Phase 1). Built once at startup by asking every
 * connected {@link McpSyncClient} for its server name and its tool list.
 *
 * <p>Note on collisions: MCP tool names are NOT prefixed by default, so if two servers exposed
 * the same tool name this map would keep the last one — a real disambiguation concern worth
 * discussing. Our three servers (currency: convert/listRates · calculator: add/subtract/multiply ·
 * fees-tax: transactionFee/taxRate) have no collision.
 */
@Component
public class McpToolServerIndex {

    private static final Logger log = LoggerFactory.getLogger(McpToolServerIndex.class);
    private static final String UNKNOWN = "?";

    private final Map<String, String> toolToServer = new HashMap<>();

    public McpToolServerIndex(List<McpSyncClient> mcpClients) {
        for (McpSyncClient client : mcpClients) {
            String server = serverName(client);
            try {
                client.listTools().tools().forEach(tool -> toolToServer.put(tool.name(), server));
            } catch (RuntimeException e) {
                log.warn("Could not list tools for MCP server '{}': {}", server, e.toString());
            }
        }
        log.info("MCP tool->server index: {}", toolToServer);
    }

    public String serverFor(String toolName) {
        return toolToServer.getOrDefault(toolName, UNKNOWN);
    }

    private static String serverName(McpSyncClient client) {
        try {
            if (client.getServerInfo() != null && client.getServerInfo().name() != null) {
                return client.getServerInfo().name();
            }
        } catch (RuntimeException e) {
            log.warn("Could not read server info: {}", e.toString());
        }
        return UNKNOWN;
    }
}
