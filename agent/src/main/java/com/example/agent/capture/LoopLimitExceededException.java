package com.example.agent.capture;

/** Thrown by the step-capture hook when a run exceeds the configured max-steps safety cap. */
public class LoopLimitExceededException extends RuntimeException {
    public LoopLimitExceededException(String message) {
        super(message);
    }
}
