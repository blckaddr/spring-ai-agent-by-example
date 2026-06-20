package com.example.currency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Currency MCP server. A passive tool provider — it exposes tools over MCP/streamable-HTTP
 * and never contains any agent loop (see docs/adr/0003).
 */
@SpringBootApplication
public class CurrencyServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CurrencyServerApplication.class, args);
    }
}
