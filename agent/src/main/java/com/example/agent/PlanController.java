package com.example.agent;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 6 — "Watch it Plan". A single synchronous endpoint: {@code POST /agent/plan} takes a goal
 * and returns the {@link PlanGraph} the planner designed (plus the tool catalog and the planning
 * cost). No execution, no streaming — planning is one model call.
 *
 * <p>The single-agent endpoints from earlier phases are untouched; this is a separate surface, like
 * the chat at {@code /} and the graph page at {@code /plan}.
 */
@RestController
@RequestMapping("/agent")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/plan")
    public ResponseEntity<?> plan(@RequestBody AgentRequest request) {
        try {
            return ResponseEntity.ok(planService.plan(request.input()));
        } catch (PlanException e) {
            // 422: the model answered, but its plan was not valid — show what it produced.
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("error", e.getMessage(), "raw", e.getRaw()));
        }
    }
}
