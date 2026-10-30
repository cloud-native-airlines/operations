package com.cloudnativeairlines.operations.api;

import com.cloudnativeairlines.operations.sim.SimClient;
import com.cloudnativeairlines.operations.sim.SimClock;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The resolved run context (run id + simulated time) from the Simulator clock,
 * so a client can scope its queries and classify flights relative to "now"
 * without reaching the Simulator itself.
 */
@RestController
@RequestMapping("/api/v1/context")
public class ContextController {

    private final SimClient sim;

    public ContextController(SimClient sim) {
        this.sim = sim;
    }

    public record ContextResponse(String runId, Instant simulatedAt) {
    }

    @GetMapping
    public ContextResponse get() {
        SimClock clock = sim.clock();
        return new ContextResponse(clock.runId(), clock.simulatedAt());
    }
}
