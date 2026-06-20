package com.example.calculator;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Arithmetic tools, kept GRANULAR on purpose: the agent must orchestrate convert (currency
 * server) + add (this server) itself. We deliberately do NOT offer a coarse "convertAndSum"
 * tool — the orchestration is the Phase 1 lesson.
 */
@Service
public class CalculatorService {

    @Tool(description = "Add a list of numbers together and return their sum.")
    public double add(
            @ToolParam(description = "The numbers to add together") double[] numbers) {
        if (numbers == null) {
            throw new IllegalArgumentException("numbers must not be null");
        }
        double sum = 0;
        for (double n : numbers) {
            sum += n;
        }
        return round(sum);
    }

    @Tool(description = "Subtract the second number from the first (a - b).")
    public double subtract(
            @ToolParam(description = "The number to subtract from") double a,
            @ToolParam(description = "The number to subtract") double b) {
        return round(a - b);
    }

    @Tool(description = "Multiply a list of numbers together and return their product.")
    public double multiply(
            @ToolParam(description = "The numbers to multiply together") double[] numbers) {
        if (numbers == null || numbers.length == 0) {
            throw new IllegalArgumentException("numbers must contain at least one value");
        }
        double product = 1;
        for (double n : numbers) {
            product *= n;
        }
        return round(product);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
