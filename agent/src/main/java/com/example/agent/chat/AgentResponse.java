package com.example.agent.chat;

import java.util.List;

import com.example.agent.capture.Step;
import com.example.agent.usage.RunUsage;

/**
 * Response for {@code POST /agent/run}: the final answer, the visible loop steps, and the
 * per-run cost/usage summary (Phase 3.5).
 */
public record AgentResponse(String answer, List<Step> steps, RunUsage usage) {
}
