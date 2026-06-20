package com.example.agent;

/** Lifecycle of a detached agent run (Phase 4). */
public enum RunStatus {
    QUEUED,
    RUNNING,
    DONE,
    FAILED
}
