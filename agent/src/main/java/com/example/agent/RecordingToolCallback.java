package com.example.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * THE CAPTURE HOOK — the through-line of the whole project.
 *
 * <p>Wraps a real {@link ToolCallback} (here, an MCP tool) and, on every invocation, records a
 * {@link Step} (tool, server, arguments, result/error, latency) into the run's
 * {@link StepCollector} and logs it. The wrapper is transparent to the model: it delegates
 * {@link #getToolDefinition()} unchanged, so the tool the model sees is identical.
 *
 * <p>Errors are recorded AND rethrown so the loop sees the failure unchanged — this is what
 * Phase 2 (failure & recovery) observes, for free.
 *
 * <p>Built once here; every later phase consumes the same {@link Step} data.
 */
public class RecordingToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(RecordingToolCallback.class);

    private final ToolCallback delegate;

    public RecordingToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return invoke(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return invoke(toolInput, toolContext);
    }

    private String invoke(String toolInput, ToolContext toolContext) {
        String tool = getToolDefinition().name();
        String server = serverOf(tool);
        long startNanos = System.nanoTime();
        String result = null;
        String error = null;
        try {
            result = (toolContext == null)
                    ? delegate.call(toolInput)
                    : delegate.call(toolInput, toolContext);
            return result;
        } catch (RuntimeException e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw e; // let the loop see the failure unchanged (Phase 2)
        } finally {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            StepCollector collector = StepCapture.current();
            if (collector != null) {
                collector.add(tool, server, toolInput, result, error, latencyMs);
            }
            log.info("[step] tool={} server={} args={} result={} error={} latencyMs={}",
                    tool, server, toolInput, result, error, latencyMs);
        }
    }

    /**
     * Best-effort server attribution. Phase 0 has a single server, so this is a constant.
     * Phase 1 (multi-server) replaces this with real attribution from the MCP connection id.
     */
    private static String serverOf(String tool) {
        return "currency";
    }
}
