package com.example.agent;

import java.util.List;

/** Response for {@code POST /agent/run}: the final answer and the visible loop steps. */
public record AgentResponse(String answer, List<Step> steps) {
}
