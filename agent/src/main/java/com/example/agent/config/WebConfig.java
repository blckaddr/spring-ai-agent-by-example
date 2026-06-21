package com.example.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Clean URLs for the UI surfaces. {@code /} is the hub (the welcome-page {@code static/index.html});
 * {@code /chat} is the single-agent chat (Phases 0–5) and {@code /plan} is the planning graph
 * (Phase 6) — each forwards to its static page.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/chat").setViewName("forward:/chat.html");
        registry.addViewController("/plan").setViewName("forward:/plan.html");
    }
}
