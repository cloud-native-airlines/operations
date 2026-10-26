package com.cloudnativeairlines.operations.sim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * A snapshot of the Simulator's clock. Only the run id and simulated time matter
 * here; the rest of the snapshot (speed, tick, etc.) is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SimClock(String runId, Instant simulatedAt) {
}
