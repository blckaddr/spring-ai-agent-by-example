package com.example.calculator;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link CalculatorService}'s {@code @Tool} methods with the MCP server.
 */
@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider calculatorTools(CalculatorService calculatorService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(calculatorService)
                .build();
    }
}
