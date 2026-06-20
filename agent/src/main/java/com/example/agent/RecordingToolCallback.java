package com.example.agent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final String server;

    public RecordingToolCallback(ToolCallback delegate, String server) {
        this.delegate = delegate;
        this.server = server;
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
        // Phase 4 safety cap: refuse to run more tool steps than allowed in one run.
        StepCollector current = StepCapture.current();
        if (current != null && current.stepCount() >= current.maxSteps()) {
            throw new LoopLimitExceededException(
                    "max steps (" + current.maxSteps() + ") reached for this run");
        }
        long startNanos = System.nanoTime();
        String result = null;
        String error = null;
        try {
            result = (toolContext == null)
                    ? delegate.call(toolInput)
                    : delegate.call(toolInput, toolContext);
            return result;
        } catch (RuntimeException e) {
            error = cleanError(e);
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
     * MCP tool failures arrive wrapped, e.g.
     * {@code ToolExecutionException: Error calling tool: [TextContent[..., text=Unsupported
     * currency code: 'AUD'. Known codes: [...]], meta=null]]}. For legible step records we pull
     * out the inner {@code text=...} root message when present (Phase 2). Falls back to the raw
     * message so nothing is ever lost.
     */
    private static String cleanError(Throwable e) {
        String type = e.getClass().getSimpleName();
        String msg = e.getMessage();
        if (msg == null) {
            return type;
        }
        Matcher m = Pattern.compile("text=(.*), meta=", Pattern.DOTALL).matcher(msg);
        return type + ": " + (m.find() ? m.group(1).trim() : msg);
    }
}
