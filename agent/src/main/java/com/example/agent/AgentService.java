package com.example.agent;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
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

    /**
     * Phase 1 lesson: a small local model orchestrates granular tools unreliably (it may add
     * before converting, or do final arithmetic "in its head"). This system prompt steers the
     * order without collapsing the work into one coarse tool — the model still orchestrates.
     */
    private static final String SYSTEM_PROMPT = """
            You are a precise calculation agent. Use the provided tools for EVERY arithmetic
            operation and EVERY currency conversion — never compute or convert in your head.
            When amounts are in different currencies, first convert EACH amount to the target
            currency with the convert tool, and only THEN sum the converted amounts with the
            add tool. Do not call add until all needed conversions are done.

            Tool argument types are strict. Pass numbers as JSON numbers, never as strings:
            use amount: 100, not amount: "100". Pass number lists as JSON arrays of numbers:
            use numbers: [1.0, 2.0, 3.0], not numbers: "[1.0, 2.0, 3.0]". When you add the
            converted amounts, pass the ACTUAL converted values returned by the convert tool.""";

    /** Phase 3: default conversation when the caller doesn't supply a sessionId. */
    private static final String DEFAULT_SESSION = "default";

    private final ChatClient chatClient;
    private final List<ToolCallback> tools;
    private final int maxSteps;

    public AgentService(ChatClient.Builder chatClientBuilder,
                        ToolCallbackProvider mcpToolProvider,
                        McpToolServerIndex serverIndex,
                        ChatMemory chatMemory,
                        @org.springframework.beans.factory.annotation.Value("${agent.safety.max-steps:20}") int maxSteps) {
        this.maxSteps = maxSteps;
        // Phase 3: a chat-memory advisor injects prior turns of the conversation (keyed by
        // conversation id) before each model call and saves the new turn after. State lives in
        // the ChatMemory store, OUTSIDE the request — never in instance fields (see CLAUDE.md).
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        // Wrap each MCP-provided tool so its invocations are captured. The wrapper is
        // transparent to the model (same tool definition). Server attribution is resolved
        // once, here, from the tool->server index.
        this.tools = Arrays.stream(mcpToolProvider.getToolCallbacks())
                .map(tc -> (ToolCallback) new RecordingToolCallback(
                        tc, serverIndex.serverFor(tc.getToolDefinition().name())))
                .toList();
    }

    public AgentResponse run(String request, String sessionId) {
        String conversationId = (sessionId == null || sessionId.isBlank()) ? DEFAULT_SESSION : sessionId;
        StepCollector collector = StepCapture.start(maxSteps);
        long startNanos = System.nanoTime();
        try {
            ChatResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(request)
                    // Phase 3: select which conversation's memory this call reads/writes.
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .toolCallbacks(tools)
                    .call()
                    .chatResponse();
            long wallClockMs = (System.nanoTime() - startNanos) / 1_000_000;
            String answer = response == null ? "" : response.getResult().getOutput().getText();
            return new AgentResponse(answer, collector.getSteps(), summarizeUsage(response, wallClockMs));
        } finally {
            StepCapture.clear();
        }
    }

    /** Phase 3.5: cost/usage summary from the final ChatResponse (see RunUsage for scope). */
    private static RunUsage summarizeUsage(ChatResponse response, long wallClockMs) {
        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;
        String model = null;
        if (response != null && response.getMetadata() != null) {
            model = response.getMetadata().getModel();
            Usage usage = response.getMetadata().getUsage();
            if (usage != null) {
                promptTokens = usage.getPromptTokens();
                completionTokens = usage.getCompletionTokens();
                totalTokens = usage.getTotalTokens();
            }
        }
        double costUsd = Pricing.estimateUsd(model,
                promptTokens == null ? 0 : promptTokens,
                completionTokens == null ? 0 : completionTokens);
        return new RunUsage(promptTokens, completionTokens, totalTokens, wallClockMs, costUsd, model, Pricing.note(model));
    }
}
