package com.example.feestax;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link FeesTaxService}'s {@code @Tool} methods with the MCP server. The MCP server
 * auto-configuration picks up any {@link ToolCallbackProvider} bean and serves the tools over
 * streamable-HTTP at the configured endpoint.
 */
@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider feesTaxTools(FeesTaxService feesTaxService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(feesTaxService)
                .build();
    }
}
