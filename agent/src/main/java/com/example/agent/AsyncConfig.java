package com.example.agent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 4 — the background executor that runs detached agent loops. A simple fixed thread pool
 * is enough for learning; production would use a real queue / message broker.
 */
@Configuration
public class AsyncConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService agentExecutor(@Value("${agent.async.pool-size:4}") int poolSize) {
        return Executors.newFixedThreadPool(poolSize);
    }
}
