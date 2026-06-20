package com.example.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The agent / orchestrator. Owns the Ollama ChatClient and an MCP client that reaches tools
 * ONLY over MCP/HTTP (see docs/adr/0003). Spring AI runs the tool-calling loop internally;
 * the {@link RecordingToolCallback} hook makes each step visible.
 */
@SpringBootApplication
public class AgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
