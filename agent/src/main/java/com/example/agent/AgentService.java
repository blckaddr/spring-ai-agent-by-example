package com.example.agent;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

/**
 * The orchestrator. Builds an Ollama {@link ChatClient}, wraps every MCP tool in a
 * {@link RecordingToolCallback}, and runs the synchronous tool-calling loop. Spring AI drives
 * the loop internally (model decides → tool called → result fed back → repeat) until a final
 * answer; the wrapper makes each step visible.
 */
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final List<ToolCallback> tools;

    public AgentService(ChatClient.Builder chatClientBuilder, ToolCallbackProvider mcpToolProvider) {
        this.chatClient = chatClientBuilder.build();
        // Wrap each MCP-provided tool so its invocations are captured. The wrapper is
        // transparent to the model (same tool definition).
        this.tools = Arrays.stream(mcpToolProvider.getToolCallbacks())
                .map(tc -> (ToolCallback) new RecordingToolCallback(tc))
                .toList();
    }

    public AgentResponse run(String request) {
        StepCollector collector = StepCapture.start();
        try {
            String answer = chatClient.prompt()
                    .user(request)
                    .toolCallbacks(tools)
                    .call()
                    .content();
            return new AgentResponse(answer, collector.getSteps());
        } finally {
            StepCapture.clear();
        }
    }
}
