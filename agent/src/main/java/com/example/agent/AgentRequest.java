package com.example.agent;

/** Request body for {@code POST /agent/run}. */
public record AgentRequest(String input) {
}
