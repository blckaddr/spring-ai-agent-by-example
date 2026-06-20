package com.example.calculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Calculator MCP server. A second passive tool provider (see docs/adr/0003). The agent
 * aggregates its tools alongside the currency server's — orchestration is the model's job.
 */
@SpringBootApplication
public class CalculatorServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CalculatorServerApplication.class, args);
    }
}
